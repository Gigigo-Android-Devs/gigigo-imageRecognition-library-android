package com.gigigo.irfragment.utils

import com.gigigo.irfragment.core.IrScanResult
import com.vuforia.TargetSearchResult

fun TargetSearchResult.toIrScanResult(): IrScanResult {

  return IrScanResult(
    id = this.uniqueTargetId ?: "",
    name = this.targetName ?: "",
    metaData = this.metaData ?: ""
  )
}