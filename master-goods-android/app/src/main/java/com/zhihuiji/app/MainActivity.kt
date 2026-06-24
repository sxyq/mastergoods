package com.zhihuiji.app

import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zhihuiji.app.navigation.AppNavGraph
import com.zhihuiji.app.navigation.AgentLaunchRequest
import com.zhihuiji.app.security.RuntimeSecurityGuard
import com.zhihuiji.app.security.SignatureIntegrityChecker
import com.zhihuiji.core.designsystem.ZhihuijiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startupAgentLaunch = parseStartupAgentLaunch(intent?.extras)
        if (shouldEnforceProductionRuntimeGuards(BuildConfig.BUILD_TYPE)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
            if (!SignatureIntegrityChecker.isSignatureTrusted(this, BuildConfig.APP_SIGNING_SHA256)) {
                finishAffinity()
                return
            }
            if (RuntimeSecurityGuard.isHighRiskRuntime()) {
                finishAffinity()
                return
            }
        }
        preferHighRefreshRateDisplayMode()
        enableEdgeToEdge()
        setContent {
            ZhihuijiTheme {
                AppNavGraph(startupAgentLaunch = startupAgentLaunch)
            }
        }
    }

    private fun preferHighRefreshRateDisplayMode() {
        @Suppress("DEPRECATION")
        val currentDisplay = windowManager.defaultDisplay
        var preferredMode: Display.Mode? = null
        for (mode in currentDisplay.supportedModes) {
            if (mode.refreshRate < 90f) continue
            val currentPreferred = preferredMode
            if (
                currentPreferred == null ||
                mode.refreshRate > currentPreferred.refreshRate ||
                (
                    mode.refreshRate == currentPreferred.refreshRate &&
                        mode.physicalWidth * mode.physicalHeight > currentPreferred.physicalWidth * currentPreferred.physicalHeight
                    )
            ) {
                preferredMode = mode
            }
        }
        val resolvedPreferredMode = preferredMode ?: return

        window.attributes = window.attributes.apply {
            preferredDisplayModeId = resolvedPreferredMode.modeId
            preferredRefreshRate = resolvedPreferredMode.refreshRate
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                setFrameRateBoostOnTouchEnabled(true)
                setFrameRatePowerSavingsBalanced(false)
            }
        }
    }

    companion object {
        const val EXTRA_AGENT_OPEN_CHAT = "com.zhihuiji.app.extra.AGENT_OPEN_CHAT"
        const val EXTRA_AGENT_INITIAL_QUESTION = "com.zhihuiji.app.extra.AGENT_INITIAL_QUESTION"
        const val EXTRA_AGENT_CONVERSATION_ID = "com.zhihuiji.app.extra.AGENT_CONVERSATION_ID"

        internal fun parseStartupAgentLaunch(extras: Bundle?): AgentLaunchRequest? {
            extras ?: return null
            return parseStartupAgentLaunchValues(
                openChat = extras.getBoolean(EXTRA_AGENT_OPEN_CHAT, false),
                initialQuestion = extras.getString(EXTRA_AGENT_INITIAL_QUESTION),
                conversationId = extras.getLong(EXTRA_AGENT_CONVERSATION_ID, -1L),
            )
        }

        internal fun parseStartupAgentLaunchValues(
            openChat: Boolean,
            initialQuestion: String?,
            conversationId: Long?,
        ): AgentLaunchRequest? {
            val normalizedQuestion = initialQuestion?.trim().orEmpty()
            val normalizedConversationId = conversationId?.takeIf { it > 0L }
            if (!openChat && normalizedQuestion.isBlank() && normalizedConversationId == null) return null
            return AgentLaunchRequest(
                openChat = openChat || normalizedQuestion.isNotBlank() || normalizedConversationId != null,
                initialQuestion = normalizedQuestion.ifBlank { null },
                conversationId = normalizedConversationId,
            )
        }

        internal fun shouldEnforceProductionRuntimeGuards(buildType: String): Boolean =
            buildType == "release"
    }
}
