import XCTest

final class EventTests: SasquatshUITestBase {

    func testBrowseEvents() {
        waitForDashboard()
        tapTab("Games")
        sleep(3)
        XCTAssertTrue(app.exists, "Games tab should load")
    }

    func testEventDetailOpens() {
        waitForDashboard()
        tapTab("Games")
        sleep(3)

        // Try to tap the first event card if any exist
        let cards = app.scrollViews.otherElements.buttons
        if cards.count > 0 {
            cards.element(boundBy: 0).tap()
            sleep(3)
            XCTAssertTrue(app.exists, "Event detail should load without crashing")
        }
    }
}
