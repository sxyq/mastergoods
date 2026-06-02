package com.zhihuiji.app.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object SignatureIntegrityChecker {
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
            packageInfo.signingInfo?.apkContentsSigners.orEmpty().map { it.toByteArray() }
        } else {
            @Suppress("DEPRECATION")
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            packageInfo.signatures.orEmpty().map { it.toByteArray() }
        }
        if (signatures.isEmpty()) return false
        return signatures.any { signatureBytes ->
            sha256(signatureBytes) == normalizedExpected
        }
    }

    private fun sha256(input: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        return digest.joinToString(separator = "") { "%02X".format(it) }
    }

    private fun String.normalizeFingerprint(): String = replace(":", "").trim().uppercase()
}
