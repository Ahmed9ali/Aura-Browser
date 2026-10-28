package com.example

import android.app.Application
import android.os.Build
import android.webkit.WebView
import java.io.File

class AuraApplication : Application() {
    companion object {
        var instance: AuraApplication? = null
            private set

        /**
         * Checks whether the Linux DRM render node (/dev/dri/renderD128) is actually present
         * and openable. Cloud/headless Android emulators often lack a DRM render node, which causes
         * "E/platform: Failed to open rendernode: No such file or directory" when hardware
         * gralloc/WebView layers attempt to open it.
         */
        fun hasHardwareRenderNode(): Boolean {
            return try {
                val renderNode = File("/dev/dri/renderD128")
                if (!renderNode.exists() || !renderNode.canRead()) {
                    false
                } else {
                    renderNode.inputStream().use { true }
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    override fun attachBaseContext(base: android.content.Context?) {
        super.attachBaseContext(base)
        configureGraphicsEnvironment()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        configureGraphicsEnvironment()
        cleanCorruptedVariationsSeeds()
    }

    private fun configureGraphicsEnvironment() {
        try {
            if (!hasHardwareRenderNode()) {
                System.setProperty("debug.hwui.renderer", "skiagl")
                System.setProperty("debug.hwui.use_vulkan", "false")
                System.setProperty("hwui.disable_vsync", "true")
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Cleans up stale, unsigned, or corrupt Chromium variations seed files
     * from the app's WebView data directory before the WebView engine starts.
     * This prevents variations_seed_loader.cc "Seed missing signature" warnings.
     */
    private fun cleanCorruptedVariationsSeeds() {
        try {
            val appDataDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                dataDir
            } else {
                filesDir.parentFile
            }

            if (appDataDir != null && appDataDir.exists()) {
                val webViewDir = File(appDataDir, "app_webview")
                if (webViewDir.exists() && webViewDir.isDirectory) {
                    webViewDir.listFiles()?.forEach { file ->
                        if (file.name.contains("variations", ignoreCase = true) ||
                            file.name.contains("seed", ignoreCase = true)
                        ) {
                            try {
                                file.delete()
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}
