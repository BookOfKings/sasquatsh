import Foundation

enum APIError: LocalizedError {
    case invalidURL
    case noData
    case decodingError(Error)
    case serverError(String)
    case unauthorized
    case networkError(Error)
    case unknown

    var errorDescription: String? {
        switch self {
        case .invalidURL: return "Invalid URL"
        case .noData: return "No data received"
        case .decodingError(let error): return "Data error: \(error.localizedDescription)"
        case .serverError(let message): return message
        case .unauthorized: return "Please sign in to continue"
        case .networkError(let error): return "Network error: \(error.localizedDescription)"
        case .unknown: return "An unknown error occurred"
        }
    }
}

struct APIErrorResponse: Codable {
    let error: String
}

actor APIClient {
    private let session: URLSession
    private let baseURL: String
    private let anonKey: String
    private var getFirebaseToken: (() async -> String?)?

    init(
        baseURL: String = AppConfig.supabaseFunctionsURL,
        anonKey: String = AppConfig.supabaseAnonKey
    ) {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        self.session = URLSession(configuration: config)
        self.baseURL = baseURL
        self.anonKey = anonKey
    }

    func setTokenProvider(_ provider: @escaping () async -> String?) {
        self.getFirebaseToken = provider
    }

    // MARK: - Public API

    func get<T: Decodable>(_ endpoint: String, queryItems: [URLQueryItem] = [], authenticated: Bool = true) async throws -> T {
        let request = try await buildRequest(endpoint: endpoint, method: "GET", queryItems: queryItems, authenticated: authenticated)
        return try await execute(request)
    }

    func post<T: Decodable>(_ endpoint: String, body: (any Encodable)? = nil, queryItems: [URLQueryItem] = [], authenticated: Bool = true) async throws -> T {
        var request = try await buildRequest(endpoint: endpoint, method: "POST", queryItems: queryItems, authenticated: authenticated)
        if let body {
            request.httpBody = try JSONEncoder().encode(body)
        }
        return try await execute(request)
    }

    func put<T: Decodable>(_ endpoint: String, body: (any Encodable)? = nil, queryItems: [URLQueryItem] = [], authenticated: Bool = true) async throws -> T {
        var request = try await buildRequest(endpoint: endpoint, method: "PUT", queryItems: queryItems, authenticated: authenticated)
        if let body {
            request.httpBody = try JSONEncoder().encode(body)
        }
        return try await execute(request)
    }

    func delete<T: Decodable>(_ endpoint: String, queryItems: [URLQueryItem] = [], authenticated: Bool = true) async throws -> T {
        let request = try await buildRequest(endpoint: endpoint, method: "DELETE", queryItems: queryItems, authenticated: authenticated)
        return try await execute(request)
    }

    func postVoid(_ endpoint: String, body: (any Encodable)? = nil, queryItems: [URLQueryItem] = [], authenticated: Bool = true) async throws {
        var request = try await buildRequest(endpoint: endpoint, method: "POST", queryItems: queryItems, authenticated: authenticated)
        if let body {
            request.httpBody = try JSONEncoder().encode(body)
        }
        try await executeVoid(request)
    }

    func deleteVoid(_ endpoint: String, queryItems: [URLQueryItem] = [], authenticated: Bool = true) async throws {
        let request = try await buildRequest(endpoint: endpoint, method: "DELETE", queryItems: queryItems, authenticated: authenticated)
        try await executeVoid(request)
    }

    func putVoid(_ endpoint: String, body: (any Encodable)? = nil, queryItems: [URLQueryItem] = [], authenticated: Bool = true) async throws {
        var request = try await buildRequest(endpoint: endpoint, method: "PUT", queryItems: queryItems, authenticated: authenticated)
        if let body {
            request.httpBody = try JSONEncoder().encode(body)
        }
        try await executeVoid(request)
    }

    func postMultipart<T: Decodable>(
        _ endpoint: String,
        fileData: Data,
        fileName: String,
        mimeType: String,
        fieldName: String,
        queryItems: [URLQueryItem] = [],
        authenticated: Bool = true
    ) async throws -> T {
        var request = try await buildRequest(endpoint: endpoint, method: "POST", queryItems: queryItems, authenticated: authenticated)

        let boundary = UUID().uuidString
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        var body = Data()
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"\(fieldName)\"; filename=\"\(fileName)\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: \(mimeType)\r\n\r\n".data(using: .utf8)!)
        body.append(fileData)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)
        request.httpBody = body

        return try await execute(request)
    }

    // MARK: - Private

    private func buildRequest(endpoint: String, method: String, queryItems: [URLQueryItem], authenticated: Bool) async throws -> URLRequest {
        guard var components = URLComponents(string: "\(baseURL)/\(endpoint)") else {
            throw APIError.invalidURL
        }

        if !queryItems.isEmpty {
            components.queryItems = (components.queryItems ?? []) + queryItems
        }

        guard let url = components.url else {
            throw APIError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(anonKey)", forHTTPHeaderField: "Authorization")

        if authenticated {
            if let token = await getFirebaseToken?() {
                request.setValue(token, forHTTPHeaderField: "X-Firebase-Token")
            } else {
                throw APIError.unauthorized
            }
        }

        return request
    }

    private func execute<T: Decodable>(_ request: URLRequest) async throws -> T {
        let (data, response) = try await performRequest(request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.unknown
        }

        if httpResponse.statusCode == 401 {
            throw APIError.unauthorized
        }

        if httpResponse.statusCode >= 400 {
            if let errorResponse = try? JSONDecoder().decode(APIErrorResponse.self, from: data) {
                throw APIError.serverError(errorResponse.error)
            }
            throw APIError.serverError("Request failed with status \(httpResponse.statusCode)")
        }

        do {
            return try JSONDecoder().decode(T.self, from: data)
        } catch {
            throw APIError.decodingError(error)
        }
    }

    private func executeVoid(_ request: URLRequest) async throws {
        let (data, response) = try await performRequest(request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.unknown
        }

        if httpResponse.statusCode == 401 {
            throw APIError.unauthorized
        }

        if httpResponse.statusCode >= 400 {
            if let errorResponse = try? JSONDecoder().decode(APIErrorResponse.self, from: data) {
                throw APIError.serverError(errorResponse.error)
            }
            throw APIError.serverError("Request failed with status \(httpResponse.statusCode)")
        }
    }

    private func performRequest(_ request: URLRequest) async throws -> (Data, URLResponse) {
        do {
            return try await session.data(for: request)
        } catch {
            throw APIError.networkError(error)
        }
    }
}
