package com.gigigo.imagerecognition.vuforia;

import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import com.gigigo.vuforiacore.sdkimagerecognition.icloudrecognition.ICloudRecognitionAR;
import com.gigigo.vuforiacore.sdkimagerecognition.vuforiaenvironment.utils.BowlAndSpoonObject;
import com.gigigo.vuforiacore.sdkimagerecognition.vuforiaenvironment.utils.CubeShaders;
import com.gigigo.vuforiacore.sdkimagerecognition.vuforiaenvironment.utils.SampleUtils;
import com.gigigo.vuforiacore.sdkimagerecognition.vuforiaenvironment.utils.Teapot;
import com.gigigo.vuforiacore.sdkimagerecognition.vuforiaenvironment.utils.Texture;
import com.gigigo.vuforiacore.sdkimagerecognition.vuforiaenvironment.utils.VuforiaUtils;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.Tool;
import com.vuforia.TrackableResult;
import com.vuforia.Vuforia;
import java.util.Vector;

/**
 * Created by nubor on 14/03/2018.
 */

public class CloudRecoARRotationImpl implements ICloudRecognitionAR {

  private int shaderProgramID;
  private int vertexHandle;

  private int textureCoordHandle;
  private int mvpMatrixHandle;
  private int texSampler2DHandle;

  private Vector<Texture> mTextures;
  private Teapot mTeapot;
  Activity mActivity; //asv solo se usa pa cargar una imagen

  private static final float OBJECT_SCALE_FLOAT = 0.005f;//0.003f

  //bowl

  final static float kBowlScaleX = 0.12f * 0.15f * 3;
  final static float kBowlScaleY = 0.12f * 0.15f * 3;
  final static float kBowlScaleZ = 0.12f * 0.15f * 3;
  private BowlAndSpoonObject bowlAndSpoonObject = new BowlAndSpoonObject();

  private void loadTextures() {
    mTextures.add(Texture.loadTextureFromApk("TextureTeapotRed.png", mActivity.getAssets()));
    mTextures.add(Texture.loadTextureFromApk("TextureBowlAndSpoon.png", mActivity.getAssets()));
  }

  @Override public void initRender(Activity activity) {
    mActivity = activity;
    // Define clear color
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f : 1.0f);
    mTextures = new Vector<Texture>();
    loadTextures();
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
    textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexTexCoord");
    mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID, "modelViewProjectionMatrix");
    texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID, "texSampler2D");

    mTeapot = new Teapot();
  }

  private double prevTime;//for rotation

  @Override public void onRenderAR(TrackableResult trackableResult, float[] projectionMatrix) {
    System.out.println("*******************RENDER AR");
    //asv check this
    //https://library.vuforia.com/content/vuforia-library/en/articles/Solution/Working-with-Vuforia-and-OpenGL-ES.html#How-To-Render-Static-3D-Models-using-OpenGL-ES
    ///    https://developer.vuforia.com/forum/ios/how-render-complex-3d-models-animations
    Matrix44F modelViewMatrix_Vuforia = Tool.convertPose2GLMatrix(trackableResult.getPose());
    float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();

    int textureIndex = 0;

    // deal with the modelview and projection matrices
    float[] modelViewProjection = new float[16];

    if (mAngle == -1) {
      Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, OBJECT_SCALE_FLOAT);
      Matrix.scaleM(modelViewMatrix, 0, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);
      Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);
      // activate the shader program and bind the vertex/normal/tex coords
      GLES20.glUseProgram(shaderProgramID);
      GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0,
          mTeapot.getVertices());
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
      GLES20.glDisable(GLES20.GL_CULL_FACE);//asv added
    }
    //asv rotation
    //mAngle = -90.0f;
    if (mAngle != -1) {
      //double time = System.currentTimeMillis(); // Get real time difference
      //float dt = (float) (time - prevTime) / 1000; // from frame to frame
      //
      //mAngle += dt * 180.0f / 3.1415f; // Animate angle based on time
      //mAngle %= 360;
      //  prevTime = time;
      Log.d("", "Delta animation time: " + mAngle);
      modelViewMatrix = modelViewMatrix_Vuforia.getData();
      Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, OBJECT_SCALE_FLOAT*20); //asv subo la tetera xa q pueda servir el tazon
      Matrix.rotateM(modelViewMatrix, 0, mAngle, 0.0f, 1.0f, 0.0f);

      Matrix.scaleM(modelViewMatrix, 0, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);
      Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);

      GLES20.glUseProgram(shaderProgramID);//new
      GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0,
          mTeapot.getVertices());
      GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0,
          mTeapot.getTexCoords());

      GLES20.glEnableVertexAttribArray(vertexHandle);//new
      GLES20.glEnableVertexAttribArray(textureCoordHandle);//new

      GLES20.glActiveTexture(GLES20.GL_TEXTURE0);//new
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
      GLES20.glUniform1i(texSampler2DHandle, 0);//new
      // pass the model view matrix to the shader
      GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);

      // finally draw the teapot
      GLES20.glDrawElements(GLES20.GL_TRIANGLES, mTeapot.getNumObjectIndex(),
          GLES20.GL_UNSIGNED_SHORT, mTeapot.getIndices());
      //  Matrix.setRotateM(modelViewMatrix, 0, mAngle, 0, 0, 1.0f);

      if (mAngle > 30 && mAngle < 35) {
      // Draw the bowl:
      modelViewMatrix = modelViewMatrix_Vuforia.getData();
      // Remove the following line to make the bowl stop spinning:
      // animateBowl(modelViewMatrix);

       Matrix.translateM(modelViewMatrix, 0, 0, -0.27f , 0); //asv esto es para pintarlo justo centrado y q coincida con la boca de la tetera
      // Matrix.translateM(modelViewMatrix, 0, -0.50f  * 0.12f*3, -0.50f*0.02f  , 0.00135f * 0.12f);
      Matrix.scaleM(modelViewMatrix, 0, kBowlScaleX, kBowlScaleY, kBowlScaleZ);
      Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);

      GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0,
          bowlAndSpoonObject.getVertices());
      GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0,
          bowlAndSpoonObject.getTexCoords());

      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(1).mTextureID[0]);
      GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
      GLES20.glDrawElements(GLES20.GL_TRIANGLES, bowlAndSpoonObject.getNumObjectIndex(),
          GLES20.GL_UNSIGNED_SHORT, bowlAndSpoonObject.getIndices());
      } else {
        // Clean up and leave
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        Renderer.getInstance().end();
        return;
      }
    }

    GLES20.glDisableVertexAttribArray(vertexHandle);
    GLES20.glDisableVertexAttribArray(textureCoordHandle);

    SampleUtils.checkGLError("CloudReco renderFrame");
  }

  public volatile float mAngle = -1;

  @Override public void setAngleRotation(float angleRotation) {
    mAngle = angleRotation;
  }

  @Override public float getAngleRotation() {
    return mAngle;
  }
}
