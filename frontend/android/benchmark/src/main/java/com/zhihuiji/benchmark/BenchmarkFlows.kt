package com.zhihuiji.benchmark

import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

private const val AppForegroundTimeoutMs = 15_000L
private const val ScreenReadyTimeoutMs = 12_000L
private const val ChatCompletionTimeoutMs = 30_000L
private const val ChatQuestion = "customer receivable"

internal object BenchmarkFlows {
    const val PackageName = "com.zhihuiji.app"
    private const val MainActivity = "com.zhihuiji.app.MainActivity"
    private const val ExtraAgentOpenChat = "com.zhihuiji.app.extra.AGENT_OPEN_CHAT"
    private const val ExtraAgentInitialQuestion = "com.zhihuiji.app.extra.AGENT_INITIAL_QUESTION"

    fun MacrobenchmarkScope.startHome() {
        pressHome()
        startActivityAndWait(mainIntent())
        waitForLoggedInHome()
        device.waitForIdle()
    }

    fun MacrobenchmarkScope.openAssistantTab() {
        waitForLoggedInHome()
        val assistantTab = requireAppObject(
            primarySelector = appDescSelector("助手"),
            fallbackSelector = appTextSelector("助手"),
            timeoutMs = ScreenReadyTimeoutMs,
            description = "assistant tab",
        )
        assistantTab.click()
        waitForAppForeground("assistant workbench")
        requireAppObject(
            primarySelector = appTextSelector("AI 助手"),
            timeoutMs = ScreenReadyTimeoutMs,
            description = "assistant workbench title",
        )
        device.waitForIdle()
    }

    fun MacrobenchmarkScope.launchAgentChatWithQuestion(question: String = ChatQuestion) {
        val intent = mainIntent().apply {
            putExtra(ExtraAgentOpenChat, true)
            putExtra(ExtraAgentInitialQuestion, question)
        }
        startActivityAndWait(intent)
        waitForAppForeground("agent chat")
        waitForChatCompletion()
    }

    private fun mainIntent(): Intent =
        Intent().apply {
            action = Intent.ACTION_MAIN
            setClassName(PackageName, MainActivity)
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun MacrobenchmarkScope.waitForLoggedInHome() {
        waitForAppForeground("logged-in home")
        if (device.wait(Until.hasObject(appTextSelector("登录")), 750L)) {
            error("Benchmark requires an already logged-in session on device.")
        }
        requireAppObject(
            primarySelector = appDescSelector("首页"),
            fallbackSelector = appTextSelector("首页"),
            timeoutMs = ScreenReadyTimeoutMs,
            description = "home tab",
        )
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.waitForChatCompletion() {
        requireAppObject(
            primarySelector = appTextSelector("AI 对话"),
            timeoutMs = ScreenReadyTimeoutMs,
            description = "chat title",
        )
        check(
            device.wait(Until.hasObject(appTextContainsSelector("查询结果")), ChatCompletionTimeoutMs)
        ) {
            "Timed out waiting for rule-summary result to render. Current package=${device.currentPackageName}"
        }
        if (
            !device.hasObject(appTextContainsSelector("customer_receivable_lookup")) &&
            !device.hasObject(appTextContainsSelector("客户应收查询"))
        ) {
            device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 1.0f)
            device.wait(Until.hasObject(appTextContainsSelector("customer_receivable_lookup")), ScreenReadyTimeoutMs)
            device.wait(Until.hasObject(appTextContainsSelector("客户应收查询")), ScreenReadyTimeoutMs)
        }
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.waitForAppForeground(description: String) {
        check(device.wait(Until.hasObject(By.pkg(PackageName)), AppForegroundTimeoutMs)) {
            "Timed out waiting for $PackageName foreground for $description. Current package=${device.currentPackageName}"
        }
    }

    private fun MacrobenchmarkScope.requireAppObject(
        primarySelector: androidx.test.uiautomator.BySelector,
        fallbackSelector: androidx.test.uiautomator.BySelector? = null,
        timeoutMs: Long,
        description: String,
    ): UiObject2 {
        waitForAppForeground(description)
        return device.wait(Until.findObject(primarySelector), timeoutMs)
            ?: fallbackSelector?.let { device.wait(Until.findObject(it), timeoutMs) }
            ?: error(
                "Timed out waiting for $description in $PackageName. Current package=${device.currentPackageName}"
            )
    }

    private fun appTextSelector(text: String) = By.pkg(PackageName).text(text)

    private fun appTextContainsSelector(text: String) = By.pkg(PackageName).textContains(text)

    private fun appDescSelector(description: String) = By.pkg(PackageName).desc(description)
}
