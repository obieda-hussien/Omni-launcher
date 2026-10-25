package app.lawnchair.baseline

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

/** The launcher must be the device's default Home when collecting this journey. */
internal fun MacrobenchmarkScope.exerciseDrawerAndSearch() {
    device.wait(Until.hasObject(By.res(Constants.PACKAGE_NAME, "workspace")), 5_000)
    val width = device.displayWidth
    val height = device.displayHeight
    device.swipe(width / 2, height * 4 / 5, width / 2, height / 4, 16)
    device.waitForIdle()
    device.swipe(width / 2, height * 3 / 4, width / 2, height / 3, 12)
    device.waitForIdle()
    val input = device.findObject(By.res(Constants.PACKAGE_NAME, "input"))
    input?.text = "omni"
    device.waitForIdle()
    input?.text = "أومني"
    device.waitForIdle()
    device.pressBack()
    pressHome()
}

/**
 * Launch Settings from the app drawer, then return Home. This exercises the launcher-owned
 * icon-to-window morph and the closing-window-to-icon spring without changing their durations.
 * The benchmark image is expected to include the Android Settings app.
 */
internal fun MacrobenchmarkScope.exerciseAppLaunchAndReturnHome() {
    check(device.wait(Until.hasObject(By.res(Constants.PACKAGE_NAME, "workspace")), 5_000)) {
        "Launcher workspace was not visible before the app-launch journey"
    }

    val width = device.displayWidth
    val height = device.displayHeight
    device.swipe(width / 2, height * 4 / 5, width / 2, height / 4, 16)
    check(device.wait(Until.hasObject(By.res(Constants.PACKAGE_NAME, "input")), 5_000)) {
        "App drawer search field was not visible"
    }

    val search = device.findObject(By.res(Constants.PACKAGE_NAME, "input"))
    check(search != null) { "Could not find the app drawer search field" }
    search.text = "Settings"
    device.waitForIdle()

    // Exclude the search EditText itself, which also contains the word "Settings".
    val settingsSelector = By.pkg(Constants.PACKAGE_NAME)
        .clazz("com.android.launcher3.BubbleTextView")
        .text("Settings")
    check(device.wait(Until.hasObject(settingsSelector), 5_000)) {
        "Settings app icon was not found in the launcher search results"
    }
    val settings = device.findObject(settingsSelector)
    check(settings != null) { "Settings app icon disappeared before it could be clicked" }
    settings.click()
    check(device.wait(Until.hasObject(By.pkg("com.android.settings")), 10_000)) {
        "Settings did not open from the launcher"
    }
    device.waitForIdle()

    pressHome()
    check(device.wait(Until.hasObject(By.res(Constants.PACKAGE_NAME, "workspace")), 5_000)) {
        "Launcher workspace was not restored after Settings"
    }
    device.waitForIdle()
}
