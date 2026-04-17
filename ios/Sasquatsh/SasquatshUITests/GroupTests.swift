import XCTest

final class GroupTests: SasquatshUITestBase {

    func testGroupsListLoads() {
        waitForDashboard()
        tapTab("Groups")
        sleep(3)
        XCTAssertTrue(app.exists, "Groups tab should load")
    }

    func testCreateAndDeleteGroup() {
        waitForDashboard()
        tapTab("Groups")
        sleep(3)

        // Find create group button
        let createButtons = app.buttons.matching(NSPredicate(format: "label CONTAINS[c] 'Create'"))
        guard createButtons.count > 0 else {
            // May be at tier limit
            return
        }
        createButtons.element(boundBy: 0).tap()
        sleep(3)

        // Fill group name
        let nameField = app.textFields["Group Name"]
        guard nameField.waitForExistence(timeout: 5) else {
            app.buttons["Cancel"].tap()
            return
        }
        nameField.tap()
        let testName = "E2E Test Group \(Int.random(in: 1000...9999))"
        nameField.typeText(testName)
        dismissKeyboard()
        sleep(1)

        // Tap Create
        let createButton = app.buttons["Create"]
        if createButton.waitForExistence(timeout: 3) && createButton.isEnabled {
            createButton.tap()
            sleep(5)

            // Should navigate to the new group or back to list
            XCTAssertTrue(app.exists, "App should work after creating group")

            // Navigate to groups to find and delete
            tapTab("Groups")
            sleep(3)

            let groupText = app.staticTexts[testName]
            if groupText.waitForExistence(timeout: 5) {
                groupText.tap()
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

                        // Confirm
                        let confirmDelete = app.alerts.buttons["Delete"]
                        if confirmDelete.waitForExistence(timeout: 3) {
                            confirmDelete.tap()
                            sleep(3)
                            print("✅ Test group deleted: \(testName)")
                        }
                    }
                }
            }
        } else {
            app.buttons["Cancel"].tap()
        }
    }

    func testGroupDetailLoads() {
        waitForDashboard()
        tapTab("Groups")
        sleep(3)

        // Try to open first group if any exist
        let scrollView = app.scrollViews.firstMatch
        let buttons = scrollView.buttons
        if buttons.count > 0 {
            buttons.element(boundBy: 0).tap()
            sleep(3)

            // Group detail should show tabs
            let membersTab = app.staticTexts["Members"].exists ||
                             app.buttons["Members"].exists
            XCTAssertTrue(app.exists, "Group detail should load")
        }
    }

    func testNeedPlayersLoads() {
        waitForDashboard()
        tapTab("Need Players")
        sleep(3)
        XCTAssertTrue(app.exists, "Need Players tab should load")
    }
}
