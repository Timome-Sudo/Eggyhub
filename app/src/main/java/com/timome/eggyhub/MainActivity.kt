package com.timome.eggyhub

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.timome.eggyhub.ui.screen.ForgotPasswordScreen
import com.timome.eggyhub.ui.screen.HomeScreen
import com.timome.eggyhub.ui.screen.LoginScreen
import com.timome.eggyhub.ui.screen.RegisterScreen
import com.timome.eggyhub.ui.theme.EggyhubTheme
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)
        enableEdgeToEdge()
        setContent {
            EggyhubTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EggyhubApp(
                        authManager = authManager
                    )
                }
            }
        }
    }
}

@Composable
fun EggyhubApp(
    authManager: AuthManager,
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
                }
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
                onLogout = {
                    coroutineScope.launch {
                        authManager.logout()
                        currentScreen = AppScreen.LOGIN
                    }
                }
            )
        }
    }
}
