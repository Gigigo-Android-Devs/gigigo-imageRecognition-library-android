package com.gigigo.irfragment.reco

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import com.gigigo.irfragment.BuildConfig
import com.gigigo.irfragment.R
import com.gigigo.irfragment.R.layout
import com.gigigo.irfragment.core.IRApplicationControl
import com.gigigo.irfragment.core.IRApplicationException
import com.gigigo.irfragment.core.IRApplicationGLView
import com.gigigo.irfragment.core.IRApplicationSession
import com.gigigo.irfragment.utils.MarkFakeFeaturePoint
import com.gigigo.irfragment.utils.toIrScanResult
import com.vuforia.CameraDevice
import com.vuforia.FUSION_PROVIDER_TYPE
import com.vuforia.ObjectTracker
import com.vuforia.PositionalDeviceTracker
import com.vuforia.State
import com.vuforia.TargetFinder
import com.vuforia.Tracker
import com.vuforia.TrackerManager
import com.vuforia.Vuforia
import kotlinx.android.synthetic.main.fragment_image_recognition.irAnimationContent
import kotlinx.android.synthetic.main.fragment_image_recognition.irContentCamera
import kotlinx.android.synthetic.main.fragment_image_recognition.irLoadingIndicator

private const val ARG_LICENSE_KEY = "ARG_LICENSE_KEY"
private const val ARG_ACCESS_KEY = "ARG_ACCESS_KEY"
private const val ANIMATION_DURATION = 4000.toLong()

private const val ARG_SECRET_KEY = "ARG_SECRET_KEY"

class ImageRecognitionFragment : Fragment(), IRApplicationControl {
  private val LOGTAG = "ImageRecognitionFra"

  private val mHandler = Handler(Looper.getMainLooper())
  private var vuforiaAppSession: IRApplicationSession? = null

  // Cloud Recognition specific error codes
  // These codes match the ones defined for the TargetFinder in Vuforia.jar
  private val UPDATE_ERROR_AUTHORIZATION_FAILED = -1
  private val UPDATE_ERROR_PROJECT_SUSPENDED = -2
  private val UPDATE_ERROR_NO_NETWORK_CONNECTION = -3
  private val UPDATE_ERROR_SERVICE_NOT_AVAILABLE = -4
  private val UPDATE_ERROR_BAD_FRAME_QUALITY = -5
  private val UPDATE_ERROR_UPDATE_SDK = -6
  private val UPDATE_ERROR_TIMESTAMP_OUT_OF_RANGE = -7
  private val UPDATE_ERROR_REQUEST_TIMEOUT = -8

  private var mGlView: IRApplicationGLView? = null

  private var mRenderer: CloudRecoRenderer? = null

  private var mFinderStarted = false
  private var mResetTargetFinderTrackables = false

  // Error message handling
  private var mlastErrorCode = 0
  private var mInitErrorCode = 0
  private var mFinishActivityOnError: Boolean = false

  // Alert Dialog used to display SDK errors
  private var mErrorDialog: AlertDialog? = null

  private var mGestureDetector: GestureDetector? = null

  private val loadingDialogHandler = LoadingDialogHandler(activity)

  // Scan line and animation
  private var scanAnimation: TranslateAnimation? = null

  private var mLastErrorTime: Double = 0.toDouble()

  private var mIsDroidDevice = false

  // The TargetFinder is used to dynamically search
  // for targets using an internet connection
  private var mTargetFinder: TargetFinder? = null

  private lateinit var markFakeFeaturePoint: MarkFakeFeaturePoint

