package com.gigigo.imagerecognition.vuforia;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.view.View;
import java.util.Random;

public class MarkFakeFeaturePoint extends View implements Runnable {

  static final int MAX_NUMBER_POINTS = 15;
  static Paint p = new Paint();
  final Bitmap fakePoint;
  boolean bDisableIfThrowException = false;
  Random randX, randY;
  Handler handler = new Handler() {
    public void handleMessage(android.os.Message msg) {
      invalidate();
    }

    ;
  };
  ObjectAnimator mAnimation;
  int maxHeight;
  int lineYPos;

  public MarkFakeFeaturePoint(Context context) {
    super(context);
    fakePoint = BitmapFactory.decodeResource(getResources(), R.drawable.ir_mark_point);

    randX = new Random();
    randY = new Random();

    new Thread(this).start();
  }

  public void setObjectAnimator(ObjectAnimator animation) {
    if (animation != null) {
      mAnimation = animation;
    }
  }

  public void setMaxHeight(int max) {
    if (max != 0) {
      maxHeight = max;
    }
  }

  public void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    Random rand = new Random();

    int n = rand.nextInt(MAX_NUMBER_POINTS);
    //paint until MAX_NUMBER_POINTS
    for (int i = 0; i < n; i++) {
      paintFakeFeaturePoint(canvas);
    }

    if (mAnimation != null) {
      System.out.println("mAnimation.getAnimatedValue() " + mAnimation.getAnimatedValue());
    }
    if (maxHeight != 0) {
      if (lineYPos <= maxHeight) {
        lineYPos = lineYPos + (maxHeight / 12);
      }
      if (lineYPos > maxHeight) lineYPos = 0;
    }
  }

  private void paintFakeFeaturePoint(Canvas canvas) {
    int xMax = 0;
    int yMax = 0;

    int x, y = 0;
    try {
      int screenHeight = getResources().getDisplayMetrics().heightPixels;
      int screenWidth = getResources().getDisplayMetrics().widthPixels;
      xMax = screenHeight;
      yMax = screenWidth;
    } catch (Throwable tr) {
      bDisableIfThrowException = true;
    }
    //this calculate the ir_scanline distance
    Random rand = new Random();

    x = randX.nextInt(xMax);
    y = randY.nextInt(yMax);
    //get ir_scanline currentposition
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
      Random randy = new Random(); //earl!

      float valueYScanline = 0f;
      if (mAnimation != null
          && android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
        valueYScanline = ((Float) (mAnimation.getAnimatedValue()));
      } else {
        valueYScanline = lineYPos;
      }
      int valueYint = Math.round(valueYScanline);

      int max = valueYint + 100;
      int min = valueYint - 100;

      if (max > xMax) max = xMax;
      if (min < 0) min = 0;

      y = randy.nextInt((max - min) + 1) + min;

      if (y > xMax) y = xMax - 50;
      if (y < 0) y = 50;
    }
    canvas.drawBitmap(fakePoint, x, y, p);
  }

  public void run() {
    while (true) {
      try {
        if (bDisableIfThrowException) break;
        Thread.sleep(300);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      handler.sendEmptyMessage(0);
    }
  }
}
