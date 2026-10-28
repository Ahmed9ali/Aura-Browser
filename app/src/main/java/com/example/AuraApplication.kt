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
         * Checks whether the Linux DRM render node (/dev/dri/renderD128) is available.
         * Cloud/headless Android emulators often run without a DRM render node, which causes
         * "E/platform: Failed to open rendernode: No such file or directory" when WebView
         * attempts hardware layer allocation.
         */
        fun hasHardwareRenderNode(): Boolean {
            return try {
                val renderNode = File("/dev/dri/renderD128")
                renderNode.exists() && renderNode.canRead()
            } catch (_: Exception) {
                false
            }
        }
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
