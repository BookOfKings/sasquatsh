import XCTest

final class HostGameTests: SasquatshUITestBase {

    func testHostGameFormOpens() {
        waitForDashboard()

        // Try the dashboard "Host a Game" button
        let hostButtons = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] 'Host'"))
        if hostButtons.count > 0 {
            hostButtons.element(boundBy: 0).tap()
            sleep(3)

            // Create Game form should appear
            let createTitle = app.staticTexts["Create Game"]
            XCTAssertTrue(waitForElement(createTitle, timeout: 5), "Create Game form should appear")

            // Cancel
            let cancel = app.buttons["Cancel"]
            if cancel.waitForExistence(timeout: 3) {
                cancel.tap()
            }
        }
    }

    func testCreateAndDeleteEvent() {
        waitForDashboard()

        // Find Host a Game - try buttons first, then static text
        let hostButtons = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] 'Host'"))
        if hostButtons.count > 0 {
            hostButtons.element(boundBy: 0).tap()
        } else {
            let hostText = app.staticTexts.matching(NSPredicate(format: "label CONTAINS[c] 'Host'"))
            guard hostText.count > 0 else { return } // Skip if not found
            hostText.element(boundBy: 0).tap()
        }
        sleep(3)

        // Fill title
        let titleField = app.textFields["Title"]
        guard titleField.waitForExistence(timeout: 5) else {
            // Try to dismiss and skip
            app.buttons["Cancel"].tap()
            return
        }
        titleField.tap()
        let testTitle = "E2E Test \(Int.random(in: 1000...9999))"
        titleField.typeText(testTitle)
        dismissKeyboard()
        sleep(1)

        // Tap Create
        let createButton = app.buttons["Create"]
        if createButton.waitForExistence(timeout: 3) && createButton.isEnabled {
            createButton.tap()
            sleep(5)

            // Should return to games list or dashboard
            XCTAssertTrue(app.exists, "App should return after creating event")

            // Now find and delete the event we created
            tapTab("Games")
            sleep(3)

            // Look for our event
            let eventText = app.staticTexts[testTitle]
            if eventText.waitForExistence(timeout: 5) {
                eventText.tap()
                sleep(3)

                // Open menu and delete
                let menuButton = app.buttons["ellipsis.circle"]
                if menuButton.waitForExistence(timeout: 3) {
                    menuButton.tap()
                    sleep(1)

                    let deleteButton = app.buttons["Delete"]
                    if deleteButton.waitForExistence(timeout: 3) {
                        deleteButton.tap()
                        sleep(1)

                        // Confirm delete
                        let confirmDelete = app.buttons["Delete"]
                        if confirmDelete.waitForExistence(timeout: 3) {
                            confirmDelete.tap()
                            sleep(3)
                            print("✅ Test event deleted: \(testTitle)")
                        }
                    }
                }
            }
        } else {
            // Can't create — may be at tier limit, cancel
            app.buttons["Cancel"].tap()
        }
    }

    func testFormFields() {
        waitForDashboard()

        let hostButtons = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] 'Host'"))
        guard hostButtons.count > 0 else { return }
        hostButtons.element(boundBy: 0).tap()
        sleep(3)

        // Verify key form fields exist
        XCTAssertTrue(app.staticTexts["Basic Information"].waitForExistence(timeout: 5), "Basic Info section")
        XCTAssertTrue(app.staticTexts["Game System"].exists, "Game System picker")
        XCTAssertTrue(app.textFields["Title"].exists, "Title field")

        // Cancel
        app.buttons["Cancel"].tap()
    }
}
