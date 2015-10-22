package com.lwansbrough.ReactCamera;

import android.hardware.Camera;

import com.lwansbrough.ReactCamera.camera.CameraManager;

/**
 * Created by marcell on 10/9/15.
 */
public class Helper {

    private static CameraManager camera = null;

    public static void setCamera(CameraManager theCamera) {
        camera = theCamera;
    }

    public static CameraManager getCamera() {
        return camera;
    }
}
