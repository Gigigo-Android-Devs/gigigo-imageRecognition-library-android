package com.gigigo.imagerecognition.vuforia

import android.app.Activity
import android.content.Context

interface ContextProvider {
  fun getCurrentActivity(): Activity
  fun isActivityContextAvailable(): Boolean
  fun getApplicationContext(): Context
  fun isApplicationContextAvailable(): Boolean
}
