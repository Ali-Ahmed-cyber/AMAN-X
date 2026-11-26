package com.amanx.app

import android.app.Application

/**
 * Keeps Robolectric unit tests isolated from production startup side effects
 * such as WorkManager scheduling and persistent application initialization.
 */
class TestAmanXApplication : Application()
