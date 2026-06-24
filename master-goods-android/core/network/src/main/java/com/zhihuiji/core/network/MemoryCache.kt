package com.zhihuiji.core.network

import java.util.concurrent.ConcurrentHashMap

class CacheEntry<T>(
    val data: T,
    val timestamp: Long = System.currentTimeMillis(),
)

class MemoryCache {
    init {
        Companion.register(this)
    }

    private val cache = ConcurrentHashMap<String, CacheEntry<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, ttlMillis: Long): T? {
        val entry = cache[key] as? CacheEntry<T> ?: return null
        val now = System.currentTimeMillis()
        return if (now - entry.timestamp < ttlMillis) {
            entry.data
        } else {
            cache.remove(key)
            null
        }
    }

    fun <T> put(key: String, data: T) {
        cache[key] = CacheEntry(data)
    }

    fun invalidate(key: String) {
        cache.remove(key)
    }

    fun invalidatePrefix(prefix: String) {
        cache.entries.removeIf { it.key.startsWith(prefix) }
    }

    fun clear() {
        cache.clear()
    }

    companion object {
        private val registeredCaches = ConcurrentHashMap.newKeySet<MemoryCache>()

        private fun register(cache: MemoryCache) {
            registeredCaches.add(cache)
        }

        fun clearAllRegistered() {
            registeredCaches.forEach { it.clear() }
        }
    }
}
