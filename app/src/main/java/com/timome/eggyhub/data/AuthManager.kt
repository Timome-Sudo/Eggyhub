package com.timome.eggyhub.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

/**
 * 认证状态管理器
 *
 * 负责保存和读取登录用户的各种状态数据
 * 参考 temp/LoginActivity.java 的 SharedPreferences 字段：
 * - email, password, access_token, username, id, role, sponser, avatar, contact, description, eggyid
 */
class AuthManager(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val EMAIL = stringPreferencesKey("email")
        private val PASSWORD = stringPreferencesKey("password")
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val USERNAME = stringPreferencesKey("username")
        private val USER_ID = stringPreferencesKey("user_id")
        private val ROLE = stringPreferencesKey("role")
        private val SPONSER = stringPreferencesKey("sponser")
        private val AVATAR = stringPreferencesKey("avatar")
        private val CONTACT = stringPreferencesKey("contact")
        private val DESCRIPTION = stringPreferencesKey("description")
        private val EGGYID = stringPreferencesKey("eggyid")
    }

    /** 是否登录 */
    val isLoggedIn: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[IS_LOGGED_IN] ?: false }

    /** 用户名 */
    val username: Flow<String> = dataStore.data
        .map { preferences -> preferences[USERNAME] ?: "" }

    /** 邮箱 */
    val email: Flow<String> = dataStore.data
        .map { preferences -> preferences[EMAIL] ?: "" }

    /** access_token */
    val accessToken: Flow<String> = dataStore.data
        .map { preferences -> preferences[ACCESS_TOKEN] ?: "" }

    /** 用户角色 */
    val role: Flow<String> = dataStore.data
        .map { preferences -> preferences[ROLE] ?: "" }

    /** 赞助状态 */
    val sponser: Flow<String> = dataStore.data
        .map { preferences -> preferences[SPONSER] ?: "" }

    /** 用户 ID */
    val userId: Flow<String> = dataStore.data
        .map { preferences -> preferences[USER_ID] ?: "" }

    /** 头像 URL */
    val avatar: Flow<String> = dataStore.data
        .map { preferences -> preferences[AVATAR] ?: "" }

    /** 自我介绍 */
    val description: Flow<String> = dataStore.data
        .map { preferences -> preferences[DESCRIPTION] ?: "" }

    /** 联系方式 */
    val contact: Flow<String> = dataStore.data
        .map { preferences -> preferences[CONTACT] ?: "" }

    /** 蛋仔昵称 */
    val eggyid: Flow<String> = dataStore.data
        .map { preferences -> preferences[EGGYID] ?: "" }

    /** 登录成功时保存所有相关信息 */
    suspend fun loginWithUser(
        email: String,
        password: String,
        accessToken: String,
        username: String,
        userId: Int,
        role: String,
        sponser: String,
        avatar: String = "",
        contact: String = "",
        description: String = "",
        eggyid: String = ""
    ) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[EMAIL] = email
            preferences[PASSWORD] = password
            preferences[ACCESS_TOKEN] = accessToken
            preferences[USERNAME] = username
            preferences[USER_ID] = userId.toString()
            preferences[ROLE] = role
            preferences[SPONSER] = sponser
            preferences[AVATAR] = avatar
            preferences[CONTACT] = contact
            preferences[DESCRIPTION] = description
            preferences[EGGYID] = eggyid
        }
    }

    /** 仅保存用户名/简单登录（用于向后兼容，不建议再使用） */
    suspend fun login(username: String = "") {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            if (username.isNotBlank()) {
                preferences[USERNAME] = username
            }
        }
    }

    /** 保存用户资料（fetchUserProfile 后的结果） */
    suspend fun saveUserProfile(
        avatar: String,
        contact: String,
        description: String,
        eggyid: String
    ) {
        dataStore.edit { preferences ->
            preferences[AVATAR] = avatar
            preferences[CONTACT] = contact
            preferences[DESCRIPTION] = description
            preferences[EGGYID] = eggyid
        }
    }

    /** 登出，清除所有数据 */
    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
            preferences[USERNAME] = ""
            preferences[EMAIL] = ""
            preferences[PASSWORD] = ""
            preferences[ACCESS_TOKEN] = ""
            preferences[USER_ID] = ""
            preferences[ROLE] = ""
            preferences[SPONSER] = ""
            preferences[AVATAR] = ""
            preferences[CONTACT] = ""
            preferences[DESCRIPTION] = ""
            preferences[EGGYID] = ""
        }
    }
}
