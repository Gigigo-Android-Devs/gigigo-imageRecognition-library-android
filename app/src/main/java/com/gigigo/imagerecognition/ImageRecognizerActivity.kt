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
import javax.crypto.SecretKey

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

//  private val licenseKey: String = "AX9Undz/////AAAACDK6kwxi7EuGsaT0cL4ZldgYjV3fE7Cb+Lh8Kb/UaGSWsfztzZqCqz3esySJezr/klkdnO3XxJoSAkvoAgU7t5JqYOoD5h2B+bleatSSi43sfMqBiEjXbpAJLplJPt/P5C/uGusgB6+0eELSGECCWfwZXMpSn7bGP3BKABNQG5kEEHUbD2w5q9cDfkGc8LsLACEqcUVChzQ2r+EZSGhs1gVUA5lqnyhBZbesjdq2YLunnKk+9+oGOfDNyra/SYpa8ucho0shOpCshigi5HmRfv+42D2DbmJX/gPY5o4yCvFMYlS8Ujf9eL54jHjvuk/gZpUaQCG620/FCJloZ+0pnuvnqFcgsSOvRA7aXd6hf4GS"
//  private val clientAccessKey: String = "30e3675263a04e3ae11a47e0a8cfb01da0262f8c"
//  private val clientSecretKey: String = "3d5e62875b15dd9261e5fc8d6c7882feced52037"


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
    Log.d("VUFORIA", result.toString())

    //el error igual lo provoca esto
    //  vuforiaView?.stopCamera()
//asv esto está reguleras,no?? solo manda un dato y deberia enviar todo el result
    var intent = Intent(this, MainActivity::class.java)
    intent.putExtra(VUFORIA_RESULT, result?.uniqueTargetId)

    //  asv wtf
    setResult(RESULT_OK, intent)
    finish()
  }


  companion object Navigator {
    val VUFORIA_RESULT = "vuforia_result"
    private val LICENSE_KEY = "license_key"
    private val CLIENT_ACCESS_KEY = "client_access_key"
    private val CLIENT_SECRET_KEY = "client_secret_key"
    private var imageRecognizerActivity: ImageRecognizerActivity? = null

    fun openForResult(activity: AppCompatActivity, requestCode: Int, licenseKey: String,
        clientAccessKey: String,
        clientSecretKey: String) {
      val intent = Intent(activity, ImageRecognizerActivity::class.java)
      intent.putExtra(LICENSE_KEY, licenseKey)
      intent.putExtra(CLIENT_ACCESS_KEY, clientAccessKey)
      intent.putExtra(CLIENT_SECRET_KEY, clientSecretKey)
      activity.startActivityForResult(intent, requestCode)
    }

    fun open(activity: AppCompatActivity, licenseKey: String,
        clientAccessKey: String,
        clientSecretKey: String) {
      val intent = Intent(activity, ImageRecognizerActivity::class.java)
      intent.putExtra(LICENSE_KEY, licenseKey)
      intent.putExtra(CLIENT_ACCESS_KEY, clientAccessKey)
      intent.putExtra(CLIENT_SECRET_KEY, clientSecretKey)
      activity.startActivity(intent)
    }

    fun finish() {
      imageRecognizerActivity?.finish()
    }
  }
}