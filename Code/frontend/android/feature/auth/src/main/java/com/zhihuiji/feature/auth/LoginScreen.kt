package com.zhihuiji.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhihuiji.core.designsystem.BackgroundGradientEnd
import com.zhihuiji.core.designsystem.BackgroundGradientStart
import com.zhihuiji.core.designsystem.GlassScaffold
import com.zhihuiji.core.designsystem.GlassSurfaceHigh
import com.zhihuiji.core.designsystem.GlassSurfaceLow
import com.zhihuiji.core.designsystem.GlassTextField
import com.zhihuiji.core.designsystem.LiquidGlassCard
import com.zhihuiji.core.designsystem.PrimaryButton
import com.zhihuiji.core.designsystem.TextPrimary
import com.zhihuiji.core.designsystem.TextSecondary
import com.zhihuiji.core.designsystem.ZhihuijiPrimary

internal val AuthBackgroundBrush = Brush.verticalGradient(
    colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
)
internal val AuthCardShape = RoundedCornerShape(28.dp)
private val AuthFieldShape = RoundedCornerShape(14.dp)

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showServerEditor by remember { mutableStateOf(false) }
    var serverDraft by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LoginContent(
            isLoading = uiState.isLoading,
            canEditBaseUrl = uiState.canEditBaseUrl,
            onLogin = { phone, password -> viewModel.login(phone, password) },
            onOpenServerEditor = {
                serverDraft = uiState.serverUrl
                showServerEditor = true
            },
            onNavigateToRegister = onNavigateToRegister,
            modifier = Modifier.fillMaxSize()
        )
        if (showServerEditor && uiState.canEditBaseUrl) {
            ServerAddressDialog(
                value = serverDraft,
                onValueChange = { serverDraft = it },
                onDismiss = { showServerEditor = false },
                onConfirm = {
                    viewModel.saveBaseUrl(serverDraft)
                    showServerEditor = false
                },
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun LoginContent(
    isLoading: Boolean,
    canEditBaseUrl: Boolean,
    onLogin: (String, String) -> Unit,
    onOpenServerEditor: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    GlassScaffold(modifier = modifier.background(AuthBackgroundBrush)) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthHeroOverview(
                    canEditBaseUrl = canEditBaseUrl,
                    onOpenServerEditor = onOpenServerEditor,
                    title = "店掌柜",
                )

                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    blurRadius = 24.dp,
                    shape = AuthCardShape,
                    surfaceColor = GlassSurfaceHigh,
                    contentPadding = 24.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AuthOutlinedField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = "手机号",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        AuthOutlinedField(
                            value = password,
                            onValueChange = { password = it },
                            label = "密码",
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = ZhihuijiPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        PrimaryButton(
                            text = "登录并进入首页",
                            onClick = { onLogin(phone, password) },
                            enabled = phone.isNotBlank() && password.isNotBlank() && !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        TextButton(onClick = onNavigateToRegister) {
                            Text("还没有账号？去注册", color = ZhihuijiPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AuthOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    GlassTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        modifier = Modifier.fillMaxWidth(),
        shape = AuthFieldShape
    )
}

@Composable
internal fun AuthHeroOverview(
    canEditBaseUrl: Boolean,
    onOpenServerEditor: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(GlassSurfaceLow, CircleShape)
                .pointerInput(canEditBaseUrl) {
                    if (canEditBaseUrl) {
                        detectTapGestures(onLongPress = { onOpenServerEditor() })
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = "店掌柜图标",
                tint = ZhihuijiPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ServerAddressDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "服务器地址", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "仅用于 debug 构建切换 HTTPS 联调环境；HTTP 地址会被自动规范为 HTTPS。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                GlassTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = "例如 https://zhj-api.sxyq27.online/",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = AuthFieldShape,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = value.isNotBlank()) {
                Text("保存", color = ZhihuijiPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        },
    )
}
