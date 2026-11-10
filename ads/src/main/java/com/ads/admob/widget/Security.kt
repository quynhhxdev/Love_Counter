package com.ads.admob.widget

import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object Security {
    private const val TAG = "IABSecurity"
    private const val KEY_FACTORY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"

    fun verifyPurchase(base64PublicKey: String, signedData: String, signature: String): Boolean {
        if (base64PublicKey.isBlank() || signedData.isBlank() || signature.isBlank()) {
            Log.e(TAG, "Purchase verification failed: missing data or key")
            return false
        }
        val key = generatePublicKey(base64PublicKey) ?: return false
        return verify(key, signedData, signature)
    }

    private fun generatePublicKey(encodedPublicKey: String): PublicKey? {
        return try {
            val decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate public key: ${e.message}")
            null
        }
    }

    private fun verify(publicKey: PublicKey, signedData: String, signature: String): Boolean {
        return try {
            val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
            sig.initVerify(publicKey)
            sig.update(signedData.toByteArray())
            val decodedSignature = Base64.decode(signature, Base64.DEFAULT)
            sig.verify(decodedSignature)
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification failed: ${e.message}")
            false
        }
    }
}
