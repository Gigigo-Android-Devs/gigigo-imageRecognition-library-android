package com.gigigo.imagerecognition

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.startScannerButton

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    startScannerButton.setOnClickListener {
      val intent = Intent(this, ScannerActivity::class.java)
      startActivity(intent)
    }
  }
}
