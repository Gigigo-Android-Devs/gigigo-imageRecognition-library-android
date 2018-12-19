package com.gigigo.irfragment.reco

import com.gigigo.irfragment.core.IrScanResult

interface IRScannerListener {
  fun onScanResult(scanResult: IrScanResult)
}