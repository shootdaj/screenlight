package com.anshul.screenlight

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Screenlight.
 * Annotated with @HiltAndroidApp to trigger Hilt code generation and enable dependency injection.
 */
@HiltAndroidApp
class ScreenlightApplication : Application()
