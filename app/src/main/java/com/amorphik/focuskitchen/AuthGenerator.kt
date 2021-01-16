package com.amorphik.focuskitchen

import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class AuthGenerator {
    companion object {
        fun generateHash(message: String, key: String): String {
            val hashingAlgorithm = "HmacSHA256"
            val bytes = hmac(hashingAlgorithm, key.toByteArray(), message.toByteArray())
            return Base64.encodeToString(bytes, Base64.DEFAULT).replace("\\s".toRegex(), "")
        }

        fun hmac(algorithm: String, key: ByteArray, message: ByteArray): ByteArray {
            val mac = Mac.getInstance(algorithm)
            mac.init(SecretKeySpec(key, algorithm))
            return mac.doFinal(message)
        }
    }
}