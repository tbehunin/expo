/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */


package abi45_0_0.host.exp.exponent.modules.api.components.svg;

import android.content.res.Resources;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.Region;

import abi45_0_0.com.facebook.react.bridge.Arguments;
import abi45_0_0.com.facebook.react.bridge.Promise;
import abi45_0_0.com.facebook.react.bridge.ReactApplicationContext;
import abi45_0_0.com.facebook.react.bridge.ReactContextBaseJavaModule;
import abi45_0_0.com.facebook.react.bridge.ReactMethod;
import abi45_0_0.com.facebook.react.bridge.ReadableMap;
import abi45_0_0.com.facebook.react.bridge.WritableMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.annotation.Nonnull;

import static abi45_0_0.com.facebook.react.common.StandardCharsets.UTF_8;

class RNSVGRenderableManager extends ReactContextBaseJavaModule {
    RNSVGRenderableManager(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Nonnull
    @Override
    public String getName() {
        return "RNSVGRenderableManager";
    }

    @SuppressWarnings("unused")
    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean isPointInFill(int tag, ReadableMap options) {
        RenderableView svg = RenderableViewManager.getRenderableViewByTag(tag);
        if (svg == null) {
            return false;
        }

        float scale = svg.mScale;
        float x = (float) options.getDouble("x") * scale;
        float y = (float) options.getDouble("y") * scale;

        int i = svg.hitTest(new float[]{x, y});
        return i != -1;
    }

    @SuppressWarnings("unused")
    @ReactMethod(isBlockingSynchronousMethod = true)
    public boolean isPointInStroke(int tag, ReadableMap options) {
        RenderableView svg = RenderableViewManager.getRenderableViewByTag(tag);
        if (svg == null) {
            return false;
        }

        try {
            svg.getPath(null, null);
        } catch (NullPointerException e) {
            svg.invalidate();
            return false;
        }

        svg.initBounds();

        float scale = svg.mScale;
        int x = (int) (options.getDouble("x") * scale);
        int y = (int) (options.getDouble("y") * scale);

        Region strokeRegion = svg.mStrokeRegion;
        return strokeRegion != null && strokeRegion.contains(x, y);
    }

    @SuppressWarnings("unused")
    @ReactMethod(isBlockingSynchronousMethod = true)
    public float getTotalLength(int tag) {
        RenderableView svg = RenderableViewManager.getRenderableViewByTag(tag);
        if (svg == null) {
            return 0;
        }

        Path path;

        try {
            path = svg.getPath(null, null);
        } catch (NullPointerException e) {
            svg.invalidate();
            return -1;
        }

        PathMeasure pm = new PathMeasure(path, false);
        return pm.getLength() / svg.mScale;
    }

    @SuppressWarnings("unused")
    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap getPointAtLength(int tag, ReadableMap options) {
        RenderableView svg = RenderableViewManager.getRenderableViewByTag(tag);
        if (svg == null) {
            return Arguments.createMap();
        }

        Path path;

        try {
            path = svg.getPath(null, null);
        } catch (NullPointerException e) {
            svg.invalidate();
            return Arguments.createMap();
        }

        PathMeasure pm = new PathMeasure(path, false);
        float length = (float) options.getDouble("length");
        float scale = svg.mScale;

        float[] pos = new float[2];
        float[] tan = new float[2];
        float distance = Math.max(0, Math.min(length, pm.getLength()));
        pm.getPosTan(distance, pos, tan);

        double angle = Math.atan2(tan[1], tan[0]);
        WritableMap result = Arguments.createMap();
        result.putDouble("x", pos[0] / scale);
        result.putDouble("y", pos[1] / scale);
        result.putDouble("angle", angle);
        return result;
    }

    @SuppressWarnings("unused")
    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap getBBox(int tag, ReadableMap options) {
        RenderableView svg = RenderableViewManager.getRenderableViewByTag(tag);
        if (svg == null) {
            return Arguments.createMap();
        }

        boolean fill = options.getBoolean("fill");
        boolean stroke = options.getBoolean("stroke");
        boolean markers = options.getBoolean("markers");
        boolean clipped = options.getBoolean("clipped");

        try {
            svg.getPath(null, null);
        } catch (NullPointerException e) {
            svg.invalidate();
            return Arguments.createMap();
        }

        float scale = svg.mScale;
        svg.initBounds();

        RectF bounds = new RectF();
        RectF fillBounds = svg.mFillBounds;
        RectF strokeBounds = svg.mStrokeBounds;
        RectF markerBounds = svg.mMarkerBounds;
        RectF clipBounds = svg.mClipBounds;

        if (fill && fillBounds != null) {
            bounds.union(fillBounds);
        }
        if (stroke && strokeBounds != null) {
            bounds.union(strokeBounds);
        }
        if (markers && markerBounds != null) {
            bounds.union(markerBounds);
        }
        if (clipped && clipBounds != null) {
            bounds.intersect(clipBounds);
        }

        WritableMap result = Arguments.createMap();
        result.putDouble("x", bounds.left / scale);
        result.putDouble("y", bounds.top / scale);
        result.putDouble("width", bounds.width() / scale);
        result.putDouble("height", bounds.height() / scale);
        return result;
    }

    @SuppressWarnings("unused")
    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap getCTM(int tag) {
        RenderableView svg = RenderableViewManager.getRenderableViewByTag(tag);
        if (svg == null) {
            return Arguments.createMap();
        }

        float scale = svg.mScale;
        Matrix ctm = new Matrix(svg.mCTM);
        Matrix invViewBoxMatrix = svg.getSvgView().mInvViewBoxMatrix;
        ctm.preConcat(invViewBoxMatrix);

        float[] values = new float[9];
        ctm.getValues(values);

        WritableMap result = Arguments.createMap();
        result.putDouble("a", values[Matrix.MSCALE_X]);
        result.putDouble("b", values[Matrix.MSKEW_Y]);
        result.putDouble("c", values[Matrix.MSKEW_X]);
        result.putDouble("d", values[Matrix.MSCALE_Y]);
        result.putDouble("e", values[Matrix.MTRANS_X] / scale);
        result.putDouble("f", values[Matrix.MTRANS_Y] / scale);
        return result;
    }

    @SuppressWarnings("unused")
    @ReactMethod(isBlockingSynchronousMethod = true)
    public WritableMap getScreenCTM(int tag) {
        RenderableView svg = RenderableViewManager.getRenderableViewByTag(tag);
        if (svg == null) {
            return Arguments.createMap();
        }

        float[] values = new float[9];
        svg.mCTM.getValues(values);
        float scale = svg.mScale;

        WritableMap result = Arguments.createMap();
        result.putDouble("a", values[Matrix.MSCALE_X]);
        result.putDouble("b", values[Matrix.MSKEW_Y]);
        result.putDouble("c", values[Matrix.MSKEW_X]);
        result.putDouble("d", values[Matrix.MSCALE_Y]);
        result.putDouble("e", values[Matrix.MTRANS_X] / scale);
        result.putDouble("f", values[Matrix.MTRANS_Y] / scale);
        return result;
    }

    @ReactMethod
    public void getRawResource(String name, Promise promise) {
        try {
            ReactApplicationContext context = getReactApplicationContext();
            Resources resources = context.getResources();
            String packageName = context.getPackageName();
            int id = resources.getIdentifier(name, "raw", packageName);
            InputStream stream = resources.openRawResource(id);
            try {
                InputStreamReader reader = new InputStreamReader(stream, UTF_8);
                char[] buffer = new char[DEFAULT_BUFFER_SIZE];
                StringBuilder builder = new StringBuilder();
                int n;
                while ((n = reader.read(buffer, 0, DEFAULT_BUFFER_SIZE)) != EOF) {
                    builder.append(buffer, 0, n);
                }
                String result = builder.toString();
                promise.resolve(result);
            } finally {
                try {
                    stream.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            promise.reject(e);
        }
    }

    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
}
