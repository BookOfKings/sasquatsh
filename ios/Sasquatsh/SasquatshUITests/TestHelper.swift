import Foundation

/// Uses a pre-created test account for all tests.
/// The account persists between test runs — tests only clean up data they create
/// (events, groups, etc.), never the account itself.
final class TestHelper {
    static let shared = TestHelper()

    private let firebaseAPIKey = "AIzaSyBpC-CQ5F6DEMIAiBlyzAoOvT0fu91fS6E"
    private let supabaseFunctionsURL = "https://yyfukoddeyiaxiufztdx.supabase.co/functions/v1"
    private let supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl5ZnVrb2RkZXlpYXhpdWZ6dGR4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzE5MTAxODIsImV4cCI6MjA4NzQ4NjE4Mn0.mC-Y3MHnXOShFr1_ax9vRfMgOG1rPGR72TiTMBSMYOk"

    // Pre-created test account — do NOT delete this account
    let email = "haggis77_7@hotmail.com"
    let password = "password1"

    private(set) var idToken: String = ""
    private(set) var isSetUp = false

    private init() {}

    // MARK: - Login (call ONCE per test run)

    func loginTestAccount() async throws {
        let loginURL = URL(string: "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=\(firebaseAPIKey)")!
        var request = URLRequest(url: loginURL)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: [
            "email": email,
            "password": password,
            "returnSecureToken": true
        ])

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            let body = String(data: data, encoding: .utf8) ?? "unknown"
            throw TestError.loginFailed("Firebase login failed: \(body)")
        }

        let json = try JSONSerialization.jsonObject(with: data) as! [String: Any]
        idToken = json["idToken"] as! String

        isSetUp = true
        print("✅ Test account logged in: \(email)")
    }

    // MARK: - API Helper

    func makeAPICall(
        endpoint: String,
        method: String = "GET",
        body: [String: Any]? = nil,
        queryParams: [String: String]? = nil
    ) async throws -> (Data, Int) {
        var urlString = "\(supabaseFunctionsURL)/\(endpoint)"
        if let params = queryParams {
            let query = params.map { "\($0.key)=\($0.value)" }.joined(separator: "&")
            urlString += "?\(query)"
        }

        var request = URLRequest(url: URL(string: urlString)!)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(supabaseAnonKey)", forHTTPHeaderField: "Authorization")
        request.setValue(idToken, forHTTPHeaderField: "X-Firebase-Token")

        if let body {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
        }

        let (data, response) = try await URLSession.shared.data(for: request)
        let statusCode = (response as? HTTPURLResponse)?.statusCode ?? 0
        return (data, statusCode)
    }
}

enum TestError: Error, LocalizedError {
    case loginFailed(String)
    case apiFailed(String)

    var errorDescription: String? {
        switch self {
        case .loginFailed(let msg): return msg
        case .apiFailed(let msg): return msg
        }
    }
}
