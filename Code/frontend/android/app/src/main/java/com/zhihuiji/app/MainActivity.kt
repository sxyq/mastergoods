package com.zhihuiji.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.zhihuiji.app.navigation.AppNavGraph
import com.zhihuiji.app.navigation.AgentLaunchRequest
import com.zhihuiji.app.security.RuntimeSecurityGuard
import com.zhihuiji.app.security.SignatureIntegrityChecker
import com.zhihuiji.core.designsystem.ZhihuijiTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startupAgentLaunch = parseStartupAgentLaunch(intent?.extras)
        val enforceGuards = shouldEnforceProductionRuntimeGuards(BuildConfig.BUILD_TYPE)
        if (enforceGuards) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
        // 刷新率交给系统与窗口策略；应用不再主动偏好 60Hz 或强制 120Hz。
        enableEdgeToEdge()
        if (!enforceGuards) {
            showAppContent(startupAgentLaunch)
            return
        }
        // 安全校验逻辑与顺序保持：先签名，后运行时风险；失败则结束且不进入业务 UI。
        lifecycleScope.launch {
            val signatureTrusted = withContext(Dispatchers.IO) {
                SignatureIntegrityChecker.isSignatureTrusted(this@MainActivity, BuildConfig.APP_SIGNING_SHA256)
            }
            if (!signatureTrusted) {
                finishAffinity()
                return@launch
            }
            val highRiskRuntime = withContext(Dispatchers.IO) {
                RuntimeSecurityGuard.isHighRiskRuntime()
            }
            if (highRiskRuntime) {
                finishAffinity()
                return@launch
            }
            if (isFinishing || isDestroyed) return@launch
            showAppContent(startupAgentLaunch)
        }
    }

    private fun showAppContent(startupAgentLaunch: AgentLaunchRequest?) {
        setContent {
            ZhihuijiTheme {
                AppNavGraph(startupAgentLaunch = startupAgentLaunch)
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
