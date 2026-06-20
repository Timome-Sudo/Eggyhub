package com.timome.eggyhub.data

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * 将 \uXXXX 格式的 Unicode 字符串解码为中文
 * 参考 LoginActivity.java 的 decodeUnicode() 方法
 */
fun decodeUnicode(unicodeStr: String?): String {
    if (unicodeStr == null) return ""
    val sb = StringBuilder()
    var i = 0
    while (i < unicodeStr.length) {
        if (unicodeStr[i] == '\\') {
            if (i + 1 < unicodeStr.length && unicodeStr[i + 1] == 'u') {
                if (i + 5 < unicodeStr.length) {
                    val hex = unicodeStr.substring(i + 2, i + 6)
                    try {
                        val codePoint = hex.toInt(16)
                        sb.append(Character.toChars(codePoint))
                        i += 6
                        continue
                    } catch (e: NumberFormatException) {
                        // 解析失败，按原样处理
                    }
                }
            }
        }
        sb.append(unicodeStr[i])
        i++
    }
    return sb.toString()
}

/**
 * 解码服务器返回的 message
 * 先 URLDecoder.decode(msg, "UTF-8")，再 decodeUnicode
 */
fun decodeServerMessage(encodedMessage: String): String {
    val urlDecoded = try {
        URLDecoder.decode(encodedMessage, StandardCharsets.UTF_8.name())
    } catch (e: Exception) {
        encodedMessage
    }
    return decodeUnicode(urlDecoded)
}

/**
 * 网络请求服务层
 *
 * 参考 temp 目录下的 Java Activity 实现方式
 *
 * - 登录: POST /api/auth/login   body: { "auth": hexString }
 * - 注册: POST /api/auth/register  body: { username, email, password, invite }
 * - 找回密码: POST /api/password/forgot body: { email }
 */
object ApiService {

    private const val BASE_URL = "https://eggyhub.top/api"
    private const val LOGIN_URL = "$BASE_URL/auth/login"
    private const val REGISTER_URL = "$BASE_URL/auth/register"
    private const val FORGOT_PASSWORD_URL = "$BASE_URL/password/forgot"
    private const val PROFILE_URL = "$BASE_URL/users/profile"
    private const val TIMEOUT_SECONDS = 30

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * 请求结果回调接口
     *
     * @param T 返回的数据类型
     */
    sealed class ApiResult<out T> {
        data class Success<T>(val data: T) : ApiResult<T>()
        data class Error(val message: String) : ApiResult<Nothing>()
        data class NetworkError(val message: String) : ApiResult<Nothing>()
    }

    /**
     * 登录成功返回的用户数据
     */
    data class LoginUser(
        val id: Int,
        val username: String,
        val email: String,
        val role: String,
        val sponser: String
    )

    /**
     * 登录结果
     */
    data class LoginResponse(
        val accessToken: String,
        val user: LoginUser
    )

