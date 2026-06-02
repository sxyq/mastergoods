package com.zhihuiji.app.security

import android.os.Build
import android.os.Debug
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

object RuntimeSecurityGuard {
    private val fridaPorts = listOf(27042, 27043)
    private val rootBinaryPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/system/app/Superuser.apk",
        "/system/bin/.ext/.su",
        "/system/usr/we-need-root/su",
        "/cache/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su",
        "/system/framework/frida-agent.jar",
    )

    fun isHighRiskRuntime(): Boolean = isDebuggerAttached() || isFridaDetected() || isRooted()

    fun isDebuggerAttached(): Boolean = Debug.isDebuggerConnected() || Debug.waitingForDebugger()

    fun isRooted(): Boolean = hasTestKeys() || rootBinaryPaths.any { File(it).exists() }

    fun isFridaDetected(): Boolean = isFridaPortReachable() || hasFridaArtifactsInMaps()

    private fun hasTestKeys(): Boolean = Build.TAGS?.contains("test-keys") == true

    private fun isFridaPortReachable(): Boolean {
        return fridaPorts.any { port ->
            runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", port), 120)
                    true
                }
            }.getOrDefault(false)
        }
    }

    private fun hasFridaArtifactsInMaps(): Boolean {
        return runCatching {
            File("/proc/self/maps").useLines { lines ->
                lines.any { line ->
                    line.contains("frida", ignoreCase = true) ||
                        line.contains("gum-js-loop", ignoreCase = true) ||
                        line.contains("gmain", ignoreCase = true)
                }
            }
        }.getOrDefault(false)
    }
}
