package com.gigigo.vuforiacore.sdkimagerecognition.icloudrecognition;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import com.gigigo.vuforiacore.sdkimagerecognition.vuforiaenvironment.VuforiaSession;
import com.gigigo.vuforiacore.sdkimagerecognition.vuforiaenvironment.utils.SampleAppRenderer;
import com.gigigo.vuforiacore.sdkimagerecognition.vuforiaenvironment.utils.SampleAppRendererControl;
import com.vuforia.Device;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.TrackableResult;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//asv todo hay q limpiar todo lo q tiene q ver con las texturas y tal, aparte el tema de las textura está tan raro x el tema del contexto
//necesariopara acceder a los resources, completamente trivial ya q se puede hacer antes y darle el textures

//revisar esta renderer https://stackoverflow.com/questions/41460581/android-vuforia-multi-target-rotate-object-on-touch
//y crear nuestro multirenderer para cambiar el mismo en caliente, en plan empezarconun cloudReco q una vez encuentre un
//resultado lo fije mediante el ground plane ese de fijar en un punto de la realidad, o pasar de cloudreco a un localrecog

public class CloudRecognitionRenderer implements GLSurfaceView.Renderer, SampleAppRendererControl {

  private VuforiaSession vuforiaAppSession;
  private SampleAppRenderer mSampleAppRenderer;
  private CloudRecognition mCloudReco;
  private boolean mIsActive = false;

  public CloudRecognitionRenderer(VuforiaSession session, CloudRecognition cloudRecog) {
    vuforiaAppSession = session;
    mCloudReco = cloudRecog;

    // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
    // the device mode AR/VR and stereo mode
    mSampleAppRenderer =
        new SampleAppRenderer(this, mCloudReco.mActivity, Device.MODE.MODE_AR, false, 0.010f, 5f);
  }

  // Called when the surface is created or recreated.
  @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    // Call Vuforia function to (re)initialize rendering after first use
    // or after OpenGL ES context was lost (e.g. after onPause/onResume):
    vuforiaAppSession.onSurfaceCreated();
    mSampleAppRenderer.onSurfaceCreated();
  }

  // Called when the surface changed size.
  @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
    // Call Vuforia function to handle render surface size changes:
    vuforiaAppSession.onSurfaceChanged(width, height);
    mSampleAppRenderer.onConfigurationChanged(mIsActive);
  }

  // Called to draw the current frame.
  @Override public void onDrawFrame(GL10 gl) {
    if (!mIsActive) //asv test this
    {
      return;
    }
    // Call our function to render content
    mSampleAppRenderer.render(); //asv rotation añado el parametro del angulo al renderer(es el cludrendere q a su vez se lo pasa al renderBase y este applica el angulo en el evento de onDrawGl
  }

  public void setActive(boolean active) {
    mIsActive = active;

    if (mIsActive) mSampleAppRenderer.configureVideoBackground();
  }

  public void renderFrame(State state, float[] projectionMatrix) {
    // Renders video background replacing Renderer.DrawVideoBackground()
    mSampleAppRenderer.renderVideoBackground();

    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    GLES20.glEnable(GLES20.GL_CULL_FACE);

    // Did we find any trackables this frame?
    if (state.getNumTrackableResults() > 0) {
      // Gets current trackable result
      TrackableResult trackableResult = state.getTrackableResult(0);
      if (trackableResult == null) {
        return;
      }
      mCloudReco.stopFinderIfStarted();
    } else {
      mCloudReco.startFinderIfStopped();
    }

    GLES20.glDisable(GLES20.GL_DEPTH_TEST);

    Renderer.getInstance().end();
  }
}
