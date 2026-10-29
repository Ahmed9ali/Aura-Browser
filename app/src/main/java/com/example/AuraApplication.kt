package com.example

import android.app.Application
import java.io.File

class AuraApplication : Application() {
    companion object {
        var instance: AuraApplication? = null
            private set

        /**
         * Checks whether the Linux DRM render node (/dev/dri/renderD128) is present
         * and openable. Cloud/headless Android emulators lack a DRM render node.
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
    }
}
