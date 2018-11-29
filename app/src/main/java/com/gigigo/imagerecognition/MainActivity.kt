package com.gigigo.imagerecognition

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {
  private val IR_REQUEST_CODE = 1

//  private val licenseKey: String = "Abi5nL//////AAAAAXzdWR6MxEppinKbZ9qJjhU7Op5/+8Pwm8tdYfI4f3zFmRweYqowENwgiOUAtaiIH06OpQFISbhX9Linf/uq5JXUADO/MFrnbzy/UIuA3whurbD+Q18bV3uRrm2FtvF64fWdH7R1GoAbOEL6wbF621Da0JJ4uVYAZEYOga/6C4fBEtf0LpKoetdNIVpIxvWsIWHRNVWX41gbRTmwSqCnoV1axtSqBAalAx5Oq/GjoD4a8isoBRJMhkIEOR+4Q7lbyJrQatD+9TqINi9wAuBY9/atNKA27AzMpnQcuAaSr2rv8Y8r3wtk7yQY7oTm8CrBMLri+TdEZoF6Z/TdZaupRaqrlKZqtptOme0zoodbOTVe"
//  private val clientAccessKey: String = "fe4d316136ea6b7ee5faa72c4884e33805128b08"
//  private val clientSecretKey: String = "670f15bb4cd34c1621a892ced5321896c0b70df6"

  //asv test with ferringo keys 4 test
  private val licenseKey: String = "AX9Undz/////AAAACDK6kwxi7EuGsaT0cL4ZldgYjV3fE7Cb+Lh8Kb/UaGSWsfztzZqCqz3esySJezr/klkdnO3XxJoSAkvoAgU7t5JqYOoD5h2B+bleatSSi43sfMqBiEjXbpAJLplJPt/P5C/uGusgB6+0eELSGECCWfwZXMpSn7bGP3BKABNQG5kEEHUbD2w5q9cDfkGc8LsLACEqcUVChzQ2r+EZSGhs1gVUA5lqnyhBZbesjdq2YLunnKk+9+oGOfDNyra/SYpa8ucho0shOpCshigi5HmRfv+42D2DbmJX/gPY5o4yCvFMYlS8Ujf9eL54jHjvuk/gZpUaQCG620/FCJloZ+0pnuvnqFcgsSOvRA7aXd6hf4GS"
  private val clientAccessKey: String = "30e3675263a04e3ae11a47e0a8cfb01da0262f8c"
  private val clientSecretKey: String = "3d5e62875b15dd9261e5fc8d6c7882feced52037"

  private lateinit var licenseKeyTv: TextView
  private lateinit var accessKeyTv: TextView
  private lateinit var secretKeyTv: TextView
  private lateinit var codeTv: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    initViews()

    licenseKeyTv.text = "license key: ${licenseKey}"
    accessKeyTv.text = "access key: ${clientAccessKey}"
    secretKeyTv.text = "secret key: ${clientSecretKey}"
  }

  private fun initViews() {
    initToolbar()

    licenseKeyTv = findViewById(R.id.imagerecognizer_license_tv) as TextView
    accessKeyTv = findViewById(R.id.imagerecognizer_accessKey_tv) as TextView
    secretKeyTv = findViewById(R.id.imagerecognizer_secretKey_tv) as TextView
    codeTv = findViewById(R.id.imagerecognizer_code_tv) as TextView

    val startButton = findViewById(R.id.start_vuforia_button) as Button
    startButton.setOnClickListener({ startVuforiaForResult() })
  }

  private fun initToolbar() {

    val toolbar = findViewById(R.id.toolbar) as Toolbar

    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(false)

    toolbar.setNavigationOnClickListener { onBackPressed() }

    title = getString(R.string.imagerecognizer_title)
  }


  private fun startVuforia() {
    ImageRecognizerActivity.open(this, licenseKey = licenseKey,
        clientAccessKey = clientAccessKey,
        clientSecretKey = clientSecretKey)
  }

  private fun startVuforiaForResult() {
    ImageRecognizerActivity.openForResult(this, IR_REQUEST_CODE, licenseKey = licenseKey,
        clientAccessKey = clientAccessKey,
        clientSecretKey = clientSecretKey)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (resultCode) {
      RESULT_OK -> {
        if (requestCode == IR_REQUEST_CODE) {

          //asv esto esta muy mal
           var code = data?.extras?.getString(ImageRecognizerActivity.VUFORIA_RESULT)
          showResponseCode(code)

         // var code = data?.extras?.getSerializable (ImageRecognizerActivity.VUFORIA_RESULT)

        }
      }
      else -> {

      }
    }
  }

  private fun showResponseCode(code: String?) {
    codeTv.text = "CODE= $code"
  }
}