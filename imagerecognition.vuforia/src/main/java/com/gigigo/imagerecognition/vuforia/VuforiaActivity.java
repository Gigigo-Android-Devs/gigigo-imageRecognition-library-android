package com.gigigo.imagerecognition.vuforia;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import com.gigigo.imagerecognition.Constants;
import com.gigigo.imagerecognition.vuforia.credentials.VuforiaCredentials;
import com.gigigo.vuforiacore.sdkimagerecognition.icloudrecognition.CloudRecognitionActivityLifeCycleCallBack;
import com.gigigo.vuforiacore.sdkimagerecognition.icloudrecognition.ICloudRecognitionCommunicator;
import com.vuforia.TargetSearchResult;
import com.vuforia.Trackable;

public class VuforiaActivity extends FragmentActivity implements ICloudRecognitionCommunicator {

  private static final String RECOGNIZED_IMAGE_INTENT =
      "com.gigigo.imagerecognition.intent.action.RECOGNIZED_IMAGE";
  private static final int ANIM_DURATION = 3000;
  //basics for any vuforia activity
  //private View mView;
  private static CloudRecognitionActivityLifeCycleCallBack mCloudRecoCallBack;
  int mCodeResult = -1;
  View mVuforiaView;
  //region New not GPU animation
  MarkFakeFeaturePoint markFakeFeaturePoint;
  private View scanLine;
  private TranslateAnimation scanAnimation;

  @Override protected void onCreate(Bundle state) {
    super.onCreate(state);
    Log.i("VuforiaActivity", "VuforiaActivity.onCreate");
    initVuforiaKeys(getIntent());
    initGetCodeForResult(getIntent());
  }

  private void initGetCodeForResult(Intent intent) {
    int codeResultAux =
        intent.getIntExtra(Constants.IMAGE_RECOGNITION_CODE_RESULT, -1);
    if (codeResultAux != -1) {
      this.mCodeResult = codeResultAux;
    }
  }

  //region implements CloudRecoCommunicator ands initializations calls
  private void initVuforiaKeys(Intent intent) {
    Bundle b = intent.getBundleExtra(
        Constants.IMAGE_RECOGNITION_CREDENTIALS);
    VuforiaCredentials vuforiaCredentials =
        b.getParcelable(Constants.IMAGE_RECOGNITION_CREDENTIALS);

    mCloudRecoCallBack = new CloudRecognitionActivityLifeCycleCallBack(this, this,
        vuforiaCredentials.getClientAccessKey(), vuforiaCredentials.getClientSecretKey(),
        vuforiaCredentials.getLicenseKey(), false);
  }

  @Override public void setContentViewTop(View vuforiaView) {

    LayoutInflater inflater =
        (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View view = inflater.inflate(R.layout.activity_vuforia, null);
    scanLine = view.findViewById(R.id.scan_line);
    RelativeLayout relativeLayout = (RelativeLayout) view.findViewById(R.id.layoutContentVuforiaGL);
    relativeLayout.addView(vuforiaView, 0);
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)

    {
      markFakeFeaturePoint = new MarkFakeFeaturePoint(this);
    }
    relativeLayout.addView(markFakeFeaturePoint);
    ViewGroup.LayoutParams vlp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT);

    addContentView(view, vlp);

    //region Button Close
    view.findViewById(R.id.btnCloseVuforia).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {

        Intent i = new Intent();
        i.putExtra(Constants.PATTERN_ID, "");
        setResult(Activity.RESULT_CANCELED, i);
        finish();
      }
    });
    //endregion
    mVuforiaView = vuforiaView;

    startBoringAnimation();
    scanlineStart();
  }

  private void startBoringAnimation() {
    scanLine.setVisibility(View.VISIBLE);
    // Create animators for y axe
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
      int yMax = 0;
      yMax =
          getResources().getDisplayMetrics().heightPixels; //mVuforiaView.getDisplay().getHeight();
      yMax = (int) (yMax * 0.9);// 174;

      ObjectAnimator oay = ObjectAnimator.ofFloat(scanLine, "translationY", 0, yMax);
      oay.setRepeatCount(Animation.INFINITE);
      oay.setDuration(ANIM_DURATION);
      oay.setRepeatMode(ValueAnimator.REVERSE);

      oay.setInterpolator(new LinearInterpolator());
      oay.start();

      //for draw points near ir_scanline
      markFakeFeaturePoint.setObjectAnimator(oay);
    }

    //scanAnimation.

  }

  private void scanlineStart() {
    this.runOnUiThread(new Runnable() {
      @Override public void run() {
        scanLine.setVisibility(View.VISIBLE);
        scanLine.setAnimation(scanAnimation);
      }
    });
  }

  private void scanlineStop() {
    this.runOnUiThread(new Runnable() {
      @Override public void run() {
        scanLine.setVisibility(View.GONE);
        scanLine.clearAnimation();
      }
    });
  }
  //endregion

  @Override public void onVuforiaResult(Trackable trackable, TargetSearchResult result) {
    scanlineStop();
    sendRecognizedPatternToClient(result);
  }

  private void sendRecognizedPatternToClient(TargetSearchResult result) {
    Intent i = setDataIntent(result);
    //or start4result, and setresult, or callback by the broadcast
    if (mCodeResult != -1) {
      setResult(Activity.RESULT_OK, i);
      finish();
    } else {
      //we add package appid,
      String appId = getApplicationContext().getPackageName();
      i.putExtra(appId, appId);
      ImageRecognitionVuforia.Companion.sendRecognizedPattern(i);
      finish();
    }
  }

  private Intent setDataIntent(TargetSearchResult result) {
    Intent i = new Intent();
    if (result.getUniqueTargetId() != null) {
      i.putExtra(Constants.PATTERN_ID, result.getUniqueTargetId());
    }
    if (result.getTargetName() != null) {
      i.putExtra(Constants.PATTERN_NAME, result.getTargetName());
    }
    if (result.getMetaData() != null) {
      i.putExtra(Constants.PATTERN_METADATA, result.getMetaData());
    }
    i.putExtra(Constants.PATTERN_SIZE, result.getTargetSize());
    i.putExtra(Constants.PATTERN_TRACK_RATING, result.getTrackingRating());
    i.setAction(RECOGNIZED_IMAGE_INTENT);
    return i;
  }
}
