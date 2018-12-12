package com.gigigo.irfragment.core;

import com.vuforia.State;

/**
 * The IRAppRendererControl interface is implemented
 * by each activity that uses IRApplicationSession
 */
public interface IRAppRendererControl {
  // This method must be implemented by the Renderer class that handles the content rendering.
  // This function is called for each view inside of a loop
  void renderFrame(State state, float[] projectionMatrix);
}
