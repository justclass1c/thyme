package com.class1c.thyme

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp // hilt creates a singleton Hilt_ThymeApplication
class ThymeApplication : Application() {
}