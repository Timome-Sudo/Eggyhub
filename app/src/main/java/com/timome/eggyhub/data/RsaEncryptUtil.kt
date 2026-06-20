package com.timome.eggyhub.data

import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import org.json.JSONObject

/**
 * RSA加密工具类
 *
 * 对 { email, password, timestamp } 进行JSON序列化后
 * 使用RSA公钥加密，并输出为十六进制字符串
 *
 * 与 temp/LoginActivity.java 的 encryptLoginData() 方法保持完全一致
 */
object RsaEncryptUtil {

    /**
     * 服务器RSA公钥（PEM格式）
     */
    private const val PUBLIC_KEY_PEM = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoGJBrgcKyxkFVSLF8kTX\n" +
            "9bW7tkoJ1IKwDxC9UpZe7uKwB+t3tU+fegu/d6zhOeEUmLLfSGmvp3ZI1RrB9Y02\n" +
            "k8AGotz9NLmr9zQciBEXmV/YkmoyK72cZnMJbq2hYODc02tEV8ITBwAwbhvD81g5\n" +
            "H/WTN16MXjA1Mpdt33qGQ87SEPTsQmWZjWfzBWq5vbC2mUxvzN6hgBs/NpLuBOLt\n" +
            "Wy7e2hvLkTTQnqnhJIzg/H2xDYHUQXZSCqi8uVYnma1SRy+lV+wZ+h26zavesJTX\n" +
            "qqD5CUE2jINiT/84DywH8W4gJSD99fa58QDgpr1MFUzRsRs9skJBq80Ds8joOxCO\n" +
            "jQIDAQAB\n" +
            "-----END PUBLIC KEY-----"

    /**
     * 懒加载的RSA公钥对象
     */
    private val publicKey: PublicKey by lazy {
        val keyString = PUBLIC_KEY_PEM
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")

        val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        keyFactory.generatePublic(keySpec)
    }

    /**
     * 加密登录数据
     *
     * @param email 用户邮箱
     * @param password 用户密码
     * @return 加密后的十六进制字符串，失败返回null
     */
    fun encryptLoginData(email: String, password: String): String? {
        return try {
            val timestamp = System.currentTimeMillis() / 1000.0

            val jsonObject = JSONObject()
            jsonObject.put("email", email)
            jsonObject.put("password", password)
            jsonObject.put("timestamp", timestamp)
            val data = jsonObject.toString()

            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)

            val dataBytes = data.toByteArray(charset("UTF-8"))
            val encryptedBytes = cipher.doFinal(dataBytes)

            val sb = StringBuilder()
            for (b in encryptedBytes) {
                sb.append(String.format("%02x", b))
            }
            sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
