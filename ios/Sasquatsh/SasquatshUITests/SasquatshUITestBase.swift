import XCTest

/// Base class for all UI tests. Creates ONE test account for the entire suite,
/// shares it across all tests, and deletes it when the suite finishes.
class SasquatshUITestBase: XCTestCase {

    // Shared across ALL test instances in this run
    private static var accountReady = false
    private static var setupError: Error?

    var app: XCUIApplication!

    // MARK: - Suite Lifecycle (ONCE per run)

    override class func setUp() {
        super.setUp()
        guard !accountReady else { return }

        let expectation = XCTestExpectation(description: "Login test account")
        Task {
            do {
                try await TestHelper.shared.loginTestAccount()
                accountReady = true
            } catch {
                setupError = error
                print("❌ Failed to login test account: \(error)")
            }
            expectation.fulfill()
        }
        _ = XCTWaiter.wait(for: [expectation], timeout: 30)
    }

    override class func tearDown() {
        // Account persists between runs — no cleanup needed
        super.tearDown()
    }

    // MARK: - Per-Test Lifecycle

    override func setUp() {
        super.setUp()
        continueAfterFailure = false

        if let error = Self.setupError {
            XCTFail("Test account setup failed: \(error)")
            return
        }

        app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launchEnvironment = [
            "TEST_EMAIL": TestHelper.shared.email,
            "TEST_PASSWORD": TestHelper.shared.password
        ]
        app.launch()
    }

    override func tearDown() {
        app?.terminate()
        super.tearDown()
    }

    // MARK: - Helpers

    /// Wait for an element to appear
    func waitForElement(_ element: XCUIElement, timeout: TimeInterval = 10) -> Bool {
        element.waitForExistence(timeout: timeout)
    }

    /// Wait for dashboard to load after login
    func waitForDashboard() {
        // The app auto-logs in via --uitesting flag
        // Wait for the Gamer Toolbox header or dashboard content
        let toolbox = app.staticTexts["Gamer Toolbox"]
        XCTAssertTrue(waitForElement(toolbox, timeout: 15), "Dashboard should load after auto-login")
    }

    /// Tap a tab bar item
    func tapTab(_ label: String) {
        let tab = app.staticTexts[label]
        if tab.waitForExistence(timeout: 5) {
            tab.tap()
        }
    }

    /// Dismiss keyboard if visible
    func dismissKeyboard() {
        if app.keyboards.element(boundBy: 0).exists {
            app.tap() // tap outside
        }
    }
}
