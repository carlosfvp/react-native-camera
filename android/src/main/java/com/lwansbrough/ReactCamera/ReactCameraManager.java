package com.lwansbrough.ReactCamera;

import com.facebook.react.uimanager.CatalystStylesDiffMap;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIProp;
import com.google.zxing.Result;
import com.lwansbrough.ReactCamera.camera.CameraManager;

import android.hardware.Camera;
import android.widget.Toast;

public class ReactCameraManager extends SimpleViewManager<ReactCameraView> {

    public static final String REACT_CLASS = "ReactCameraView";
    private ThemedReactContext context;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @UIProp(UIProp.Type.STRING)
    public static final String PROP_PLACEHOLDERPROP = "placeholderprop";

    @Override
    public ReactCameraView createViewInstance(final ThemedReactContext context) {
        this.context = context;
        final ReactCameraView view = new ReactCameraView(context);
        view.setmCallBack(new ReactCameraView.IResultCallback() {
            @Override
            public void result(Result lastResult) {
                view.onReceiveNativeEvent(lastResult.getText());
            }
        });
        return view;
    }

    @Override
    public void updateView(final ReactCameraView view, final CatalystStylesDiffMap props) {
        super.updateView(view, props);
        view.maybeUpdateView();
    }
}
