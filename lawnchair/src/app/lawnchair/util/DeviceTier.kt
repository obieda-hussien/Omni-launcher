package app.lawnchair.util

import android.app.ActivityManager
import android.content.Context

enum class DeviceTier {
    LOW,
    MID,
    HIGH
}

class DeviceTierManager private constructor(context: Context) {
    val tier: DeviceTier

    init {
        // Safe check for null to prevent crashes when testing with a mock context
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?

        val isLowRam = activityManager?.isLowRamDevice ?: false
        val memoryClass = activityManager?.memoryClass ?: 512

        // We use 256MB as the threshold for memoryClass.
        // On modern Android (e.g., Android 11+), devices with ~2-4GB RAM typically
        // report a memoryClass of 256MB or lower. Flagships (8GB+) report 512MB+.
        val threshold = 256

        tier = if (isLowRam || memoryClass <= threshold) {
            DeviceTier.LOW
        } else if (memoryClass <= 512) {
            DeviceTier.MID
        } else {
            DeviceTier.HIGH
        }
    }

    companion object {
        @Volatile
        private var instance: DeviceTierManager? = null

        fun getInstance(context: Context): DeviceTierManager {
            return instance ?: synchronized(this) {
                instance ?: DeviceTierManager(context.applicationContext ?: context).also { instance = it }
            }
        }
    }
}
