package com.gigigo.irfragment.utils

import com.gigigo.irfragment.core.IrScanResult
import com.vuforia.CloudRecoSearchResult
import com.vuforia.TargetSearchResult

fun CloudRecoSearchResult.toIrScanResult(): IrScanResult {

    return IrScanResult(
        id = this.uniqueTargetId ?: "",
        name = this.targetName ?: "",
        metaData = this.metaData ?: ""
    )
}
