package com.gigigo.vuforiacore.sdkimagerecognition.icloudrecognition;

import android.app.Activity;
import com.vuforia.TrackableResult;

/**
 * Created by nubor on 14/03/2018.
 */

public interface ICloudRecognitionAR {

  void initRender(Activity activity);

  void onRenderAR(TrackableResult trackableResult, float[] projectionMatrix);

  //asv ojo el angle lo añado por una probatina, este codigo en los q la implementes es identico siempre,
  //casi tiraba por abstrct calss q interface, x culpa de esto

  void setAngleRotation(float angleRotation);

  float getAngleRotation();
/*
  public volatile float mAngle = -1;

  @Override public void setAngleRotation(float angleRotation) {
    mAngle = angleRotation;
  }

  @Override public float getAngleRotation() {
    return mAngle;
  }
  */
  //loadtextures()? , quizás sea necesario aunk se puede hacer en el initrender(lo malo q así no las puedes cambiar en caliente)
}
