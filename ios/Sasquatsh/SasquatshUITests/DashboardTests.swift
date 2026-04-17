import XCTest

final class DashboardTests: SasquatshUITestBase {

    func testDashboardLoads() {
        waitForDashboard()
    }

    func testToolboxOpens() {
        waitForDashboard()

        let toolbox = app.staticTexts["Gamer Toolbox"]
        XCTAssertTrue(toolbox.exists, "Gamer Toolbox should be in header")
        toolbox.tap()

        let firstPlayer = app.staticTexts["First Player Pickers"]
        XCTAssertTrue(waitForElement(firstPlayer), "First Player Pickers should be visible")
    }

    func testTabNavigation() {
        waitForDashboard()

        // Navigate to each tab
        tapTab("Games")
        sleep(3)
        XCTAssertTrue(app.exists, "Games tab should load without crashing")

        tapTab("Groups")
        sleep(3)
        XCTAssertTrue(app.exists, "Groups tab should load without crashing")

        tapTab("Profile")
        sleep(3)
        XCTAssertTrue(app.exists, "Profile tab should load without crashing")

        tapTab("Dashboard")
        waitForDashboard()
    }
}
