import SwiftUI

@Observable
final class ServiceContainer {
    let api: APIClient
    let auth: AuthServiceProtocol
    let events: EventsServiceProtocol
    let groups: GroupsServiceProtocol
    let planning: PlanningServiceProtocol
    let profile: ProfileServiceProtocol
    let social: SocialServiceProtocol
    let bgg: BGGServiceProtocol
    let billing: BillingServiceProtocol

    init(
        api: APIClient = APIClient(),
        auth: AuthServiceProtocol = AuthService.shared
    ) {
        self.api = api
        self.auth = auth
        self.events = EventsService(api: api)
        self.groups = GroupsService(api: api)
        self.planning = PlanningService(api: api)
        self.profile = ProfileService(api: api)
        self.social = SocialService(api: api)
        self.bgg = BGGService(api: api)
        self.billing = BillingService(api: api)
    }
}

private struct ServiceContainerKey: EnvironmentKey {
    static let defaultValue = ServiceContainer()
}

extension EnvironmentValues {
    var services: ServiceContainer {
        get { self[ServiceContainerKey.self] }
        set { self[ServiceContainerKey.self] = newValue }
    }
}
