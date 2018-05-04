package com.gigigo.permissions.exception

data class Error(val code: Int, val message: String) {

  fun isValid(): Boolean = code != INVALID_ERROR
//todo check JvMStatic wotks without companion in the caller
  companion object {

    @JvmStatic
    val INVALID_ERROR = -1
    @JvmStatic
    val PERMISSION_ERROR = 0x9990
    @JvmStatic
    val PERMISSION_RATIONALE_ERROR = 0x9991
    @JvmStatic
    val FATAL_ERROR = 0x9991
  }
}