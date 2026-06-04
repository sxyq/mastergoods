package com.zhihuiji.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zhihuiji.core.designsystem.*

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val phoneValid = phone.matches(Regex("^1\\d{10}$"))

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onRegisterSuccess()
    }

    GlassScaffold(
        selectedDestination = "",
        destinations = emptyList(),
        onNavigate = {},
        showBottomBar = false,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            GlassTopBar(
                title = "创建账号",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("注册后即可使用智慧记全部功能", style = ZhihuijiTypography.bodyMedium, color = ZhihuijiColors.TextSecondary)
            Spacer(modifier = Modifier.height(18.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text("手机号") }, leadingIcon = { Icon(Icons.Default.Phone, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                        isError = phone.isNotBlank() && !phoneValid,
                        supportingText = if (phone.isNotBlank() && !phoneValid) {{ Text("请输入11位手机号") }} else null,
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("密码") }, leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                    )
                    OutlinedTextField(
                        value = inviteCode, onValueChange = { inviteCode = it },
                        label = { Text("邀请码") }, leadingIcon = { Icon(Icons.Default.VpnKey, null) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                    )
                    PrimaryButton(
                        text = "注册", onClick = { viewModel.register(phone, password, inviteCode) },
                        enabled = phoneValid && password.isNotBlank() && inviteCode.isNotBlank() && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.error!!.text, style = ZhihuijiTypography.bodySmall, color = ZhihuijiColors.Danger)
                    }
                }
            }
        }
    }
}