    /**
     * 用户资料（从 /api/users/profile 获取或从登录响应中解析）
     */
    data class UserProfile(
        val id: Int,
        val username: String,
        val email: String,
        val role: String,
        val sponser: String,
        val avatar: String,
        val contact: String,
        val description: String,
        val eggyid: String
    )

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            .build()
    }

    /**
     * 登录接口（RSA加密版本）
     *
     * 流程：
     * 1. 将 email, password, timestamp 组成 JSON
     * 2. RSA加密后转为十六进制字符串
     * 3. 通过 { "auth": hexString } 发送给服务器
     *
     * 参考 LoginActivity.java 的完整错误处理逻辑
     */
    fun login(
        email: String,
        password: String,
        onSuccess: (LoginResponse) -> Unit,
        onFailure: (String) -> Unit
    ): Call? {
        val encrypted = RsaEncryptUtil.encryptLoginData(email, password)
        if (encrypted == null) {
            onFailure("登录数据处理失败")
            return null
        }

        val jsonBody = JSONObject()
        jsonBody.put("auth", encrypted)

        val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(LOGIN_URL)
            .post(body)
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                try {
                    if (response.isSuccessful) {
                        // 响应体为空
                        if (responseBody.isEmpty()) {
                            onFailure("服务器返回空响应")
                            return
                        }
                        val json = JSONObject(responseBody)
                        val status = json.optString("status", "")
                        if (status == "success") {
                            // 登录成功
                            val accessToken = json.optString("access_token", "")
                            val userObj = json.optJSONObject("user")
                            val user = LoginUser(
                                id = userObj?.optInt("id", -1) ?: -1,
                                username = userObj?.optString("username", "") ?: "",
                                email = userObj?.optString("email", "") ?: "",
                                role = userObj?.optString("role", "") ?: "",
                                sponser = userObj?.optString("sponser", "0") ?: "0"
                            )
                            onSuccess(LoginResponse(accessToken, user))
                        } else {
                            // 登录失败，但响应有 message
                            val message = json.optString("message", "")
                            if (message.isNotEmpty()) {
                                val decoded = decodeServerMessage(message)
                                onFailure(decoded)
                            } else {
                                onFailure("登录失败: $responseBody")
                            }
                        }
                    } else {
                        // 非 2xx 响应
                        val errorBody = if (responseBody.isNotEmpty()) {
                            // 尝试从 error body 中提取 message
                            try {
                                val json = JSONObject(responseBody)
                                val message = json.optString("message", "")
                                if (message.isNotEmpty()) {
                                    decodeServerMessage(message)
                                } else {
                                    responseBody
                                }
                            } catch (e: Exception) {
                                responseBody
                            }
                        } else {
                            response.message
                        }
                        onFailure("登录失败: ${response.code} - $errorBody")
                    }
                } catch (e: Exception) {
                    // JSON 解析异常
                    onFailure("响应解析失败: $responseBody")
                }
            }
        })

        return call
    }

    /**
     * 注册接口
     */
    fun register(
        username: String,
        email: String,
        password: String,
        invite: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val jsonBody = JSONObject()
        jsonBody.put("username", username)
        jsonBody.put("email", email)
        jsonBody.put("password", password)
        jsonBody.put("invite", invite)

        val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(REGISTER_URL)
            .addHeader("Accept", "application/json, text/plain, */*")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    try {
                        val json = JSONObject(responseBody)
                        val message = json.optString("message", "请前往邮箱验证")
                        onSuccess(message)
                    } catch (e: Exception) {
                        onFailure("请前往邮箱验证")
                    }
                } else {
                    try {
                        val json = JSONObject(responseBody)
                        val message = json.optString("message", "未知错误")
                        onFailure("注册失败: $message")
                    } catch (e: Exception) {
                        onFailure("注册失败")
                    }
                }
            }
        })

        return call
    }

    /**
     * 找回密码接口（发送重置密码邮件）
     */
    fun forgotPassword(
        email: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val jsonBody = JSONObject()
        jsonBody.put("email", email)

        val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(FORGOT_PASSWORD_URL)
            .post(body)
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    onSuccess("重置密码链接已发送至邮箱")
                } else {
                    try {
                        val json = JSONObject(responseBody)
                        val message = json.optString("message", "未知错误")
                        onFailure("重置密码失败: $message")
                    } catch (e: Exception) {
                        onFailure("重置密码失败")
                    }
                }
            }
        })

        return call
    }

    /**
     * 获取用户资料接口（GET /api/users/profile）
     *
     * 参考 PersonalHomePageActivity.java 中从 SharedPreferences 读取的字段：
     * - id, username, email, role, sponser, avatar, contact, description, eggyid
     *
     * @param accessToken 登录令牌（Bearer Token）
     */
    fun fetchUserProfile(
        accessToken: String,
        onSuccess: (UserProfile) -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val request = Request.Builder()
            .url(PROFILE_URL)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                try {
                    if (response.isSuccessful) {
                        if (responseBody.isEmpty()) {
                            onFailure("服务器返回空响应")
                            return
                        }
                        val json = JSONObject(responseBody)
                        val status = json.optString("status", "")

                        val userObj = if (json.has("user")) {
                            json.optJSONObject("user")
                        } else {
                            json
                        }

                        val profile = UserProfile(
                            id = userObj?.optInt("id", -1) ?: -1,
                            username = userObj?.optString("username", "") ?: "",
                            email = userObj?.optString("email", "") ?: "",
                            role = userObj?.optString("role", "") ?: "",
                            sponser = userObj?.optString("sponser", "0") ?: "0",
                            avatar = userObj?.optString("avatar", "") ?: userObj?.optString("cover", "") ?: "",
                            contact = userObj?.optString("contact", "") ?: "",
                            description = userObj?.optString("description", "") ?: "",
                            eggyid = userObj?.optString("eggyid", "") ?: ""
                        )
                        onSuccess(profile)
                    } else {
                        onFailure("获取用户资料失败: ${response.code}")
                    }
                } catch (e: Exception) {
                    onFailure("响应解析失败: $responseBody")
                }
            }
        })

        return call
    }
}
