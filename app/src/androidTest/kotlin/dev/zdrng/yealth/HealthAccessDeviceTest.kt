package dev.zdrng.yealth

import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import dev.zdrng.yealth.domain.model.*
import dev.zdrng.yealth.domain.repository.HealthRepository
import dev.zdrng.yealth.domain.service.HealthBrowserService
import dev.zdrng.yealth.ui.MainActivity
import dev.zdrng.yealth.ui.feature.*
import dev.zdrng.yealth.ui.navigation.YealthApp
import dev.zdrng.yealth.ui.theme.YealthTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

class HealthAccessDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun click(tag: String) = compose.onNodeWithTag(tag).performClick()
    private fun scroll(list: String, tag: String) {
        compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
        click(tag)
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(400)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("screencap -p")).use {
            File(instrumentation.targetContext.getExternalFilesDir(null), "m3-$name.png").outputStream().use { out -> it.copyTo(out) }
        }
    }
    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }

    private fun systemClick(text: String, optional: Boolean = false) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        var node: android.view.accessibility.AccessibilityNodeInfo? = null
        repeat(if (optional) 1 else 50) {
            if (node == null) {
                node = automation.rootInActiveWindow?.findAccessibilityNodeInfosByText(text)?.firstOrNull { it.text?.toString() == text }
                if (node == null) android.os.SystemClock.sleep(100)
            }
        }
        if (node == null) { check(optional) { "System action missing: $text in ${automation.rootInActiveWindow?.packageName}" }; return }
        var target: android.view.accessibility.AccessibilityNodeInfo = requireNotNull(node)
        while (!target.isClickable && target.parent != null) target = requireNotNull(target.parent)
        check(target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
        android.os.SystemClock.sleep(700)
    }

    // Isolated mutating test: invoke with -e grantFlow true on the project emulator.
    @Test fun systemPartialGrantDenialAndHistory() {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("grantFlow") == "true")
        scroll("screen-today", "summary-steps")
        compose.onNodeWithText("Review access").performScrollTo().performClick()
        click("permission-continue")
        android.os.SystemClock.sleep(1200)
        systemClick("Get started", optional = true)
        screenshot("before-grant")
        systemClick("Steps")
        systemClick("Allow")
        compose.waitUntil(10000) { compose.onAllNodesWithTag("screen-metric").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("No records for this range").performScrollTo().assertExists()
        click("nav-access")
        compose.onNodeWithTag("screen-access").performScrollToNode(hasTestTag("access-ACTIVITY"))
        compose.onNodeWithText("Partially granted").assertExists()
        screenshot("partial-grant")
        click("access-ACTIVITY")
        click("permission-continue")
        android.os.SystemClock.sleep(1000)
        systemClick("Don't allow")
        compose.waitUntil(10000) { compose.onAllNodesWithTag("screen-access").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Partially granted").assertExists()
        scroll("screen-access", "history-request")
        screenshot("history-rationale")
        click("permission-continue")
        android.os.SystemClock.sleep(1200)
        screenshot("history-system")
        systemClick("Don't allow")
        compose.waitUntil(10000) { compose.onAllNodesWithTag("screen-access").fetchSemanticsNodes().isNotEmpty() }
        click("nav-today")
        compose.onNodeWithText("No records for this range").performScrollTo().assertExists()
        compose.onNodeWithText("Read access is required", substring = true).assertDoesNotExist()
        screenshot("recent-after-history-denial")
        click("nav-access")
        scroll("screen-access", "history-request")
        click("permission-continue")
        android.os.SystemClock.sleep(1200)
        systemClick("Allow")
        compose.waitUntil(10000) { compose.onAllNodesWithTag("screen-access").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("screen-access").performScrollToNode(hasText("Older-data access is granted.", substring = true))
        screenshot("history-granted")
    }

    // Run on a clean emulator permission state. This enters the real platform permission UI.
    @Test fun systemRequestCancellationPreservesSelection() {
        scroll("screen-today", "summary-steps")
        click("period-WEEK")
        click("previous-date")
        val selectedDate = compose.onNodeWithTag("choose-date").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].single().text
        compose.onNodeWithText("Review access").performScrollTo().performClick()
        compose.onNodeWithText("Yealth requests read access only", substring = true).assertExists()
        compose.onNodeWithText("Steps cadence").assertExists() // M5 now implements this shared-permission reader.
        screenshot("rationale")
        click("permission-continue")
        android.os.SystemClock.sleep(1800)
        systemClick("Get started", optional = true)
        val root = InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow
        if (root?.packageName.toString() != "dev.zdrng.yealth") {
            screenshot("system-permissions")
            shell("input keyevent KEYCODE_BACK")
        } else {
            // Android can suppress repeat requests after explicit denial. The app must
            // retain the missing-access state and offer settings, never show an empty result.
            compose.onNodeWithTag("missing-access-settings").performScrollTo().assertExists()
            screenshot("repeat-denial-settings")
        }
        compose.waitUntil(10000) { compose.onAllNodesWithTag("screen-metric").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("screen-metric").performScrollToIndex(0)
        compose.onNodeWithTag("period-WEEK").assertIsSelected()
        compose.onNodeWithTag("choose-date").assertTextContains(selectedDate)
        compose.onNodeWithTag("screen-metric").performScrollToNode(hasText("Review access"))
        compose.onNodeWithText("Review access").assertExists()
        screenshot("cancelled")
    }

    @Test fun actualProviderAccessAndSettings() {
        click("nav-access")
        compose.waitUntil(10000) { compose.onAllNodesWithTag("access-ACTIVITY").fetchSemanticsNodes().isNotEmpty() }
        screenshot("access")
        scroll("screen-access", "health-settings")
        android.os.SystemClock.sleep(1400)
        screenshot("health-settings")
        val root = InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow
        check(root?.packageName.toString() != "dev.zdrng.yealth")
        shell("input keyevent KEYCODE_BACK")
        compose.waitUntil(10000) { compose.onAllNodesWithTag("screen-access").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("nav-access").assertIsSelected()
    }

    @Test fun providerAndHistoryRecoveryStates() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = (context.applicationContext as YealthApplication).container.browserContent.catalog
        var environment = HealthEnvironment(ProviderStatus.UPDATE_REQUIRED)
        val repo = object : HealthRepository {
            override val recordTypes = catalog
            override suspend fun stepsTotal(range: HealthTimeRange): StepsTotalResult = StepsTotalResult.Failed(ReadFailure.INVALID_REQUEST)
        override suspend fun environment() = EnvironmentResult.Ready(environment)
            override suspend fun read(request: RecordRequest): PageResult = error("Recovery screens must not read records")
        }
        val service = HealthBrowserService(repo)
        lateinit var access: AccessViewModel
        var recoveries = 0
        compose.activityRule.scenario.onActivity { activity ->
            access = AccessViewModel(service)
            activity.setContent { YealthTheme { YealthApp(BrowserContent(catalog), access, service, recover = { recoveries++ }) } }
            access.foreground()
        }
        click("nav-access")
        compose.onNodeWithText("Open system or provider settings").performScrollTo().performClick()
        compose.runOnIdle { check(recoveries == 1); environment = HealthEnvironment(ProviderStatus.UNAVAILABLE); access.refresh() }
        compose.onNodeWithText("Health Connect is unavailable", substring = true).assertExists()
        screenshot("unavailable")
        compose.runOnIdle {
            environment = HealthEnvironment(ProviderStatus.AVAILABLE, grantedPermissions = setOf(catalog.single { it.id == "steps" }.readPermission))
            access.refresh()
        }
        scroll("screen-access", "access-ACTIVITY")
        compose.onNodeWithTag("permission-continue").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("screen-access").performScrollToNode(hasText("Older-data access is not supported", substring = true))
        compose.onNodeWithTag("history-request").assertDoesNotExist()
        screenshot("history-unsupported")
        compose.runOnIdle { access.background() }
    }
}
