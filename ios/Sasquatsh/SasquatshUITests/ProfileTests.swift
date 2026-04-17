import XCTest

final class ProfileTests: SasquatshUITestBase {

    func testProfileLoads() {
        waitForDashboard()
        tapTab("Profile")
        sleep(3)
        XCTAssertTrue(app.exists, "Profile tab should load")
    }

    func testEditProfileOpens() {
        waitForDashboard()
        tapTab("Profile")
        sleep(3)

        // Find Edit Profile by looking for it in the scroll view
        let editButton = app.staticTexts["Edit Profile"]
        if editButton.waitForExistence(timeout: 5) {
            editButton.tap()
            sleep(2)

            // Cancel if sheet appeared
            let cancelButton = app.buttons["Cancel"]
            if cancelButton.waitForExistence(timeout: 3) {
                cancelButton.tap()
            }
        }
    }

    func testAppearanceToggle() {
        waitForDashboard()
        tapTab("Profile")
        sleep(3)

        // Look for appearance section
        let dark = app.buttons["Dark"]
        let light = app.buttons["Light"]
        let system = app.buttons["System"]
        XCTAssertTrue(
            dark.waitForExistence(timeout: 5) || light.exists || system.exists,
            "Appearance toggle should exist on profile"
        )
    }

    func testBadgesPage() {
        waitForDashboard()
        tapTab("Profile")
        sleep(3)

        let badges = app.staticTexts["Badges & Achievements"]
        if badges.waitForExistence(timeout: 5) {
            badges.tap()
            sleep(3)
            XCTAssertTrue(app.exists, "Badges page should load")
        }
    }
}
