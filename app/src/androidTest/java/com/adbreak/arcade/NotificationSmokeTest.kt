package com.adbreak.arcade

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adbreak.arcade.notification.NotificationController
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationSmokeTest {
    @Test fun notificationControllerCreatesChannelWithoutCrash() { NotificationController(ApplicationProvider.getApplicationContext<Context>()).ensureChannel() }
}
