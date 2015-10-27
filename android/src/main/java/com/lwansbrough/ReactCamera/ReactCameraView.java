package com.lwansbrough.ReactCamera;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.lwansbrough.ReactCamera.camera.CameraManager;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

class ReactCameraView extends ViewfinderView implements SurfaceHolder.Callback {
    private static final String TAG = ReactCameraView.class.getSimpleName();

    private SurfaceHolder surfaceHolder;
    private ReactCameraHandler handler;
    private Result savedResultToShow;
    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType,?> decodeHints;
    private String characterSet;
    private boolean hasSurface;
    //private InactivityTimer inactivityTimer;
    private Result lastResult;
    //private BeepManager beepManager;
    private IResultCallback mCallBack;
    private AmbientLightManager ambientLightManager;


    public interface IResultCallback {
        void result(Result lastResult);
    }

    public Handler getHandler() {
        return handler;
    }

    public void setmCallBack(IResultCallback mCallBack) {
        this.mCallBack = mCallBack;
    }

    public void onReceiveNativeEvent(String value) {
        WritableMap event = Arguments.createMap();
        event.putString("message", value);
        ReactContext reactContext = (ReactContext)getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "topChange",
                event);
    }

    @UIProp(UIProp.Type.STRING)
    public static final String PROP_SRC = "test";

    public ReactCameraView(ThemedReactContext context) {
        super(context);

        ambientLightManager = new AmbientLightManager(context);

        startScan();
    }

    public void maybeUpdateView() {
        return;
    }

    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        //inactivityTimer.onActivity();
        lastResult = rawResult;
//    ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(getActivity(), rawResult);

        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            //beepManager.playBeepSoundAndVibrate();
            drawResultPoints(barcode, scaleFactor, rawResult);
            if (mCallBack!=null) {
                mCallBack.result(rawResult);
            }

            restartPreviewAfterDelay(500L);
        }
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor) {
        if (a != null && b != null) {
            canvas.drawLine(scaleFactor * a.getX(),
                    scaleFactor * a.getY(),
                    scaleFactor * b.getX(),
                    scaleFactor * b.getY(),
                    paint);
        }
    }

    private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
            } else if (points.length == 4 &&
                    (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
                            rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
                drawLine(canvas, paint, points[2], points[3], scaleFactor);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    if (point != null) {
                        canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
                    }
                }
            }
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (this.getCameraManager().isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            this.getCameraManager().openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new ReactCameraHandler(this, decodeFormats, decodeHints, characterSet, this.getCameraManager());
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
//      displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
//      displayFrameworkBugMessageAndExit();
        }
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(IConstants.RESTART_PREVIEW, delayMS);
        }
    }

    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, IConstants.DECODE_SUCCEDED, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (getCameraManager() == null) {
            Log.e(TAG, "stopScan: scan already stopped");
            return;
        }

        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }

        //inactivityTimer.onPause();
        ambientLightManager.stop();
        //beepManager.close();
        getCameraManager().closeDriver();
        setCameraManager(null);

        if (!hasSurface) {
            SurfaceHolder surfaceHolder = this.getHolder();
            surfaceHolder.removeCallback(this);
        }
    }

    public void startScan()
    {
        if (getCameraManager() != null) {
            Log.e(TAG, "startScan: scan already started.");
            return;
        }

        this.setCameraManager( new CameraManager(this.getContext()));
        this.getCameraManager().setManualCameraId(-1);

        handler = null;
        lastResult = null;

//
//    if (prefs.getBoolean(PreferencesActivity.KEY_DISABLE_AUTO_ORIENTATION, true)) {
//      //noinspection ResourceType
//      getActivity().setRequestedOrientation(getCurrentOrientation());
//    } else {
//      getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
//    }

        SurfaceHolder surfaceHolder = this.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
        ambientLightManager.start(getCameraManager());

        decodeFormats = null;
        characterSet = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        //TODO handle rotation etc
    }
}
