package com.zhihuiji.uireference

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zhihuiji.core.designsystem.ZhihuijiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Force the light scheme: the historical glass visual language targets light backgrounds.
            ZhihuijiTheme(darkTheme = false) {
                UIReferenceScreen()
            }
        }
    }
}
