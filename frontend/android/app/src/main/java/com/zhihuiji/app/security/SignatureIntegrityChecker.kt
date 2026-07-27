package com.zhihuiji.app.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object SignatureIntegrityChecker {
    private val hexDigits = "0123456789ABCDEF".toCharArray()

    fun isSignatureTrusted(context: Context, expectedSha256: String): Boolean {
        val normalizedExpected = expectedSha256.normalizeFingerprint()
        if (normalizedExpected.isBlank()) return true
        val packageManager = context.packageManager
        val packageName = context.packageName
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
            packageInfo.signingInfo?.apkContentsSigners.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            packageInfo.signatures.orEmpty()
        }
        return signatures.isNotEmpty() &&
            signatures.any { sha256(it.toByteArray()) == normalizedExpected }
    }

    private fun sha256(input: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        val hexChars = CharArray(digest.size * 2)
        var index = 0
        for (byte in digest) {
            val value = byte.toInt() and 0xFF
            hexChars[index++] = hexDigits[value ushr 4]
            hexChars[index++] = hexDigits[value and 0x0F]
        }
        return String(hexChars)
    }

    private fun String.normalizeFingerprint(): String = replace(":", "").trim().uppercase()
}
