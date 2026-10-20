package pl.antithiefpro

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SecurityEvent(val time: Long, val type: String, val details: String)

object EventLog {
    private const val PREFS = "anti_thief_event_log"
    private const val DATA_KEY = "encrypted_events"
    private const val KEY_ALIAS = "AntiThiefPRO_EventLog_Key"
    private const val MAX_EVENTS = 200

    @Synchronized
    fun add(context: Context, type: String, details: String): Boolean {
        return try {
            val events = read(context).toMutableList()
            events.add(SecurityEvent(System.currentTimeMillis(), type, details))
            val limited = events.takeLast(MAX_EVENTS)
            val array = JSONArray()
            limited.forEach {
                array.put(JSONObject().put("time", it.time).put("type", it.type).put("details", it.details))
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(DATA_KEY, encrypt(array.toString()))
                .apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    fun read(context: Context): List<SecurityEvent> {
        return try {
            val encrypted = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(DATA_KEY, null) ?: return emptyList()
            val array = JSONArray(decrypt(encrypted))
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                SecurityEvent(item.optLong("time"), item.optString("type"), item.optString("details"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun formattedTime(time: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(time))

    private fun getOrCreateKey(): SecretKey {
        val store = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val combined = cipher.iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        require(combined.size > 12) { "Nieprawidłowy zaszyfrowany dziennik" }
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }
}
