package jiren.service.security

import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object EncryptionService {

    private fun setKey(myKey: String): SecretKeySpec? {
        val sha: MessageDigest?
        try {
            sha = MessageDigest.getInstance("SHA-1")
            var convertedKey = myKey.toByteArray(charset("UTF-8"))
            convertedKey = sha.digest(convertedKey)
            convertedKey = convertedKey.copyOf(16)
            return SecretKeySpec(convertedKey, "AES")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun encrypt(strToEncrypt: String, key: String?): String? {
        try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, setKey(key!!))
            return Base64.getEncoder()
                .encodeToString(cipher.doFinal(strToEncrypt.toByteArray(charset("UTF-8"))))
        } catch (e: Exception) {
            println("Error while encrypting: $e")
        }
        return null
    }

    fun decrypt(strToDecrypt: String?, key: String?): String? {
        try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING")
            cipher.init(Cipher.DECRYPT_MODE, setKey(key!!))
            return String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)))
        } catch (e: Exception) {
            println("Error while decrypting: $e")
        }
        return null
    }
}