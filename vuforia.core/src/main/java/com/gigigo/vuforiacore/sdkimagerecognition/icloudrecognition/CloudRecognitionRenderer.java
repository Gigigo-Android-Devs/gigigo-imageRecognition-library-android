package com.gigigo.vuforiacore.sdkimagerecognition.icloudrecognition;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import com.gigigo.vuforiacore.sdkimagerecognition.vuforiaenvironment.VuforiaSession;
import com.gigigo.vuforiacore.sdkimagerecognition.vuforiaenvironment.utils.SampleAppRenderer;
import com.gigigo.vuforiacore.sdkimagerecognition.vuforiaenvironment.utils.SampleAppRendererControl;
import com.gigigo.vuforiacore.sdkimagerecognition.vuforiaenvironment.utils.Teapot;
import com.gigigo.vuforiacore.sdkimagerecognition.vuforiaenvironment.utils.Texture;
import com.vuforia.Device;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.TrackableResult;
import java.util.Vector;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//asv todo hay q limpiar todo lo q tiene q ver con las texturas y tal, aparte el tema de las textura está tan raro x el tema del contexto
//necesariopara acceder a los resources, completamente trivial ya q se puede hacer antes y darle el textures

//revisar esta renderer https://stackoverflow.com/questions/41460581/android-vuforia-multi-target-rotate-object-on-touch
//y crear nuestro multirenderer para cambiar el mismo en caliente, en plan empezarconun cloudReco q una vez encuentre un
//resultado lo fije mediante el ground plane ese de fijar en un punto de la realidad, o pasar de cloudreco a un localrecog

public class CloudRecognitionRenderer implements GLSurfaceView.Renderer, SampleAppRendererControl {
  private static final float OBJECT_SCALE_FLOAT = 0.005f;//0.003f

  private VuforiaSession vuforiaAppSession;
  private SampleAppRenderer mSampleAppRenderer;

  private int shaderProgramID;
  private int vertexHandle;

  private int textureCoordHandle;
  private int mvpMatrixHandle;
  private int texSampler2DHandle;

  private Vector<Texture> mTextures;
  private Teapot mTeapot;
  private CloudRecognition mCloudReco;

  private boolean mIsActive = false;
  private ICloudRecognitionAR mCloudRecognitionAR;

  //asv rotation ini, gitanada extreme ¬¬

  public float getAngle() {
    if (mCloudRecognitionAR != null) {
      return mCloudRecognitionAR.getAngleRotation();
    } else

    {
      return -1;
    }
  }

  public void setAngle(float angle) {
    if (mCloudRecognitionAR != null) mCloudRecognitionAR.setAngleRotation(angle);
  }

  //asv rotation fin

  public CloudRecognitionRenderer(VuforiaSession session, CloudRecognition cloudRecog,
      ICloudRecognitionAR cloudRecognitionAR) {
    vuforiaAppSession = session;
    mCloudReco = cloudRecog;
    mCloudRecognitionAR = cloudRecognitionAR;
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

    // Call function to initialize rendering:
    initRendering();
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

  // Function for initializing the renderer.
  private void initRendering() {

    mCloudRecognitionAR.initRender(mCloudReco.mActivity);
     /*

    // Define clear color
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f : 1.0f);

    if (mTextures != null && mTextures.size() > 0) {
      for (Texture t : mTextures) {
        GLES20.glGenTextures(1, t.mTextureID, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, t.mWidth, t.mHeight, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, t.mData);
      }
    }
    shaderProgramID = VuforiaUtils.createProgramFromShaderSrc(CubeShaders.CUBE_MESH_VERTEX_SHADER,
        CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

    vertexHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexPosition");
    //normalHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexNormal");
    textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexTexCoord");
    mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID, "modelViewProjectionMatrix");
    texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID, "texSampler2D");

    mTeapot = new Teapot();*/
  }

  public void setTextures(Vector<Texture> textures) {
    mTextures = textures;
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
      System.out.println("*******************RENDER  1"
          + trackableResult.toString()
          + "projection:"
          + projectionMatrix.toString());
      // Renders the Augmentation View with the 3D Book data Panel

      renderAugmentation(trackableResult, projectionMatrix);
    } else {
      mCloudReco.startFinderIfStopped();
      System.out.println("*******************RENDER  2");
    }

    GLES20.glDisable(GLES20.GL_DEPTH_TEST);

    Renderer.getInstance().end();
  }

  private void renderAugmentation(TrackableResult trackableResult, float[] projectionMatrix) {
    mCloudRecognitionAR.onRenderAR(trackableResult, projectionMatrix);
   /* System.out.println("*******************RENDER AR");
    //asv check this
    //https://library.vuforia.com/content/vuforia-library/en/articles/Solution/Working-with-Vuforia-and-OpenGL-ES.html#How-To-Render-Static-3D-Models-using-OpenGL-ES
    ///    https://developer.vuforia.com/forum/ios/how-render-complex-3d-models-animations
    Matrix44F modelViewMatrix_Vuforia = Tool.convertPose2GLMatrix(trackableResult.getPose());
    float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();

    int textureIndex = 0;

    // deal with the modelview and projection matrices
    float[] modelViewProjection = new float[16];
    Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, OBJECT_SCALE_FLOAT);
    Matrix.scaleM(modelViewMatrix, 0, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);
    Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);

    // activate the shader program and bind the vertex/normal/tex coords
    GLES20.glUseProgram(shaderProgramID);
    GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mTeapot.getVertices());
    GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0,
        mTeapot.getTexCoords());

    GLES20.glEnableVertexAttribArray(vertexHandle);
    GLES20.glEnableVertexAttribArray(textureCoordHandle);

    // activate texture 0, bind it, and pass to shader
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
    GLES20.glUniform1i(texSampler2DHandle, 0);

    // pass the model view matrix to the shader
    GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);

    // finally draw the teapot
    GLES20.glDrawElements(GLES20.GL_TRIANGLES, mTeapot.getNumObjectIndex(),
        GLES20.GL_UNSIGNED_SHORT, mTeapot.getIndices());
    //try {
    //  Thread.sleep(50);
    //} catch (InterruptedException e) {
    //  e.printStackTrace();
    //}
    // disable the enabled arrays
    GLES20.glDisableVertexAttribArray(vertexHandle);
    GLES20.glDisableVertexAttribArray(textureCoordHandle);

    SampleUtils.checkGLError("CloudReco renderFrame");*/
  }
}
