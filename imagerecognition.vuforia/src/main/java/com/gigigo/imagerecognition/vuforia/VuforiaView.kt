package com.gigigo.imagerecognition.vuforia

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.gigigo.imagerecognition.vuforia.credentials.VuforiaCredentials
import com.gigigo.vuforiacore.sdkimagerecognition.icloudrecognition.CloudRecognitionActivityLifeCycleCallBack
import com.gigigo.vuforiacore.sdkimagerecognition.icloudrecognition.ICloudRecognitionCommunicator
import com.vuforia.TargetSearchResult
import com.vuforia.Trackable
import kotlinx.coroutines.experimental.runBlocking

class VuforiaView : FrameLayout, ICloudRecognitionCommunicator {
  private lateinit var resultHandler: ResultHandler

  private lateinit var currentActivity: AppCompatActivity

  private var viewContext: Context
  private var vuforiaContentView: ViewGroup
  private var scanLine: View? = null
  private lateinit var markFakeFeaturePoint: MarkFakeFeaturePoint
  private val scanAnimation: TranslateAnimation? = null
  private val ANIMATION_DURATION = 3000

  private lateinit var cloudRecognitionCallBack: CloudRecognitionActivityLifeCycleCallBack


  constructor(context: Context, contentView: View, contextProvider: ContextProvider,
      vuforiaCredentials: VuforiaCredentials) : super(context) {
    viewContext = context
    vuforiaContentView = contentView as ViewGroup
    setupVuforiaCredentials(vuforiaCredentials, contextProvider)
  }

  private fun setupVuforiaCredentials(vuforiaCredentials: VuforiaCredentials,
      contextProvider: ContextProvider) {
    currentActivity = contextProvider.getCurrentActivity()
    cloudRecognitionCallBack = CloudRecognitionActivityLifeCycleCallBack(currentActivity, this,
        vuforiaCredentials.clientAccessKey, vuforiaCredentials.clientSecretKey,
        vuforiaCredentials.licenseKey, false)
  }

  override fun setContentViewTop(vuforiaView: View?) {
    val inflater = viewContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val view = inflater.inflate(R.layout.view_vuforia, null)
    scanLine = view.findViewById(R.id.scan_line)
    val relativeLayout = view.findViewById(R.id.layoutContentVuforiaGL) as RelativeLayout
    relativeLayout.addView(vuforiaView, 0)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
      markFakeFeaturePoint = MarkFakeFeaturePoint(viewContext)
    }
    relativeLayout.addView(markFakeFeaturePoint)
    val vlp = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT)

    vuforiaContentView.addView(view, vlp)

    startBoringAnimation()
    scanlineStart()
  }

  override fun onVuforiaResult(trackable: Trackable?, uniqueID: TargetSearchResult?) {
    scanlineStop()
    resultHandler.handleResult(uniqueID)
  }

  private fun startBoringAnimation() {
    scanLine?.let {
      it.visibility = View.VISIBLE
      // Create animators for y axe
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
        var yMax: Float
        yMax = resources.displayMetrics.heightPixels.toFloat()
        yMax = (yMax * 0.9f)

        val objectAnimator = ObjectAnimator.ofFloat(it, "translationY", 0f, yMax).apply {
          setRepeatCount(Animation.INFINITE)
          setDuration(ANIMATION_DURATION.toLong())
          setRepeatMode(ValueAnimator.REVERSE)
          setInterpolator(LinearInterpolator())
          start()
        }

        //for draw points near ir_scanline
        markFakeFeaturePoint.setObjectAnimator(objectAnimator)
      }
    }
  }

  private fun scanlineStart() {
    runBlocking {
      scanLine?.let {
        visibility = View.VISIBLE
        animation = scanAnimation
      }
    }
  }

  private fun scanlineStop() {
    runBlocking() {
      scanLine?.let {
        visibility = View.GONE
        clearAnimation()
      }
    }
  }

  fun stopCamera() {
    scanlineStop()
  }


  fun setResultHandler(handler: ResultHandler) {
    this.resultHandler = handler
  }

  interface ResultHandler {
    fun handleResult(result: TargetSearchResult?)
  }
}
