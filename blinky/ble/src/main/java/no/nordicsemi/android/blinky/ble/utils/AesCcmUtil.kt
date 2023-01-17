package no.nordicsemi.android.blinky.ble.utils

import android.util.Log
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class AesCcmUtil {
    companion object {
        fun encrypt(src: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
            Security.addProvider(BouncyCastleProvider())
            val parameterSpec = GCMParameterSpec(80, nonce)
            val cipher: Cipher = Cipher.getInstance("AES/CCM/NoPadding")
            val secretKeySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec)
            var encrypted = ByteArray(cipher.getOutputSize(src.size))
            var res = cipher.update(src, 0, src.size, encrypted, 0)
            cipher.doFinal(encrypted, res)

            return encrypted
//        dst = encrypted.copyOfRange(0, src.size)
//        tag = encrypted.copyOfRange(src.size, encrypted.size)
//
//        return false
        }

        fun decrypt(src: ByteArray, key: ByteArray, nonce: ByteArray): ByteArray {
            Security.addProvider(BouncyCastleProvider())
            val parameterSpec = GCMParameterSpec(80, nonce)
            val cipher: Cipher = Cipher.getInstance("AES/CCM/NoPadding")
            val secretKeySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec)
            var plaintext = ByteArray(cipher.getOutputSize(src.size))
            Log.d("AesCcmUtil", "decrypt:plaintext.size=${plaintext.size},debug")
            val res = cipher.update(src, 0, src.size, plaintext, 0)
            cipher.doFinal(plaintext, res)

            return plaintext
//        dst = encrypted.copyOfRange(0, src.size)
//        tag = encrypted.copyOfRange(src.size, encrypted.size)
//
//        return false
        }
    }
}