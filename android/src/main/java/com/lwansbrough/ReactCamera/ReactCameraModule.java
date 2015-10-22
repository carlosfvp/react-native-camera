package com.lwansbrough.ReactCamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.util.Base64;
import android.widget.Toast;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.io.ByteArrayOutputStream;

/**
 * Created by marcell on 10/9/15.
 */
public class ReactCameraModule extends ReactContextBaseJavaModule {
    ReactApplicationContext reactContext;
    public ReactCameraModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "ReactCameraModule";
    }

    @ReactMethod
    public void _(final Callback successCallback) {
        final Callback cb = successCallback;
        //Helper.getCamera().setPreviewCallback(new MyPictureCallback(cb));
        //Helper.getCamera().takePicture(null, null, );
    }

    private class MyPictureCallback implements Camera.PictureCallback {
        Callback callback;

        MyPictureCallback(Callback callback) {
            this.callback = callback;
        }

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            camera.startPreview();
            String encoded = Base64.encodeToString(data, Base64.DEFAULT);
            callback.invoke(encoded);
        }
    }
}
