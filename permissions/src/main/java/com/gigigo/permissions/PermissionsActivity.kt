package com.gigigo.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.gigigo.permissions.exception.Error
import com.gigigo.permissions.exception.PermissionException

class PermissionsActivity : AppCompatActivity() {

  //todo:
  //1º asking permission from open method, create public class with all permissions PERMISSIONS_REQUEST_LOCATION
  //2º Raise up show rationale to the integration app by-->finishWithoutPermissions(permissionException) and setting Error.setCode
  //3º Create another Navigator.open with Navigator.retry for avoid close activity permission for show retry


  private val PERMISSIONS_REQUEST_LOCATION = 1
  private val PERMISSIONS_REQUEST_CAMERA = 2

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_transparent)
    title = ""

    if (ContextCompat.checkSelfPermission(this@PermissionsActivity,
        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
      finishWithPermissionsGranted()
    } else {
      requestPermission()
    }
  }

  private fun requestPermission() {
    if (ContextCompat.checkSelfPermission(this,
        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

      if (ActivityCompat.shouldShowRequestPermissionRationale(this,
          Manifest.permission.CAMERA)) {
        var permissionException = PermissionException(Error.PERMISSION_RATIONALE_ERROR,
            resources.getString(R.string.camera_rationale_permission))

        Snackbar.make(window.decorView.rootView, permissionException.error,
            Snackbar.LENGTH_LONG)
            .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
              override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                super.onDismissed(transientBottomBar, event);
                finish();
              }
            })
            .setAction(resources.getString(R.string.camera_rationale_permission_button_text),
                { doRequestPermission() }).show()
        //todo check this for callback in app, in this case never finish activity and need another retry method like .open for check again permission from integration app
        // finishWithoutPermissions(permissionException)

      } else {
        doRequestPermission()
      }
    }
  }

  private fun doRequestPermission() {
    ActivityCompat.requestPermissions(this,
        arrayOf(Manifest.permission.CAMERA),
        PERMISSIONS_REQUEST_CAMERA)
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
      grantResults: IntArray) {

    when (requestCode) {
      PERMISSIONS_REQUEST_CAMERA -> {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          finishWithPermissionsGranted()
        } else {
          finishWithoutPermissions(
              PermissionException(Error.PERMISSION_ERROR,
                  resources.getString(R.string.denied_permission)
              ))
        }
      }
    }
  }

  private fun finishWithPermissionsGranted() {
    finish()
    onSuccess()
  }

  private fun finishWithoutPermissions(exception: PermissionException) {
    finish()//todo control this if is rationale not close activity and retry permission from rationale
    onError(exception)
  }

  companion object Navigator {

    var onSuccess: () -> Unit = {}
    var onError: (PermissionException) -> Unit = {}


    //todo create a retry method that call  to  doRequestPermission() wiothout close activity
    fun open(context: Context, onSuccess: () -> Unit = {},
        onError: (PermissionException) -> Unit = {}) {

      this.onSuccess = onSuccess
      this.onError = onError

      val intent = Intent(context, PermissionsActivity::class.java)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      context.startActivity(intent)
    }
  }
}
