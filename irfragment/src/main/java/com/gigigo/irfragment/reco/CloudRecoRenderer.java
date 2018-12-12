package com.gigigo.irfragment.reco;

import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import com.gigigo.irfragment.core.IRAppRenderer;
import com.gigigo.irfragment.core.IRAppRendererControl;
import com.gigigo.irfragment.core.IRApplicationSession;
import com.gigigo.irfragment.utils.IRMath;
import com.gigigo.irfragment.utils.IRUtils;
import com.vuforia.Device;
import com.vuforia.ImageTargetResult;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.TrackableResult;
import com.vuforia.TrackableResultList;
import com.vuforia.Vuforia;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * The renderer class for the ImageRecognitionFragment sample.
 *
 * In the renderFrame() function you can render augmentations to display over the Target
 */
public class CloudRecoRenderer implements GLSurfaceView.Renderer, IRAppRendererControl {
  private final IRApplicationSession vuforiaAppSession;
  private final IRAppRenderer mIRAppRenderer;

  private final Activity mActivity;
  private final ImageRecognitionFragment finderListener;

  private boolean mIsActive = false;

  CloudRecoRenderer(IRApplicationSession session, Activity activity,
      ImageRecognitionFragment finderListener) {
    vuforiaAppSession = session;
    mActivity = activity;
    this.finderListener = finderListener;

    // IRAppRenderer used to encapsulate the use of RenderingPrimitives setting
    // the device mode AR/VR and stereo mode
    mIRAppRenderer = new IRAppRenderer(this, mActivity, Device.MODE.MODE_AR, false, 0.010f, 5f);
  }

  // Called when the surface is created or recreated.
  @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    // Call Vuforia function to (re)initialize rendering after first use
    // or after OpenGL ES context was lost (e.g. after onPause/onResume):
    vuforiaAppSession.onSurfaceCreated();

    mIRAppRenderer.onSurfaceCreated();
  }

  // Called when the surface changes size.
  @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
    // Call Vuforia function to handle render surface size changes:
    vuforiaAppSession.onSurfaceChanged(width, height);

    // RenderingPrimitives to be updated when some rendering change is done
    mIRAppRenderer.onConfigurationChanged(mIsActive);

    // Call function to initialize rendering:
    initRendering();
  }

  @Override public void onDrawFrame(GL10 gl) {
    // Call our function to render content from IRAppRenderer class
    mIRAppRenderer.render();
  }

  public void setActive(boolean active) {
    mIsActive = active;

    if (mIsActive) mIRAppRenderer.configureVideoBackground();
  }

  private void initRendering() {
    // Define clear color
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f : 1.0f);
  }

  public void updateRenderingPrimitives() {
    mIRAppRenderer.updateRenderingPrimitives();
  }

  // The render function.
  // This function is called from the IRAppRenderer by using the RenderingPrimitives views.
  // The state is owned by IRAppRenderer which is controlling its lifecycle.
  // NOTE: State should not be cached outside this method.
  public void renderFrame(State state, float[] projectionMatrix) {
    // Renders video background replacing Renderer.DrawVideoBackground()
    mIRAppRenderer.renderVideoBackground(state);

    // Set the device pose matrix as identity
    Matrix44F devicePoseMatrix = IRMath.Matrix44FIdentity();
    Matrix44F modelMatrix;

    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    GLES20.glEnable(GLES20.GL_CULL_FACE);

    // Start the target finder if we can't find an Image Target result.
    // If the Device pose exists, we can assume we will receive two
    // Trackable Results if the ImageTargetResult is available:
    // ImageTargetResult and DeviceTrackableResult
    int numExpectedResults = state.getDeviceTrackableResult() == null ? 0 : 1;
    if (state.getTrackableResults().size() <= numExpectedResults) {
      finderListener.startFinderIfStopped();
    }

    // Read device pose from the state and create a corresponding view matrix (inverse of the device pose)
    if (state.getDeviceTrackableResult() != null
        && state.getDeviceTrackableResult().getStatus() != TrackableResult.STATUS.NO_POSE) {
      modelMatrix = Tool.convertPose2GLMatrix(state.getDeviceTrackableResult().getPose());

      // We transpose here because Matrix44FInverse returns a transposed matrix
      devicePoseMatrix = IRMath.Matrix44FTranspose(IRMath.Matrix44FInverse(modelMatrix));
    }

    // Did we find any trackables this frame?
    TrackableResultList trackableResultList = state.getTrackableResults();
    for (TrackableResult result : trackableResultList) {
      modelMatrix = Tool.convertPose2GLMatrix(result.getPose());

      if (result.isOfType(ImageTargetResult.getClassType())) {
        finderListener.stopFinderIfStarted();

        // Renders the augmentation

        IRUtils.checkGLError("ImageRecognitionFragment renderFrame");
      }
    }

    GLES20.glDisable(GLES20.GL_DEPTH_TEST);

    Renderer.getInstance().end();
  }
}
