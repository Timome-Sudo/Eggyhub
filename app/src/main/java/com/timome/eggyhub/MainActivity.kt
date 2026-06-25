package com.timome.eggyhub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.timome.eggyhub.data.AuthManager
import com.timome.eggyhub.ui.component.DataCollectionConfig
import com.timome.eggyhub.ui.component.PermissionDialog
import com.timome.eggyhub.ui.screen.ForgotPasswordScreen
import com.timome.eggyhub.ui.screen.HomeScreen
import com.timome.eggyhub.ui.screen.LoginScreen
import com.timome.eggyhub.ui.screen.RegisterScreen
import com.timome.eggyhub.ui.theme.EggyhubTheme
import com.timome.eggyhub.util.LogcatExportUtil
import kotlinx.coroutines.launch

/**
 * 应用页面路由枚举
 */
enum class AppScreen {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD,
    HOME
}

class MainActivity : ComponentActivity() {
    private lateinit var authManager: AuthManager
    private lateinit var safLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    
    var pendingExportConfig: DataCollectionConfig? = null
    var onExportComplete: ((Boolean) -> Unit)? = null
    var showPermissionDialog by mutableStateOf(false)
    var missingPermissions by mutableStateOf(listOf<String>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)
        enableEdgeToEdge()
        
        safLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data?.data != null) {
                val uri = result.data?.data!!
                pendingExportConfig?.let { config ->
                    val success = LogcatExportUtil.exportLogToSafUri(this, uri, config)
                    onExportComplete?.invoke(success)
                }
            } else {
                onExportComplete?.invoke(false)
            }
            pendingExportConfig = null
            onExportComplete = null
        }
        
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                showPermissionDialog = false
                pendingExportConfig?.let { config ->
                    safLauncher.launch(LogcatExportUtil.createSafPickerIntent())
                }
            } else {
                checkPermissionsAndShowDialog()
            }
        }
        
        setContent {
            EggyhubTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EggyhubApp(
                        authManager = authManager,
                        onExportRequested = { config, onComplete ->
                            pendingExportConfig = config
                            onExportComplete = onComplete
                            
                            val missing = LogcatExportUtil.getMissingPermissions(this)
                            if (missing.isEmpty()) {
                                safLauncher.launch(LogcatExportUtil.createSafPickerIntent())
                            } else {
                                missingPermissions = missing
                                showPermissionDialog = true
                            }
                        },
                        showPermissionDialog = showPermissionDialog,
                        missingPermissions = missingPermissions,
                        onRequestPermission = {
                            val shouldShowRationale = LogcatExportUtil.shouldShowRequestPermissionRationale(
                                this,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                            
                            if (shouldShowRationale) {
                                permissionLauncher.launch(
                                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                )
                            } else {
                                goToAppSettings()
                                android.widget.Toast.makeText(
                                    this,
                                    "请授权存储权限",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onCheckPermission = {
                            checkPermissionsAndShowDialog()
                        },
                        onPermissionDialogDismiss = {
                            showPermissionDialog = false
                            onExportComplete?.invoke(false)
                            pendingExportConfig = null
                            onExportComplete = null
                        }
                    )
                }
            }
        }
    }
    
    private fun checkPermissionsAndShowDialog() {
        val missing = LogcatExportUtil.getMissingPermissions(this)
        if (missing.isEmpty()) {
            showPermissionDialog = false
            pendingExportConfig?.let { config ->
                safLauncher.launch(LogcatExportUtil.createSafPickerIntent())
            }
        } else {
            missingPermissions = missing
            showPermissionDialog = true
        }
    }
    
    private fun goToAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}

@Composable
fun EggyhubApp(
    authManager: AuthManager,
    onExportRequested: (DataCollectionConfig, (Boolean) -> Unit) -> Unit,
    showPermissionDialog: Boolean = false,
    missingPermissions: List<String> = emptyList(),
    onRequestPermission: () -> Unit = {},
    onCheckPermission: () -> Unit = {},
    onPermissionDialogDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLoggedIn by authManager.isLoggedIn.collectAsState(initial = false)
    val username by authManager.username.collectAsState(initial = "")
    val userId by authManager.userId.collectAsState(initial = "")
    val email by authManager.email.collectAsState(initial = "")
    val description by authManager.description.collectAsState(initial = "")
    val avatar by authManager.avatar.collectAsState(initial = "")
    val role by authManager.role.collectAsState(initial = "")
    val sponser by authManager.sponser.collectAsState(initial = "")
    val eggyid by authManager.eggyid.collectAsState(initial = "")
    val contact by authManager.contact.collectAsState(initial = "")
    val accessToken by authManager.accessToken.collectAsState(initial = "")
    val password by authManager.password.collectAsState(initial = "")
    val isGuestMode = accessToken.isBlank()

    var currentScreen by remember {
        mutableStateOf(if (isLoggedIn) AppScreen.HOME else AppScreen.LOGIN)
    }

    if (isLoggedIn && currentScreen != AppScreen.HOME) {
        currentScreen = AppScreen.HOME
    }

    val coroutineScope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current

    when (currentScreen) {
        AppScreen.LOGIN -> {
            LoginScreen(
                context = context,
                onLoginSuccess = { accessToken, uname, email, password, userId, role, sponser, avatar, contact, description, eggyid ->
                    authManager.loginWithUser(email, password, accessToken, uname, userId, role, sponser, avatar, contact, description, eggyid)
                },
                onRegisterClick = {
                    currentScreen = AppScreen.REGISTER
                },
                onForgotPasswordClick = {
                    currentScreen = AppScreen.FORGOT_PASSWORD
                },
                onExportRequested = onExportRequested
            )
        }

        AppScreen.REGISTER -> {
            RegisterScreen(
                onBack = {
                    currentScreen = AppScreen.LOGIN
                },
                onRegisterSuccess = {
                    currentScreen = AppScreen.LOGIN
                }
            )
        }

        AppScreen.FORGOT_PASSWORD -> {
            ForgotPasswordScreen(
                onBack = {
                    currentScreen = AppScreen.LOGIN
                },
                onGoToLogin = {
                    currentScreen = AppScreen.LOGIN
                }
            )
        }

        AppScreen.HOME -> {
            HomeScreen(
                username = username,
                userId = userId,
                email = email,
                description = description,
                avatarUrl = avatar,
                role = role,
                sponser = sponser,
                eggyid = eggyid,
                contact = contact,
                accessToken = accessToken,
                password = password,
                isGuestMode = isGuestMode,
                onLogout = {
                    coroutineScope.launch {
                        authManager.logout()
                        currentScreen = AppScreen.LOGIN
                    }
                },
                onExportRequested = onExportRequested
            )
        }
    }

    PermissionDialog(
        show = showPermissionDialog,
        missingPermissions = missingPermissions,
        onRequestPermission = onRequestPermission,
        onCheckPermission = onCheckPermission,
        onDismiss = onPermissionDialogDismiss
    )
}
