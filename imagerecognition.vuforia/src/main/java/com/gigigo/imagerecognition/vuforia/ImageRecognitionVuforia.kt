package com.gigigo.imagerecognition.vuforia

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import android.widget.Toast
import com.gigigo.imagerecognition.Constants
import com.gigigo.imagerecognition.Credentials
import com.gigigo.imagerecognition.ImageRecognition
import com.gigigo.imagerecognition.vuforia.credentials.VuforiaCredentials
import com.gigigo.permissions.PermissionsActivity
import com.gigigo.permissions.exception.Error as PermissionError

/**
 * This is a suitable implementation for image recognition module, in fact this this Vuforia
 * ImageRecognition interface specialization. An instance of this class would call Vuforia SDK
 * when startImageRecognition is called.
 *
 *
 * This class is already managing Camera permissions implementation.
 */
class ImageRecognitionVuforia : ImageRecognition<ContextProvider> {
  private lateinit var credentials: VuforiaCredentials

  override fun setContextProvider(ctxProvider: ContextProvider) {
    contextProvider = ctxProvider
  }

  /**
   * Checks permissions and starts Image recognitio activity using given credentials. If Permissions
   * were not granted User will be notified. If credentials are not valid you'll have an error log
   * message.
   *
   * @param vuforiaCredentials interface implementation with Vuforia keys
   */
  override fun startImageRecognition(vuforiaCredentials: Credentials) {
    credentials = digestCredentials(vuforiaCredentials)

    PermissionsActivity.open(contextProvider.getApplicationContext(),
        onSuccess = {
          startImageRecognitionActivity()
        },
        onError = { permissionException ->
          when(permissionException.code) {
            PermissionError.PERMISSION_ERROR -> {
              Toast.makeText(contextProvider.getCurrentActivity(), permissionException.error,
                  Toast.LENGTH_SHORT).show()
            }
            else -> {

            }
          }
        }
    )
  }

  private fun digestCredentials(credentials: Credentials): VuforiaCredentials = VuforiaCredentials(
      credentials.licensekey, credentials.clientAccessKey,
      credentials.clientSecretKey)

  private fun startImageRecognitionActivity() {
    val imageRecognitionIntent = Intent(contextProvider.getApplicationContext(),
        VuforiaActivity::class.java)
    imageRecognitionIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

    val b = Bundle()
    b.putParcelable(Constants.IMAGE_RECOGNITION_CREDENTIALS, credentials)

    imageRecognitionIntent.putExtra(Constants.IMAGE_RECOGNITION_CREDENTIALS, b)
    contextProvider.getApplicationContext().startActivity(imageRecognitionIntent)
  }

  companion object {
    private lateinit var contextProvider: ContextProvider
    private lateinit var recognizedCallback: (String) -> Unit

    fun onRecognizedPattern(callback: (String) -> Unit) {
      recognizedCallback = callback
    }

    fun sendRecognizedPattern(intent: Intent) {
      if (intent.extras.containsKey(contextProvider.getApplicationContext().packageName)
          && intent.extras.containsKey(Constants.PATTERN_ID)) {

        var code: String = intent.getStringExtra(Constants.PATTERN_ID)

        if (code.isNotBlank()) {
          recognizedCallback(code)
        }
      }
    }
  }
}
