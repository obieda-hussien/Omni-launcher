package app.lawnchair.util

import android.app.ActivityManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@SmallTest
@RunWith(AndroidJUnit4::class)
class DeviceTierTest {

    @Before
    fun setup() {
        val resetField = DeviceTierManager::class.java.getDeclaredField("instance")
        resetField.isAccessible = true
        resetField.set(null, null)
    }

    @Test
    fun testLowTier_LowRamDevice() {
        val context = mock<Context>()
        val activityManager = mock<ActivityManager>()

        whenever(context.applicationContext).thenReturn(context)
        whenever(context.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(activityManager)

        whenever(activityManager.isLowRamDevice).thenReturn(true)
        whenever(activityManager.memoryClass).thenReturn(512)

        val manager = DeviceTierManager.getInstance(context)
        assertEquals(DeviceTier.LOW, manager.tier)
    }

    @Test
    fun testLowTier_LowMemoryClass() {
        val context = mock<Context>()
        val activityManager = mock<ActivityManager>()

        whenever(context.applicationContext).thenReturn(context)
        whenever(context.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(activityManager)

        whenever(activityManager.isLowRamDevice).thenReturn(false)
        whenever(activityManager.memoryClass).thenReturn(256)

        val manager = DeviceTierManager.getInstance(context)
        assertEquals(DeviceTier.LOW, manager.tier)
    }

    @Test
    fun testMidTier() {
        val context = mock<Context>()
        val activityManager = mock<ActivityManager>()

        whenever(context.applicationContext).thenReturn(context)
        whenever(context.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(activityManager)

        whenever(activityManager.isLowRamDevice).thenReturn(false)
        whenever(activityManager.memoryClass).thenReturn(512)

        val manager = DeviceTierManager.getInstance(context)
        assertEquals(DeviceTier.MID, manager.tier)
    }

    @Test
    fun testHighTier() {
        val context = mock<Context>()
        val activityManager = mock<ActivityManager>()

        whenever(context.applicationContext).thenReturn(context)
        whenever(context.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(activityManager)

        whenever(activityManager.isLowRamDevice).thenReturn(false)
        whenever(activityManager.memoryClass).thenReturn(768)

        val manager = DeviceTierManager.getInstance(context)
        assertEquals(DeviceTier.HIGH, manager.tier)
    }
}
