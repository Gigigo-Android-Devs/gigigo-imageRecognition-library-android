package com.gigigo.imagerecognition

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import com.gigigo.imagerecognition.vuforia.ContextProvider
import com.gigigo.imagerecognition.vuforia.VuforiaView
import com.gigigo.imagerecognition.vuforia.credentials.VuforiaCredentials
import com.gigigo.permissions.PermissionsActivity
import com.gigigo.permissions.exception.Error
import com.vuforia.TargetSearchResult

class ImageRecognizerActivity : AppCompatActivity(), VuforiaView.ResultHandler {

  private lateinit var credentials: VuforiaCredentials

  private var vuforiaView: VuforiaView? = null
  private lateinit var contextProvider: ContextProvider

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_imagerecognizer)

    initViews()
  }

  private fun initViews() {
    initToolbar()

    initVuforia()
    startImageRecognition()
  }

  private fun initToolbar() {

    val toolbar = findViewById(R.id.toolbar) as Toolbar

    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setDisplayShowHomeEnabled(true)

    toolbar.setNavigationOnClickListener { onBackPressed() }

    title = getString(R.string.imagerecognizer_vuforia_title)
  }

  private fun getLicenseKey(): String = intent.getStringExtra(LICENSE_KEY)
  private fun getClientAccessKey(): String = intent.getStringExtra(CLIENT_ACCESS_KEY)
  private fun getClientSecretKey(): String = intent.getStringExtra(CLIENT_SECRET_KEY)


  private fun initVuforia() {
    contextProvider = object : ContextProvider {
      override fun getCurrentActivity(): AppCompatActivity = this@ImageRecognizerActivity
      override fun isActivityContextAvailable(): Boolean = true
      override fun getApplicationContext(): Context = this@ImageRecognizerActivity.application.applicationContext
      override fun isApplicationContextAvailable(): Boolean = true
    }
    credentials = VuforiaCredentials(getLicenseKey(), getClientAccessKey(), getClientSecretKey())
    var contentView = findViewById(R.id.content_frame)
    vuforiaView = VuforiaView(this, contentView, contextProvider, credentials)
  }

  private fun startImageRecognition() {
    PermissionsActivity.open(contextProvider.getApplicationContext(),
        onSuccess = {
          startVuforia()
        },
        onError = { permissionException ->
          when (permissionException.code) {
            Error.PERMISSION_ERROR -> {
              Toast.makeText(contextProvider.getCurrentActivity(), permissionException.error,
                  Toast.LENGTH_SHORT).show()
            }
            else -> {

            }
          }
        }
    )
  }


  fun startVuforia() {
    var contentFrame = findViewById(R.id.content_frame) as FrameLayout
    contentFrame.addView(vuforiaView)
  }

  public override fun onResume() {
    super.onResume()
    imageRecognizerActivity = this
    vuforiaView?.setResultHandler(this)
  }

  public override fun onPause() {
    super.onPause()
    vuforiaView?.stopCamera()
  }

  public override fun onDestroy() {
    super.onDestroy()
  }

  override fun handleResult(result: TargetSearchResult?) {
    Log.d("VUFORIA", result?.uniqueTargetId)
    vuforiaView?.stopCamera()

    var intent = Intent()
    intent.putExtra(VUFORIA_RESULT, result?.uniqueTargetId)
    setResult(RESULT_OK, intent)
    finish()
  }


  companion object Navigator {
    val VUFORIA_RESULT = "vuforia_result"
    private val LICENSE_KEY = "license_key"
    private val CLIENT_ACCESS_KEY = "client_access_key"
    private val CLIENT_SECRET_KEY = "client_secret_key"
    private var imageRecognizerActivity: ImageRecognizerActivity? = null

    fun open(activity: AppCompatActivity, requestCode: Int, licenseKey: String,
        clientAccessKey: String,
        clientSecretKey: String) {
      val intent = Intent(activity, ImageRecognizerActivity::class.java)
      intent.putExtra(LICENSE_KEY, licenseKey)
      intent.putExtra(CLIENT_ACCESS_KEY, clientAccessKey)
      intent.putExtra(CLIENT_SECRET_KEY, clientSecretKey)
      activity.startActivityForResult(intent, requestCode)
    }

    fun finish() {
      imageRecognizerActivity?.finish()
    }
  }
}