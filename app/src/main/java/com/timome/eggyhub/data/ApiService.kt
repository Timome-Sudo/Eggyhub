package com.timome.eggyhub.data

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import okio.Buffer
import okio.BufferedSink

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
class CountingRequestBody(
    private val delegate: RequestBody,
    private val totalBytes: Long,
    private val onProgress: (Long, Long, Double) -> Unit
) : RequestBody() {

    private var uploadedBytes: Long = 0
    private var startTime: Long = 0

    override fun contentType() = delegate.contentType()

    override fun contentLength() = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        startTime = System.currentTimeMillis()
        uploadedBytes = 0

        val buffer = Buffer()
        delegate.writeTo(buffer)

        val bufferSize = 8192L
        var bytesRead: Long

        while (buffer.size > 0) {
            bytesRead = minOf(buffer.size, bufferSize)
            sink.write(buffer, bytesRead)
            uploadedBytes += bytesRead

            val elapsedTime = (System.currentTimeMillis() - startTime).toDouble() / 1000.0
            val speed = if (elapsedTime > 0) uploadedBytes / elapsedTime else 0.0

            onProgress(uploadedBytes, totalBytes, speed)
        }

        sink.flush()
    }
}

object ApiService {

    private const val BASE_URL = "https://eggyhub.top/api"
    private const val LOGIN_URL = "$BASE_URL/auth/login"
    private const val REGISTER_URL = "$BASE_URL/auth/register"
    private const val FORGOT_PASSWORD_URL = "$BASE_URL/password/forgot"
    private const val PROFILE_URL = "$BASE_URL/users/profile"
    private const val CHANGE_PASSWORD_URL = "$BASE_URL/reset_pswd"
    private const val UPDATE_PROFILE_URL = "$BASE_URL/users/update_profile"
    private const val CHANGE_USERNAME_URL = "$BASE_URL/reset_name"
    private const val DELETE_ACCOUNT_URL = "$BASE_URL/account/delete"
    private const val PUBLISH_ARTICLE_URL = "$BASE_URL/publish"
    private const val PUBLISH_VIDEO_URL = "$BASE_URL/videos/sub"
    private const val PUBLISH_SHARE_CODE_URL = "$BASE_URL/gifts/sub"
    private const val PUBLISH_FILE_URL = "$BASE_URL/repos/upload"
    private const val USER_INFO_URL = "$BASE_URL/user/info"
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
        val sponser: String,
        val isActive: Int = 1
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
                            try {
                                val user = LoginUser(
                                    id = userObj?.optInt("id", -1) ?: -1,
                                    username = userObj?.optString("username", "") ?: "",
                                    email = userObj?.optString("email", "") ?: "",
                                    role = userObj?.optString("role", "") ?: "",
                                    sponser = userObj?.optString("sponser", "0") ?: "0",
                                    isActive = userObj?.optInt("is_active", 1) ?: 1
                                )
                                onSuccess(LoginResponse(accessToken, user))
                            } catch (e: Exception) {
                                onFailure("用户数据解析失败: ${e.message}")
                            }
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
     * 获取用户资料接口（GET /api/users/profile?id={userId}）
     *
     * 参考 LoginActivity.java 的 fetchUserProfile 方法：
     * - 请求方式：GET，参数 id
     * - 返回结构：{ success: true, data: { avatar, contact, description, eggyid } }
     *
     * @param userId 用户 ID
     */
    fun fetchUserProfile(
        userId: Int,
        onSuccess: (UserProfile) -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val request = Request.Builder()
            .url("$PROFILE_URL?id=$userId")
            .get()
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
                        val success = json.optBoolean("success", false)

                        if (success) {
                            val dataObj = json.optJSONObject("data")
                            val profile = UserProfile(
                                id = userId,
                                username = "",
                                email = "",
                                role = "",
                                sponser = "0",
                                avatar = dataObj?.optString("avatar", "") ?: "",
                                contact = dataObj?.optString("contact", "") ?: "",
                                description = dataObj?.optString("description", "") ?: "",
                                eggyid = dataObj?.optString("eggyid", "") ?: ""
                            )
                            onSuccess(profile)
                        } else {
                            onFailure("获取用户资料失败: ${json.optString("message", "未知错误")}")
                        }
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

    /**
     * 获取当前登录用户的完整信息（需要 access_token 鉴权）
     *
     * 返回包含 email, username, role, sponser, avatar, contact, description, eggyid 等完整信息
     *
     * @param accessToken 访问令牌
     */
    fun fetchCurrentUserInfo(
        accessToken: String,
        onSuccess: (UserProfile) -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val request = Request.Builder()
            .url(USER_INFO_URL)
            .get()
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
                        val success = json.optBoolean("success", false)

                        if (success) {
                            val dataObj = json.optJSONObject("data")
                            val profile = UserProfile(
                                id = dataObj?.optInt("id", 0) ?: 0,
                                username = dataObj?.optString("username", "") ?: "",
                                email = dataObj?.optString("email", "") ?: "",
                                role = dataObj?.optString("role", "") ?: "",
                                sponser = dataObj?.optString("sponser", "0") ?: "0",
                                avatar = dataObj?.optString("avatar", "") ?: "",
                                contact = dataObj?.optString("contact", "") ?: "",
                                description = dataObj?.optString("description", "") ?: "",
                                eggyid = dataObj?.optString("eggyid", "") ?: ""
                            )
                            onSuccess(profile)
                        } else {
                            onFailure("获取用户信息失败: ${json.optString("message", "未知错误")}")
                        }
                    } else {
                        onFailure("获取用户信息失败: ${response.code}")
                    }
                } catch (e: Exception) {
                    onFailure("响应解析失败: $responseBody")
                }
            }
        })

        return call
    }

    /**
     * 修改密码接口
     */
    fun changePassword(
        accessToken: String,
        oldPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val jsonBody = JSONObject()
        jsonBody.put("old_password", oldPassword)
        jsonBody.put("new_password", newPassword)

        val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(CHANGE_PASSWORD_URL)
            .addHeader("Authorization", "Bearer $accessToken")
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
                        val message = json.optString("message", "")
                        if ("Password updated successfully" == message) {
                            onSuccess()
                        } else {
                            onFailure(message.ifEmpty { "修改密码失败" })
                        }
                    } catch (e: Exception) {
                        onSuccess()
                    }
                } else {
                    onFailure("密码错误或请求失败")
                }
            }
        })

        return call
    }

    /**
     * 更新用户资料接口（POST）
     */
    fun updateUserProfile(
        accessToken: String,
        eggyid: String?,
        description: String?,
        contact: String?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val jsonBody = JSONObject()
        eggyid?.let { jsonBody.put("eggyid", it) }
        description?.let { jsonBody.put("description", it) }
        contact?.let { jsonBody.put("contact", it) }

        val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(UPDATE_PROFILE_URL)
            .addHeader("Authorization", "Bearer $accessToken")
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
                    onSuccess()
                } else {
                    onFailure("请求失败: ${response.code}")
                }
            }
        })

        return call
    }

    /**
     * 修改用户名接口
     */
    fun changeUsername(
        accessToken: String,
        newUsername: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val jsonBody = JSONObject()
        jsonBody.put("new_name", newUsername)

        val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(CHANGE_USERNAME_URL)
            .addHeader("Authorization", "Bearer $accessToken")
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
                        val updatedName = json.optString("new_username", "")
                        if (updatedName.isNotEmpty() && newUsername == updatedName) {
                            onSuccess()
                        } else {
                            onFailure("修改失败")
                        }
                    } catch (e: Exception) {
                        onSuccess()
                    }
                } else {
                    onFailure("请求失败: ${response.code}")
                }
            }
        })

        return call
    }

    /**
     * 删除账户接口
     */
    fun deleteAccount(
        accessToken: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val jsonBody = JSONObject()
        jsonBody.put("password", password)

        val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(DELETE_ACCOUNT_URL)
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure("密码错误或请求失败")
                }
            }
        })

        return call
    }

    fun publishArticle(
        accessToken: String,
        title: String,
        content: String,
        group: Int,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val jsonBody = JSONObject()
        jsonBody.put("id", "unstaged1")
        jsonBody.put("title", title)
        jsonBody.put("author", 1)
        jsonBody.put("content", content)
        jsonBody.put("group", group)

        val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(PUBLISH_ARTICLE_URL)
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure("上传失败，请检查网络连接")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    try {
                        val json = JSONObject(responseBody)
                        val message = json.optString("message", "发布成功")
                        onSuccess(message)
                    } catch (e: Exception) {
                        onSuccess("发布成功")
                    }
                } else if (response.code == 500) {
                    onFailure("上传失败，请检查文章名是否重复")
                } else {
                    onFailure("上传失败，错误码: ${response.code}")
                }
            }
        })

        return call
    }

    fun publishVideo(
        accessToken: String,
        name: String,
        bvId: String,
        description: String,
        imageUri: android.net.Uri,
        fileName: String,
        context: android.content.Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("name", name)
            jsonObject.put("cover", "")
            jsonObject.put("description", description)
            jsonObject.put("stock", 1)
            jsonObject.put("link", bvId)

            val inputStream = context.contentResolver.openInputStream(imageUri)
            val imageBytes = getBytes(inputStream)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", fileName,
                    RequestBody.create("image/jpeg".toMediaType(), imageBytes))
                .addFormDataPart(
                    "info",
                    null,
                    RequestBody.create(
                        "application/json".toMediaType(),
                        jsonObject.toString()
                    )
                )
                .build()

            val request = Request.Builder()
                .url(PUBLISH_VIDEO_URL)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            val call = httpClient.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onFailure("上传失败")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val jsonString = response.body?.string() ?: ""
                        try {
                            val jsonObject = JSONObject(jsonString)
                            val cover = jsonObject.optString("cover", "")
                            if (cover.isNotEmpty()) {
                                onSuccess()
                            } else {
                                onFailure("上传失败")
                            }
                        } catch (e: Exception) {
                            onFailure("解析异常")
                        }
                    } else {
                        onFailure("上传失败")
                    }
                }
            })

            return call
        } catch (e: Exception) {
            onFailure("上传异常")
            throw RuntimeException("上传异常", e)
        }
    }

    fun publishShareCode(
        accessToken: String,
        name: String,
        firstCode: String,
        description: String,
        eggCodeQuantity: Int,
        imageUri: android.net.Uri,
        fileName: String,
        context: android.content.Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("name", name)
            jsonObject.put("cover", "")
            jsonObject.put("description", description)
            jsonObject.put("stock", 1)
            jsonObject.put("first", firstCode)
            jsonObject.put("val", eggCodeQuantity.toString())

            val inputStream = context.contentResolver.openInputStream(imageUri)
            val imageBytes = getBytes(inputStream)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", fileName,
                    RequestBody.create("image/jpeg".toMediaType(), imageBytes))
                .addFormDataPart(
                    "info",
                    null,
                    RequestBody.create(
                        "application/json".toMediaType(),
                        jsonObject.toString()
                    )
                )
                .build()

            val request = Request.Builder()
                .url(PUBLISH_SHARE_CODE_URL)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            val call = httpClient.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onFailure("上传失败")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val jsonString = response.body?.string() ?: ""
                        try {
                            val jsonObject = JSONObject(jsonString)
                            val cover = jsonObject.optString("cover", "")
                            if (cover.isNotEmpty()) {
                                onSuccess()
                            } else {
                                onFailure("上传失败")
                            }
                        } catch (e: Exception) {
                            onFailure("解析异常")
                        }
                    } else {
                        onFailure("上传失败")
                    }
                }
            })

            return call
        } catch (e: Exception) {
            onFailure("上传异常")
            throw RuntimeException("上传异常", e)
        }
    }

    fun publishFile(
        accessToken: String,
        fileUri: android.net.Uri,
        fileName: String,
        context: android.content.Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
        onProgress: (Long, Long, Double) -> Unit = { _, _, _ -> }
    ): Call {
        try {
            val inputStream = context.contentResolver.openInputStream(fileUri)
            val fileBytes = getBytes(inputStream)
            val totalBytes = fileBytes.size.toLong()

            val fileRequestBody = RequestBody.create("application/octet-stream".toMediaType(), fileBytes)
            val countingRequestBody = CountingRequestBody(fileRequestBody, totalBytes, onProgress)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, countingRequestBody)
                .build()

            val request = Request.Builder()
                .url(PUBLISH_FILE_URL)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            val call = httpClient.newCall(request)
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onFailure("上传失败，请检查网络连接")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        onFailure("文件类型不支持或其他错误")
                    }
                }
            })

            return call
        } catch (e: Exception) {
            onFailure("上传异常")
            throw RuntimeException("上传异常", e)
        }
    }

    private fun getBytes(inputStream: InputStream?): ByteArray {
        if (inputStream == null) {
            return ByteArray(0)
        }
        return inputStream.use { stream ->
            val byteBuffer = ByteArrayOutputStream()
            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)

            var len: Int
            while (stream.read(buffer).also { len = it } != -1) {
                byteBuffer.write(buffer, 0, len)
            }
            byteBuffer.toByteArray()
        }
    }

    fun downloadFile(
        accessToken: String,
        url: String,
        destFile: java.io.File,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
        onProgress: (Long, Long, Double) -> Unit = { _, _, _ -> }
    ): Call {
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "下载失败")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onFailure("下载失败，响应码: ${response.code}")
                    return
                }

                val body = response.body
                if (body == null) {
                    onFailure("下载失败，响应体为空")
                    return
                }

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                val startTime = System.currentTimeMillis()

                try {
                    val inputStream = body.byteStream()
                    val outputStream = java.io.FileOutputStream(destFile)
                    val buffer = ByteArray(8192)
                    var len: Int

                    while (inputStream.read(buffer).also { len = it } != -1) {
                        outputStream.write(buffer, 0, len)
                        downloadedBytes += len

                        val elapsedTime = (System.currentTimeMillis() - startTime).toDouble() / 1000.0
                        val speed = if (elapsedTime > 0) downloadedBytes / elapsedTime else 0.0

                        onProgress(downloadedBytes, totalBytes, speed)
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()

                    onSuccess()
                } catch (e: Exception) {
                    onFailure("下载失败: ${e.message}")
                }
            }
        })

        return call
    }

    // ========== 内容管理相关API ==========

    private const val MY_ARTICLES_URL = "$BASE_URL/article_list"
    private const val MY_VIDEOS_URL = "$BASE_URL/myvideos"
    private const val MY_GIFTS_URL = "$BASE_URL/mygifts"
    private const val USER_REPOS_URL = "$BASE_URL/user/repos"
    private const val REPOS_UPDATE_URL = "$BASE_URL/repos/update"
    private const val EXPAND_STORAGE_URL = "$BASE_URL/coins/tospace"
    private const val GIFT_GROUPS_URL = "$BASE_URL/giftgroups"
    private const val GIFT_UPDATE_GROUP_URL = "$BASE_URL/gifts/updategroup"
    private const val GIFT_UPDATE_DESC_URL = "$BASE_URL/gifts/updatedesc"
    private const val GIFT_ADD_CODES_URL = "$BASE_URL/gifts/addcodes"
    private const val GIFT_DELETE_URL = "$BASE_URL/gifts/delete"
    private const val GIFT_DELETE_CODE_URL = "$BASE_URL/gifts/deletecode"
    private const val GIFT_DETAIL_URL = "$BASE_URL/gifts/detail"

    data class ManageArticle(
        val id: Int,
        val title: String,
        val author: String,
        val date: String,
        val category: String
    )

    data class ManageVideo(
        val id: Int,
        val name: String,
        val cover: String,
        val description: String,
        val link: String,
        val stock: Int
    )

    data class ManageShareCode(
        val id: Int,
        val name: String,
        val cover: String,
        val description: String,
        val stock: Int,
        val first: String,
        val grid: Int
    )

    data class ShareCodeDetail(
        val id: Int,
        val name: String,
        val cover: String,
        val description: String,
        val stock: Int,
        val grid: Int,
        val codes: List<String>
    )

    data class CategoryItem(
        val id: Int,
        val name: String
    )

    data class RepoInfo(
        val repoId: Int,
        val name: String,
        val description: String,
        val totalSpace: String,
        val totalSpaceKb: Int,
        val usedSpace: String,
        val usedSpaceKb: Int,
        val fileCount: Int,
        val createdAt: String,
        val likes: Int
    )

    data class ManageFile(
        val fileId: Int,
        val fileSize: String,
        val fileSizeKb: Int,
        val fileType: String,
        val originalName: String,
        val status: Int,
        val uploadTime: String,
        val repoId: Int = 0
    )

    data class UserRepoResponse(
        val success: Boolean,
        val repo: RepoInfo?,
        val files: List<ManageFile>,
        val userId: String
    )

    fun fetchMyArticles(
        accessToken: String,
        onSuccess: (List<ManageArticle>) -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val request = Request.Builder()
            .url(MY_ARTICLES_URL)
            .get()
            .addHeader("Authorization", "Bearer $accessToken")
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
                        val list = parseArticleList(responseBody)
                        onSuccess(list)
                    } catch (e: Exception) {
                        onFailure("解析失败: ${e.message}")
                    }
                } else {
                    onFailure("请求失败: ${response.code}")
                }
            }
        })
        return call
    }

    private fun parseArticleList(jsonStr: String): List<ManageArticle> {
        val list = mutableListOf<ManageArticle>()
        val jsonArray = org.json.JSONArray(jsonStr)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                ManageArticle(
                    id = obj.optInt("id", 0),
                    title = obj.optString("title", ""),
                    author = obj.optString("author", ""),
                    date = obj.optString("date", ""),
                    category = obj.optString("category", "")
                )
            )
        }
        return list
    }

    fun fetchMyVideos(
        accessToken: String,
        onSuccess: (List<ManageVideo>) -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val request = Request.Builder()
            .url(MY_VIDEOS_URL)
            .get()
            .addHeader("Authorization", "Bearer $accessToken")
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
                        val list = parseVideoList(responseBody)
                        onSuccess(list)
                    } catch (e: Exception) {
                        onFailure("解析失败: ${e.message}")
                    }
                } else {
                    onFailure("请求失败: ${response.code}")
                }
            }
        })
        return call
    }

    private fun parseVideoList(jsonStr: String): List<ManageVideo> {
        val list = mutableListOf<ManageVideo>()
        val jsonArray = org.json.JSONArray(jsonStr)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                ManageVideo(
                    id = obj.optInt("id", 0),
                    name = obj.optString("name", ""),
                    cover = obj.optString("cover", ""),
                    description = obj.optString("description", ""),
                    link = obj.optString("link", ""),
                    stock = obj.optInt("stock", 0)
                )
            )
        }
        return list
    }

    fun fetchMyShareCodes(
        accessToken: String,
        onSuccess: (List<ManageShareCode>) -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val request = Request.Builder()
            .url(MY_GIFTS_URL)
            .get()
            .addHeader("Authorization", "Bearer $accessToken")
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
                        val list = parseShareCodeList(responseBody)
                        onSuccess(list)
                    } catch (e: Exception) {
                        onFailure("解析失败: ${e.message}")
                    }
                } else {
                    onFailure("请求失败: ${response.code}")
                }
            }
        })
        return call
    }

    private fun parseShareCodeList(jsonStr: String): List<ManageShareCode> {
        val list = mutableListOf<ManageShareCode>()
        val jsonArray = org.json.JSONArray(jsonStr)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                ManageShareCode(
                    id = obj.optInt("id", 0),
                    name = obj.optString("name", ""),
                    cover = obj.optString("cover", ""),
                    description = obj.optString("description", ""),
                    stock = obj.optInt("stock", 0),
                    first = obj.optString("first", ""),
                    grid = obj.optInt("grid", 0)
                )
            )
        }
        return list
    }

    fun fetchGiftCategories(
        onSuccess: (List<CategoryItem>) -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val request = Request.Builder()
            .url(GIFT_GROUPS_URL)
            .get()
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
                        val list = parseCategoryList(responseBody)
                        onSuccess(list)
                    } catch (e: Exception) {
                        onFailure("解析失败: ${e.message}")
                    }
                } else {
                    onFailure("请求失败: ${response.code}")
                }
            }
        })
        return call
    }

    private fun parseCategoryList(jsonStr: String): List<CategoryItem> {
        val list = mutableListOf<CategoryItem>()
        val jsonArray = org.json.JSONArray(jsonStr)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                CategoryItem(
                    id = obj.optInt("id", 0),
                    name = obj.optString("name", "")
                )
            )
        }
        return list
    }

    fun fetchShareCodeDetail(
        accessToken: String,
        giftId: Int,
        onSuccess: (ShareCodeDetail) -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val request = Request.Builder()
            .url("$GIFT_DETAIL_URL?id=$giftId")
            .get()
            .addHeader("Authorization", "Bearer $accessToken")
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
                        val obj = org.json.JSONObject(responseBody)
                        val codesArray = obj.optJSONArray("codes")
                        val codes = mutableListOf<String>()
                        if (codesArray != null) {
                            for (i in 0 until codesArray.length()) {
                                codes.add(codesArray.getString(i))
                            }
                        }
                        val detail = ShareCodeDetail(
                            id = obj.optInt("id", 0),
                            name = obj.optString("name", ""),
                            cover = obj.optString("cover", ""),
                            description = obj.optString("description", ""),
                            stock = obj.optInt("stock", 0),
                            grid = obj.optInt("grid", 0),
                            codes = codes
                        )
                        onSuccess(detail)
                    } catch (e: Exception) {
                        onFailure("解析失败: ${e.message}")
                    }
                } else {
                    onFailure("请求失败: ${response.code}")
                }
            }
        })
        return call
    }

    fun updateShareCodeGroup(
        accessToken: String,
        giftId: Int,
        groupId: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val jsonBody = org.json.JSONObject()
        jsonBody.put("id", giftId)
        jsonBody.put("grid", groupId)

        val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(GIFT_UPDATE_GROUP_URL)
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure("请求失败: ${response.code}")
                }
            }
        })
        return call
    }

    fun updateShareCodeDescription(
        accessToken: String,
        giftId: Int,
        description: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val jsonBody = org.json.JSONObject()
        jsonBody.put("id", giftId)
        jsonBody.put("description", description)

        val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(GIFT_UPDATE_DESC_URL)
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure("请求失败: ${response.code}")
                }
            }
        })
        return call
    }

    fun addShareCodes(
        accessToken: String,
        giftId: Int,
        codes: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val jsonBody = org.json.JSONObject()
        jsonBody.put("id", giftId)
        jsonBody.put("codes", codes)

        val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(GIFT_ADD_CODES_URL)
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure("请求失败: ${response.code}")
                }
            }
        })
        return call
    }

    fun deleteShareCode(
        accessToken: String,
        giftId: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val request = Request.Builder()
            .url("$GIFT_DELETE_URL?id=$giftId")
            .get()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure("请求失败: ${response.code}")
                }
            }
        })
        return call
    }

    fun deleteSingleShareCode(
        accessToken: String,
        giftId: Int,
        code: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val jsonBody = org.json.JSONObject()
        jsonBody.put("id", giftId)
        jsonBody.put("code", code)

        val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(GIFT_DELETE_CODE_URL)
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure("请求失败: ${response.code}")
                }
            }
        })
        return call
    }

    fun fetchUserRepo(
        accessToken: String,
        onSuccess: (UserRepoResponse) -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val request = Request.Builder()
            .url(USER_REPOS_URL)
            .get()
            .addHeader("Authorization", "Bearer $accessToken")
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
                        val result = parseUserRepoResponse(responseBody)
                        onSuccess(result)
                    } catch (e: Exception) {
                        onFailure("解析失败: ${e.message}")
                    }
                } else {
                    onFailure("请求失败: ${response.code}")
                }
            }
        })
        return call
    }

    private fun parseUserRepoResponse(jsonStr: String): UserRepoResponse {
        val obj = org.json.JSONObject(jsonStr)
        val success = obj.optBoolean("success", false)
        val userId = obj.optString("userId", "")

        var repoInfo: RepoInfo? = null
        val repoObj = obj.optJSONObject("repo")
        if (repoObj != null) {
            repoInfo = RepoInfo(
                repoId = repoObj.optInt("repo_id", 0),
                name = repoObj.optString("name", ""),
                description = repoObj.optString("description", ""),
                totalSpace = repoObj.optString("total_space", ""),
                totalSpaceKb = repoObj.optInt("total_space_kb", 0),
                usedSpace = repoObj.optString("used_space", ""),
                usedSpaceKb = repoObj.optInt("used_space_kb", 0),
                fileCount = repoObj.optInt("file_count", 0),
                createdAt = repoObj.optString("created_at", ""),
                likes = repoObj.optInt("likes", 0)
            )
        }

        val files = mutableListOf<ManageFile>()
        val filesArray = obj.optJSONArray("files")
        val repoId = repoObj?.optInt("repo_id", 0) ?: 0
        if (filesArray != null) {
            for (i in 0 until filesArray.length()) {
                val fileObj = filesArray.getJSONObject(i)
                files.add(
                    ManageFile(
                        fileId = fileObj.optInt("file_id", 0),
                        fileSize = fileObj.optString("file_size", ""),
                        fileSizeKb = fileObj.optInt("file_size_kb", 0),
                        fileType = fileObj.optString("file_type", ""),
                        originalName = fileObj.optString("original_name", ""),
                        status = fileObj.optInt("status", 0),
                        uploadTime = fileObj.optString("upload_time", ""),
                        repoId = repoId
                    )
                )
            }
        }

        return UserRepoResponse(success, repoInfo, files, userId)
    }

    fun updateRepo(
        accessToken: String,
        repoId: Int,
        name: String,
        description: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val jsonBody = org.json.JSONObject()
        jsonBody.put("id", repoId)
        jsonBody.put("name", name)
        jsonBody.put("description", description)

        val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(REPOS_UPDATE_URL)
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure("请求失败: ${response.code}")
                }
            }
        })
        return call
    }

    fun expandStorage(
        accessToken: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ): Call {
        val request = Request.Builder()
            .url(EXPAND_STORAGE_URL)
            .get()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure("请求失败: ${response.code}")
                }
            }
        })
        return call
    }
}
