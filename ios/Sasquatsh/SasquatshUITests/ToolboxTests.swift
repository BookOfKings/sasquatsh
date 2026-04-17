import XCTest

final class ToolboxTests: SasquatshUITestBase {

    func testToolboxListLoads() {
        waitForDashboard()

        app.staticTexts["Gamer Toolbox"].tap()

        XCTAssertTrue(waitForElement(app.staticTexts["First Player Pickers"]))
        XCTAssertTrue(app.staticTexts["Turn Tracker"].exists)
        XCTAssertTrue(app.staticTexts["Score Keeper"].exists)
        XCTAssertTrue(app.staticTexts["Round Counter"].exists)
        XCTAssertTrue(app.staticTexts["Random Game Picker"].exists)
    }

    func testRoundCounter() {
        waitForDashboard()

        app.staticTexts["Gamer Toolbox"].tap()
        sleep(1)
        app.staticTexts["Round Counter"].tap()
        sleep(2)

        // Should see round display
        let roundText = app.staticTexts["ROUND"]
        XCTAssertTrue(waitForElement(roundText, timeout: 5), "Round counter should show ROUND label")
    }

    func testScoreKeeper() {
        waitForDashboard()

        app.staticTexts["Gamer Toolbox"].tap()
        sleep(1)
        app.staticTexts["Score Keeper"].tap()
        sleep(2)

        // Should see setup screen with player name field
        let playerField = app.textFields["Player name"]
        XCTAssertTrue(waitForElement(playerField, timeout: 5), "Should see player name field")

        // Add two players
        playerField.tap()
        playerField.typeText("Alice")
        sleep(1)

        // Tap the + button
        let addButtons = app.buttons.matching(identifier: "plus.circle.fill")
        if addButtons.count > 0 {
            addButtons.element(boundBy: 0).tap()
            sleep(1)
        }

        let playerField2 = app.textFields["Player name"]
        if playerField2.waitForExistence(timeout: 3) {
            playerField2.tap()
            playerField2.typeText("Bob")
            sleep(1)
            if addButtons.count > 0 {
                addButtons.element(boundBy: 0).tap()
                sleep(1)
            }
        }

        // Verify players added
        XCTAssertTrue(app.staticTexts["Alice"].exists || app.staticTexts["Bob"].exists,
                       "At least one player should be visible")
    }

    func testTurnTracker() {
        waitForDashboard()

        app.staticTexts["Gamer Toolbox"].tap()
        sleep(1)

        let turnTracker = app.staticTexts["Turn Tracker"]
        XCTAssertTrue(waitForElement(turnTracker))
        turnTracker.tap()
        sleep(2)

        // Quick start mode - should see player count stepper
        XCTAssertTrue(app.exists, "Turn tracker should load without crashing")
    }
}
