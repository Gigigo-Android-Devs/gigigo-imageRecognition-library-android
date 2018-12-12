package com.gigigo.imagerecognition.vuforia.credentials

import android.os.Parcel
import android.os.Parcelable

data class VuforiaCredentials(val licenseKey: String, val clientAccessKey: String,
    val clientSecretKey: String) : Parcelable {
  constructor(parcel: Parcel) : this(
      parcel.readString(),
      parcel.readString(),
      parcel.readString())

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeString(licenseKey)
    parcel.writeString(clientAccessKey)
    parcel.writeString(clientSecretKey)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<VuforiaCredentials> {
    override fun createFromParcel(parcel: Parcel): VuforiaCredentials {
      return VuforiaCredentials(parcel)
    }

    override fun newArray(size: Int): Array<VuforiaCredentials?> {
      return arrayOfNulls(size)
    }
  }
}
