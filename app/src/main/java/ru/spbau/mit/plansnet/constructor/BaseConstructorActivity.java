package ru.spbau.mit.plansnet.constructor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import org.andengine.engine.camera.SmoothCamera;
import org.andengine.engine.camera.ZoomCamera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.andengine.entity.scene.Scene;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.detector.PinchZoomDetector;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.bitmap.BitmapTexture;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.ui.activity.SimpleLayoutGameActivity;
import org.andengine.util.color.Color;
import org.andengine.util.debug.Debug;

import java.io.IOException;

import ru.spbau.mit.plansnet.data.FloorMap;

public abstract class BaseConstructorActivity extends SimpleLayoutGameActivity {
    protected static int CAMERA_WIDTH = 0;
    protected static int CAMERA_HEIGHT = 0;

    protected final static int GRID_COLS = 30;
    protected final static int GRID_ROWS = 20;
    protected static int GRID_SIZE;

    protected Map map;
    protected FloorMap toOpenMap;
    protected PinchZoomDetector mPinchZoomDetector;

    protected void setCameraResolution() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return;
        }
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        CAMERA_WIDTH = metrics.widthPixels;
        CAMERA_HEIGHT = metrics.heightPixels;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        toOpenMap = (FloorMap) intent.getSerializableExtra("currentMap");
    }

    @Override
    public EngineOptions onCreateEngineOptions() {
        setCameraResolution();
        GRID_SIZE = (100000 / CAMERA_HEIGHT);
        Map.setGridSize(GRID_SIZE);
        Map.setGridCols(GRID_COLS);
        Map.setGridRows(GRID_ROWS);
        MapObjectLinear.setThickness(60000 / CAMERA_HEIGHT);
        final SmoothCamera camera = new SmoothCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT,
                CAMERA_WIDTH, CAMERA_HEIGHT, 50);
        camera.setCenter(GRID_SIZE * GRID_COLS / 2, GRID_SIZE * GRID_ROWS / 2);
        camera.setBounds(0, 0,
                GRID_SIZE * GRID_COLS, GRID_SIZE * GRID_ROWS);
        camera.setBoundsEnabled(true);
        return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED,
                new FillResolutionPolicy(), camera);
    }

    @Override
    protected void onCreateResources() {
        try {
            ITexture wallTexture = new BitmapTexture(this.getTextureManager(),
                    () -> getAssets().open("wall.png"));
            wallTexture.load();
            WallSprite.setTexture(TextureRegionFactory.extractFromTexture(wallTexture));
            ITexture windowTexture = new BitmapTexture(this.getTextureManager(),
                    () -> getAssets().open("window.png"));
            windowTexture.load();
            WindowSprite.setTexture(TextureRegionFactory.extractFromTexture(windowTexture));
            ITexture doorTexture = new BitmapTexture(this.getTextureManager(),
                    () -> getAssets().open("door.png"));
            doorTexture.load();
            DoorSprite.setTexture(TextureRegionFactory.extractFromTexture(doorTexture));
            ITexture stickersTextures[] = new ITexture[] {
                    new BitmapTexture(this.getTextureManager(),
                            () -> getAssets().open("exit.png")),
                    new BitmapTexture(this.getTextureManager(),
                            () -> getAssets().open("lift.png")),
                    new BitmapTexture(this.getTextureManager(),
                            () -> getAssets().open("stairs.png")),
                    new BitmapTexture(this.getTextureManager(),
                            () -> getAssets().open("wc.png"))
            };
            ITextureRegion[] stickersTextureRegions = new ITextureRegion[stickersTextures.length];
            for (int i = 0; i < stickersTextures.length; i++) {
                stickersTextures[i].load();
                stickersTextureRegions[i] = TextureRegionFactory
                        .extractFromTexture(stickersTextures[i]);
            }
            StickerSprite.setTextureRegions(stickersTextureRegions);
        } catch (IOException e) {
            Debug.e(e);
        }
    }

    protected void initPinchZoomDetector() {
        mPinchZoomDetector = new PinchZoomDetector(
                new PinchZoomDetector.IPinchZoomDetectorListener() {

                    float mInitialTouchZoomFactor;

                    @Override
                    public void onPinchZoomStarted(PinchZoomDetector pPinchZoomDetector,
                                                   TouchEvent pSceneTouchEvent) {
                        ZoomCamera mCamera = (ZoomCamera) getEngine().getCamera();
                        mInitialTouchZoomFactor = mCamera.getZoomFactor();
                    }

                    @Override
                    public void onPinchZoom(PinchZoomDetector pPinchZoomDetector,
                                            TouchEvent pTouchEvent, float pZoomFactor) {
                        final float newZoomFactor = mInitialTouchZoomFactor * pZoomFactor;
                        ZoomCamera mCamera = (ZoomCamera) getEngine().getCamera();
                        mCamera.setZoomFactor(newZoomFactor);
                    }

                    @Override
                    public void onPinchZoomFinished(PinchZoomDetector pPinchZoomDetector,
                                                    TouchEvent pTouchEvent, float pZoomFactor) {
                        final float newZoomFactor = mInitialTouchZoomFactor * pZoomFactor;
                        ZoomCamera mCamera = (ZoomCamera) getEngine().getCamera();
                        mCamera.setZoomFactor(newZoomFactor);
                    }
                });
        mPinchZoomDetector.setEnabled(true);
    }

    protected void initSprites() {
        MapObjectSprite.setVertexBufferObjectManager(getVertexBufferObjectManager());
        RoomSprite.setVertexBufferObjectManager(getVertexBufferObjectManager());
        RoomSprite.setFont(FontFactory.create(getEngine().getFontManager(),
                getEngine().getTextureManager(), 256, 256,
                Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL),
                100f, true, Color.WHITE_ABGR_PACKED_INT));
    }

    protected void initMap(Scene pScene) {
        if (toOpenMap != null) {
            map = new Map(toOpenMap, pScene);
        }
        if (map == null) {
            map = new Map();
        }
        MapObjectSprite.setMap(map);
        RoomSprite.setMap(map);

        for (MapObjectSprite o : map.getObjects()) {
            pScene.attachChild(o);
            pScene.registerTouchArea(o);
        }
    }
}
