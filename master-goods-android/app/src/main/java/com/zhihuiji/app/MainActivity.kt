package com.zhihuiji.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zhihuiji.app.navigation.AppNavGraph
import com.zhihuiji.app.security.RuntimeSecurityGuard
import com.zhihuiji.app.security.SignatureIntegrityChecker
import com.zhihuiji.core.designsystem.ZhihuijiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
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
        enableEdgeToEdge()
        setContent {
            ZhihuijiTheme {
                AppNavGraph()
            }
        }
    }
}
