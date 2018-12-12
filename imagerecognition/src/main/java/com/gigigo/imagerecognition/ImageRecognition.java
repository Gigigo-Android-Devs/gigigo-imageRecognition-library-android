package com.gigigo.imagerecognition;

public interface ImageRecognition<T> {

  /**
   * You MUST Call this method before calling startImageRecognition in order to provide a valid
   * context provider, Android context is not enough because this implementation should match
   * Context provider Implementation from GGG lib because current activity context is required
   *
   * @param contextProvider Context Provider
   */
  void setContextProvider(T contextProvider);

  /**
   * Checks permissions and starts Image recognitio activity using given credentials. If Permissions
   * were not granted User will be notified. If credentials are not valid you'll have an error log
   * message.
   *
   * @param credentials interface implementation with Vuforia keys
   * @Throws NotFoundContextException is context has not been provided before
   */
  void startImageRecognition(Credentials credentials);
}