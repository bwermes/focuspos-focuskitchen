package com.amorphik.focuskitchen

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Base64
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CryptoService {
    fun generateAuthToken(ctx: Context): String? {
        return if (prefs.licenseVerified) {
            val sigString = "${prefs.license!!.key}:${prefs.venueKey}:${prefs.deviceId}"
            val signature = prefs.license!!.secret?.let { generateHash(sigString, it) }
            "${prefs.license!!.key}:$signature"
        } else {
            null
        }
    }

    @Throws(Exception::class)
    fun generateHash(message: String, key: String): String {
        val hashingAlgorithm = "HmacSHA256" //or "HmacSHA1", "HmacSHA512"
        val bytes = hmac(hashingAlgorithm, key.toByteArray(), message.toByteArray())
        return Base64.encodeToString(bytes, Base64.DEFAULT).replace("\\s".toRegex(), "")
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
    fun hmac(algorithm: String, key: ByteArray, message: ByteArray): ByteArray {
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(key, algorithm))
        return mac.doFinal(message)
    }
}

