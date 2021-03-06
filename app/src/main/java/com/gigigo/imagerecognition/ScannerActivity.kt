package com.gigigo.imagerecognition

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.gigigo.irfragment.core.IrScanResult
import com.gigigo.irfragment.reco.IRScannerListener
import com.gigigo.irfragment.reco.ImageRecognitionFragment
import kotlinx.android.synthetic.main.activity_scanner.fab
import kotlinx.android.synthetic.main.activity_scanner.toolbar

class ScannerActivity : AppCompatActivity(), IRScannerListener {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_scanner)
    setSupportActionBar(toolbar)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    initView()
  }

  private fun initView() {
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    val fragmentManager = supportFragmentManager
    val fragmentTransaction = fragmentManager.beginTransaction()

    val fragment = ImageRecognitionFragment.newInstance(
      licenseKey = LICENSE_KEY,
      accessKey = ACCESS_KEY,
      secretKey = SECRET_KEY
    )

    fab.setOnClickListener { fragment.resetScanner() }
    fragmentTransaction.add(R.id.fragment_container, fragment)
    fragmentTransaction.commit()
  }

  override fun onScanResult(scanResult: IrScanResult) {
    Toast.makeText(this, "Code: ${scanResult.name}", Toast.LENGTH_SHORT).show()
  }
}
