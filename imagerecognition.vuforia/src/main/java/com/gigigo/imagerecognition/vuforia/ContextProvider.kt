package com.gigigo.imagerecognition.vuforia


import android.content.Context
import android.support.v7.app.AppCompatActivity

interface ContextProvider {
  fun getCurrentActivity(): AppCompatActivity
  fun isActivityContextAvailable(): Boolean
  fun getApplicationContext(): Context
  fun isApplicationContextAvailable(): Boolean
}
