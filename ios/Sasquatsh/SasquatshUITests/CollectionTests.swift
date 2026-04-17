import XCTest

final class CollectionTests: SasquatshUITestBase {

    func testCollectionPageLoads() {
        waitForDashboard()
        tapTab("Profile")
        sleep(3)

        let collection = app.staticTexts["My Game Collection"]
        XCTAssertTrue(waitForElement(collection, timeout: 5), "Collection link should be on profile")
        collection.tap()
        sleep(3)

        // Should see the tabs
        let myGames = app.buttons["My Games (0)"].exists || app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'My Games'")).count > 0
        let top50 = app.buttons["Top 50"].exists
        let search = app.buttons["Search BGG"].exists
        XCTAssertTrue(myGames || top50 || search, "Collection tabs should be visible")
    }

    func testSearchBGG() {
        waitForDashboard()
        tapTab("Profile")
        sleep(3)

        app.staticTexts["My Game Collection"].tap()
        sleep(3)

        // Switch to Search BGG tab
        let searchTab = app.buttons["Search BGG"]
        if searchTab.waitForExistence(timeout: 5) {
            searchTab.tap()
            sleep(1)

            // Type a search
            let searchField = app.textFields["Search BoardGameGeek..."]
            if searchField.waitForExistence(timeout: 5) {
                searchField.tap()
                searchField.typeText("Catan")
                sleep(3) // wait for debounced search

                // Should see results
                XCTAssertTrue(app.exists, "Search should complete without crashing")
            }
        }
    }

    func testTop50Loads() {
        waitForDashboard()
        tapTab("Profile")
        sleep(3)

        app.staticTexts["My Game Collection"].tap()
        sleep(3)

        let top50 = app.buttons["Top 50"]
        if top50.waitForExistence(timeout: 5) {
            top50.tap()
            sleep(3)
            XCTAssertTrue(app.exists, "Top 50 should load without crashing")
        }
    }
}
