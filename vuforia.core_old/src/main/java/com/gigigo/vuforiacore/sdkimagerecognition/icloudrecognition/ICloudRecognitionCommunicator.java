package com.gigigo.vuforiacore.sdkimagerecognition.icloudrecognition;

import android.view.View;
import com.vuforia.TargetSearchResult;
import com.vuforia.Trackable;

public interface ICloudRecognitionCommunicator {
  void setContentViewTop(View view);

  void onVuforiaResult(Trackable trackable, TargetSearchResult UniqueID);
}