  private var licenseKey: String? = null
  private var accessKey: String? = null
  private var secretKey: String? = null
  private var listener: IRScannerListener? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.let {
      licenseKey = it.getString(ARG_LICENSE_KEY)
      accessKey = it.getString(ARG_ACCESS_KEY)
      secretKey = it.getString(ARG_SECRET_KEY)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {

    return inflater.inflate(layout.fragment_image_recognition, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    vuforiaAppSession = IRApplicationSession(this)

    startLoadingAnimation()

    vuforiaAppSession?.initAR(activity, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, licenseKey)

    // Creates the GestureDetector listener for processing double tap
    mGestureDetector = GestureDetector(context, GestureListener())

    mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith("droid")

    markFakeFeaturePoint = MarkFakeFeaturePoint(context)
  }

  private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
    // Used to set autofocus one second after a manual focus is triggered
    private val autofocusHandler = Handler()

    override fun onDown(e: MotionEvent): Boolean {
      return true
    }

    // Process Single Tap event to trigger autofocus
    override fun onSingleTapUp(e: MotionEvent): Boolean {
      val result =
        CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO)
      if (!result) Log.e("SingleTapUp", "Unable to trigger focus")

      // Generates a Handler to trigger continuous auto-focus
      // after 1 second
      autofocusHandler.postDelayed({
        val autofocusResult = CameraDevice.getInstance()
          .setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)

        if (!autofocusResult) Log.e("SingleTapUp", "Unable to re-enable continuous auto-focus")
      }, 1000L)

      return true
    }
  }

  override fun onResume() {
    super.onResume()
    showProgressIndicator(true)

    // This is needed for some Droid devices to force portrait
    if (mIsDroidDevice) {
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    vuforiaAppSession?.onResume()
  }

  override fun onConfigurationChanged(newConfig: Configuration?) {
    super.onConfigurationChanged(newConfig)
    vuforiaAppSession?.onConfigurationChanged()
  }

  override fun onPause() {
    super.onPause()
    vuforiaAppSession?.onPause()

    if (mGlView != null) {
      mGlView?.visibility = View.INVISIBLE
      mGlView?.onPause()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    deinitCloudReco()

    try {
      vuforiaAppSession?.stopAR()
    } catch (e: IRApplicationException) {
      Log.e(LOGTAG, e.string)
    }

    System.gc()
  }

  private fun deinitCloudReco() {
    if (mTargetFinder == null) {
      Log.e(LOGTAG, "Could not deinit cloud reco because it was not initialized")
      return
    }

    mTargetFinder?.deinit()
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun startLoadingAnimation() {
    // Inflates the Overlay Layout to be displayed above the Camera View

    irContentCamera.setOnTouchListener { _, motionEvent ->
      mGestureDetector?.onTouchEvent(motionEvent)
      true
    }

    irContentCamera.visibility = View.VISIBLE
    irContentCamera.setBackgroundColor(Color.BLACK)

    loadingDialogHandler.mLoadingDialogContainer = irLoadingIndicator
    loadingDialogHandler.mLoadingDialogContainer.visibility = View.VISIBLE

    irAnimationContent.visibility = View.GONE
    scanAnimation = TranslateAnimation(
      TranslateAnimation.ABSOLUTE, 0f, TranslateAnimation.ABSOLUTE, 0f,
      TranslateAnimation.RELATIVE_TO_PARENT, -0.1f, TranslateAnimation.RELATIVE_TO_PARENT, 0.9f
    )
    scanAnimation?.duration = ANIMATION_DURATION
    scanAnimation?.repeatCount = Animation.INFINITE
    scanAnimation?.repeatMode = Animation.REVERSE
    scanAnimation?.interpolator = LinearInterpolator()
  }

  private fun initApplicationAR() {
    // Create OpenGL ES view:
    val depthSize = 16
    val stencilSize = 0
    val translucent = Vuforia.requiresAlpha()

    // Initialize the GLView with proper flags
    mGlView = IRApplicationGLView(context)
    mGlView?.init(translucent, depthSize, stencilSize)

    // Sets up the Renderer of the GLView
    mRenderer = CloudRecoRenderer(vuforiaAppSession, activity, this)
    mGlView?.setRenderer(mRenderer)
  }

  // Returns the error message for each error code
  private fun getStatusDescString(code: Int): String {
    if (code == UPDATE_ERROR_AUTHORIZATION_FAILED) {
      return getString(R.string.UPDATE_ERROR_AUTHORIZATION_FAILED_DESC)
    }
    if (code == UPDATE_ERROR_PROJECT_SUSPENDED) {
      return getString(R.string.UPDATE_ERROR_PROJECT_SUSPENDED_DESC)
    }
    if (code == UPDATE_ERROR_NO_NETWORK_CONNECTION) {
      return getString(R.string.UPDATE_ERROR_NO_NETWORK_CONNECTION_DESC)
    }
    if (code == UPDATE_ERROR_SERVICE_NOT_AVAILABLE) {
      return getString(R.string.UPDATE_ERROR_SERVICE_NOT_AVAILABLE_DESC)
    }
    if (code == UPDATE_ERROR_UPDATE_SDK) return getString(R.string.UPDATE_ERROR_UPDATE_SDK_DESC)
    if (code == UPDATE_ERROR_TIMESTAMP_OUT_OF_RANGE) {
      return getString(R.string.UPDATE_ERROR_TIMESTAMP_OUT_OF_RANGE_DESC)
    }
    if (code == UPDATE_ERROR_REQUEST_TIMEOUT) {
      return getString(R.string.UPDATE_ERROR_REQUEST_TIMEOUT_DESC)
    }
    return if (code == UPDATE_ERROR_BAD_FRAME_QUALITY) {
      getString(R.string.UPDATE_ERROR_BAD_FRAME_QUALITY_DESC)
    } else {
      getString(R.string.UPDATE_ERROR_UNKNOWN_DESC)
    }
  }

  // Returns the error message header for each error code
  private fun getStatusTitleString(code: Int): String {
    if (code == UPDATE_ERROR_AUTHORIZATION_FAILED) {
      return getString(R.string.UPDATE_ERROR_AUTHORIZATION_FAILED_TITLE)
    }
    if (code == UPDATE_ERROR_PROJECT_SUSPENDED) {
      return getString(R.string.UPDATE_ERROR_PROJECT_SUSPENDED_TITLE)
    }
    if (code == UPDATE_ERROR_NO_NETWORK_CONNECTION) {
      return getString(R.string.UPDATE_ERROR_NO_NETWORK_CONNECTION_TITLE)
    }
    if (code == UPDATE_ERROR_SERVICE_NOT_AVAILABLE) {
      return getString(R.string.UPDATE_ERROR_SERVICE_NOT_AVAILABLE_TITLE)
    }
    if (code == UPDATE_ERROR_UPDATE_SDK) return getString(R.string.UPDATE_ERROR_UPDATE_SDK_TITLE)
    if (code == UPDATE_ERROR_TIMESTAMP_OUT_OF_RANGE) {
      return getString(R.string.UPDATE_ERROR_TIMESTAMP_OUT_OF_RANGE_TITLE)
    }
    if (code == UPDATE_ERROR_REQUEST_TIMEOUT) {
      return getString(R.string.UPDATE_ERROR_REQUEST_TIMEOUT_TITLE)
    }
    return if (code == UPDATE_ERROR_BAD_FRAME_QUALITY) {
      getString(R.string.UPDATE_ERROR_BAD_FRAME_QUALITY_TITLE)
    } else {
      getString(R.string.UPDATE_ERROR_UNKNOWN_TITLE)
    }
  }

  private fun showErrorMessage(errorCode: Int, errorTime: Double, finishActivityOnError: Boolean) {
    if (errorTime < (mLastErrorTime + 5.0) || errorCode == mlastErrorCode) return

    if (!BuildConfig.DEBUG) {
      if (errorCode == UPDATE_ERROR_BAD_FRAME_QUALITY) return
    }

    UPDATE_ERROR_BAD_FRAME_QUALITY

    mLastErrorTime = errorTime
    mlastErrorCode = errorCode
    mFinishActivityOnError = finishActivityOnError

    activity?.runOnUiThread {
      mErrorDialog?.dismiss()

      // Generates an Alert Dialog to show the error message
      val builder = AlertDialog.Builder(context)
      builder.setMessage(getStatusDescString(this@ImageRecognitionFragment.mlastErrorCode))
        .setTitle(getStatusTitleString(this@ImageRecognitionFragment.mlastErrorCode))
        .setCancelable(false)
        .setIcon(0)
        .setPositiveButton(
          getString(R.string.button_OK)
        ) { dialog, id ->
          if (mFinishActivityOnError) {
            activity?.finish()
          } else {
            dialog.dismiss()
          }
        }

      mErrorDialog = builder.create()
      mErrorDialog?.show()
    }
  }

  private fun showInitializationErrorMessage(message: String) {
    activity?.runOnUiThread {
      mErrorDialog?.dismiss()

      // Generates an Alert Dialog to show the error message
      val builder = AlertDialog.Builder(context)
      builder.setMessage(message)
        .setTitle(getString(R.string.INIT_ERROR))
        .setCancelable(false)
        .setIcon(0)
        .setPositiveButton(
          getString(R.string.button_OK)
        ) { _, _ -> activity?.finish() }

      mErrorDialog = builder.create()
      mErrorDialog?.show()
    }
  }

  protected fun startFinderIfStopped() {
    if (!mFinderStarted) {
      if (mTargetFinder == null) {
        Log.e(LOGTAG, "Tried to start TargetFinder but was not initialized")
        return
      }

      mTargetFinder?.clearTrackables()
      mTargetFinder?.startRecognition()
      scanLineStart()

      mFinderStarted = true
    }
  }

  protected fun stopFinderIfStarted() {
    if (mFinderStarted) {
      if (mTargetFinder == null) {
        Log.e(LOGTAG, "Tried to stop TargetFinder but was not initialized")
        return
      }

      mTargetFinder?.stop()
      scanLineStop()

      mFinderStarted = false
    }
  }

  override fun doLoadTrackersData(): Boolean {
    Log.d(LOGTAG, "initCloudReco")

    // Get the object tracker:
    val trackerManager = TrackerManager.getInstance()
    val objectTracker = trackerManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker

    // Start the target finder using keys
    val targetFinder = objectTracker.targetFinder
    targetFinder.startInit(accessKey, secretKey)

    targetFinder.waitUntilInitFinished()

    val resultCode = targetFinder.initState
    if (resultCode != TargetFinder.INIT_SUCCESS) {
      if (resultCode == TargetFinder.INIT_ERROR_NO_NETWORK_CONNECTION) {
        mInitErrorCode = UPDATE_ERROR_NO_NETWORK_CONNECTION
      } else {
        mInitErrorCode = UPDATE_ERROR_SERVICE_NOT_AVAILABLE
      }

      Log.e(LOGTAG, "Failed to initialize target finder.")
      return false
    }

    mTargetFinder = targetFinder

    return true
  }

  override fun doUnloadTrackersData(): Boolean {
    return true
  }

  override fun onVuforiaResumed() {
    if (mGlView != null) {
      mGlView?.visibility = View.VISIBLE
      mGlView?.onResume()
    }
  }

  // Called once Vuforia has been initialized or
  // an error has caused Vuforia initialization to stop
  override fun onInitARDone(exception: IRApplicationException?) {

    if (exception == null) {
      initApplicationAR()

      mRenderer?.setActive(true)

      // Now add the GL surface view. It is important
      // that the OpenGL ES surface view gets added
      // BEFORE the camera is started and video
      // background is configured.
      irContentCamera.addView(
        mGlView,
        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
      )

      irContentCamera.bringToFront()

      // Hides the Loading Dialog
      loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG)

      irContentCamera.setBackgroundColor(Color.TRANSPARENT)

      vuforiaAppSession?.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT)
    } else
    // Show error message if an exception was thrown during the init process
    {
      Log.e(LOGTAG, exception.string)
      if (mInitErrorCode != 0) {
        showErrorMessage(mInitErrorCode, 10.0, true)
      } else {
        showInitializationErrorMessage(exception.string)
      }
    }
  }

  override fun onVuforiaStarted() {
    mRenderer?.updateRenderingPrimitives()

    // Set camera focus mode
    if (!CameraDevice.getInstance()
        .setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)
    ) {
      // If continuous autofocus mode fails, attempt to set to a different mode
      if (!CameraDevice.getInstance()
          .setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO)
      ) {
        CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL)
      }
    }

    showProgressIndicator(false)
  }

  private fun showProgressIndicator(show: Boolean) {
    if (show) {
      loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG)
    } else {
      loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG)
    }
  }

  // Called every frame
  override fun onVuforiaUpdate(state: State) {

    val finder = mTargetFinder
    if (finder == null) {
      Log.e(LOGTAG, "Tried to query TargetFinder but was not initialized")
      return
    }

    // Check if there are new results available:
    val queryResult = finder.updateQueryResults()
    val queryStatus = queryResult.status

    // Show a message if we encountered an error:
    if (queryStatus < 0) {
      val closeAppAfterError =
        queryStatus == UPDATE_ERROR_NO_NETWORK_CONNECTION || queryStatus == UPDATE_ERROR_SERVICE_NOT_AVAILABLE

      showErrorMessage(queryStatus, state.frame.timeStamp, closeAppAfterError)
    } else if (queryStatus == TargetFinder.UPDATE_RESULTS_AVAILABLE) {
      val queryResultsList = queryResult.results

      // Process new search results
      if (!queryResultsList.empty()) {
        val result = queryResultsList.at(0)

        // Check if this target is suitable for tracking:
        listener?.onScanResult(result.toIrScanResult())

        if (result.trackingRating > 0) {
          finder.enableTracking(result)
        }
      }
    }

    if (mResetTargetFinderTrackables) {
      finder.clearTrackables()
      mResetTargetFinderTrackables = false
    }
  }

  override fun doInitTrackers(): Boolean {
    // Indicate if the trackers were initialized correctly
    var result = true

    // For ImageRecognitionFragment, the recommended fusion provider mode is
    // the one recommended by the FUSION_OPTIMIZE_IMAGE_TARGETS_AND_VUMARKS enum
    if (!vuforiaAppSession?.setFusionProviderType(
        FUSION_PROVIDER_TYPE.FUSION_OPTIMIZE_IMAGE_TARGETS_AND_VUMARKS
      )!!
    ) {
      return false
    }

    val tManager = TrackerManager.getInstance()
    val tracker: Tracker?

    tracker = tManager.initTracker(ObjectTracker.getClassType())
    if (tracker == null) {
      Log.e(
        LOGTAG,
        "Tracker not initialized. Tracker already initialized or the camera is already started"
      )
      result = false
    } else {
      Log.i(LOGTAG, "Tracker successfully initialized")
    }

    // Initialize the Positional Device Tracker
    val deviceTracker =
      tManager.initTracker(PositionalDeviceTracker.getClassType()) as PositionalDeviceTracker

    if (deviceTracker != null) {
      Log.i(LOGTAG, "Successfully initialized Device Tracker")
    } else {
      Log.e(LOGTAG, "Failed to initialize Device Tracker")
    }

    return result
  }

  override fun doStartTrackers(): Boolean {
    // Indicate if the trackers were started correctly
    var result = true

    if (mTargetFinder == null) {
      Log.e(LOGTAG, "Tried to start TargetFinder but was not initialized")
      return false
    }

    mTargetFinder?.startRecognition()
    scanLineStart()
    mFinderStarted = true

    // Start the Object Tracker
    // The Object Tracker tracks the target recognized by the target finder
    val objectTracker = TrackerManager.getInstance().getTracker(ObjectTracker.getClassType())

    if (objectTracker != null) {
      result = objectTracker.start()
    }

    // Start device tracker if enabled
    if (isDeviceTrackerActive()) {
      val tManager = TrackerManager.getInstance()
      val deviceTracker =
        tManager.getTracker(PositionalDeviceTracker.getClassType()) as PositionalDeviceTracker

      if (deviceTracker != null && deviceTracker.start()) {
        Log.i(LOGTAG, "Successfully started Device Tracker")
      } else {
        Log.e(LOGTAG, "Failed to start Device Tracker")
      }
    }

    return result
  }

  override fun doStopTrackers(): Boolean {
    // Indicate if the trackers were stopped correctly
    var result = true

    val trackerManager = TrackerManager.getInstance()
    val objectTracker = trackerManager.getTracker(ObjectTracker.getClassType()) as ObjectTracker

    if (objectTracker != null) {
      objectTracker.stop()

      if (mTargetFinder == null) {
        Log.e(LOGTAG, "Tried to stop TargetFinder but was not initialized")
        return false
      }

      val targetFinder = mTargetFinder
      targetFinder?.stop()
      scanLineStop()
      mFinderStarted = false

      // Clears the trackables
      targetFinder?.clearTrackables()
    } else {
      result = false
    }

    // Stop device tracker
    if (isDeviceTrackerActive()) {
      val deviceTracker = trackerManager.getTracker(PositionalDeviceTracker.getClassType())

      if (deviceTracker != null) {
        deviceTracker.stop()
        Log.i(LOGTAG, "Successfully stopped device tracker")
      } else {
        Log.e(LOGTAG, "Could not stop device tracker")
      }
    }

    return result
  }

  override fun doDeinitTrackers(): Boolean {
    val tManager = TrackerManager.getInstance()

    // Indicate if the trackers were deinitialized correctly
    val result = tManager.deinitTracker(ObjectTracker.getClassType())
    tManager.deinitTracker(PositionalDeviceTracker.getClassType())

    return result
  }


  private fun isDeviceTrackerActive(): Boolean {
    return false
  }

  private fun scanLineStart() {
    mHandler.post {

      irAnimationContent?.apply {
        removeView(markFakeFeaturePoint)
        addView(markFakeFeaturePoint)

        bringToFront()
        visibility = View.VISIBLE
        animation = scanAnimation
      }
    }
  }

  private fun scanLineStop() {
    mHandler.post {
      irAnimationContent?.apply {
        visibility = View.GONE
        clearAnimation()
      }
    }
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    if (context is IRScannerListener) {
      listener = context
    } else {
      throw RuntimeException(context.toString() + " must implement IRScannerListener")
    }
  }

  override fun onDetach() {
    super.onDetach()
    listener = null
  }

  fun resetScanner() {
    mResetTargetFinderTrackables = true
  }

  companion object {

    @JvmStatic
    fun newInstance(licenseKey: String, accessKey: String, secretKey: String) =
      ImageRecognitionFragment().apply {
        arguments = Bundle().apply {
          putString(ARG_LICENSE_KEY, licenseKey)
          putString(ARG_ACCESS_KEY, accessKey)
          putString(ARG_SECRET_KEY, secretKey)
        }
      }
  }
}
