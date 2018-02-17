package ru.spbau.mit.plansnet.constructor.constructorController;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import org.andengine.engine.camera.Camera;
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

import ru.spbau.mit.plansnet.constructor.objects.DoorSprite;
import ru.spbau.mit.plansnet.constructor.Geometry;
import ru.spbau.mit.plansnet.constructor.Map;
import ru.spbau.mit.plansnet.constructor.objects.MapObjectSprite;
import ru.spbau.mit.plansnet.constructor.objects.RoomSprite;
import ru.spbau.mit.plansnet.constructor.objects.StickerSprite;
import ru.spbau.mit.plansnet.constructor.objects.WallSprite;
import ru.spbau.mit.plansnet.constructor.objects.WindowSprite;
import ru.spbau.mit.plansnet.data.FloorMap;

/**
 * Base class for activities related to plans editor and viewer.
 * Provides common activity lifecycle methods.
 */
public abstract class BaseConstructorActivity extends SimpleLayoutGameActivity {

    public final static int MAP_WIDTH = 4096;
    public final static int MAP_HEIGHT = 2560;
    public final static int GRID_SIZE_MIN = 64;

    protected int mCameraWidth = 0;
    protected int mCameraHeight = 0;
    protected float mCameraZoomFactor = 1;
    protected int mGridSize = 256;

    protected Map mMap;
    protected FloorMap mToOpenMap;
    protected PinchZoomDetector mPinchZoomDetector;

    protected void setCameraResolution() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return;
        }
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        mCameraWidth = metrics.widthPixels;
        mCameraHeight = metrics.heightPixels;
    }

    @Override
    protected void onCreate(@NonNull Bundle pSavedInstanceState) {
        super.onCreate(pSavedInstanceState);
        Intent intent = getIntent();
        mToOpenMap = (FloorMap) intent.getSerializableExtra("currentMap");
    }

    /**
     * Creates camera and sets screen options.
     * @return engine options.
     */
    @Override
    public @NonNull EngineOptions onCreateEngineOptions() {
        setCameraResolution();
        final ZoomCamera camera = new ZoomCamera(0, 0, mCameraWidth, mCameraHeight);
        camera.setCenter(MAP_WIDTH / 2, MAP_HEIGHT / 2);
        camera.setBounds(-mGridSize, -mGridSize,
                MAP_WIDTH + mGridSize, MAP_HEIGHT + mGridSize);
        camera.setBoundsEnabled(true);
        mCameraZoomFactor = Math.min(1f, (float) mCameraHeight / camera.getBoundsHeight());
        camera.setZoomFactor(mCameraZoomFactor);
        return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED,
                new FillResolutionPolicy(), camera);
    }

    /**
     * Loads images of stickers and linear objects.
     */
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
                            () -> getAssets().open("wc.png")),
                    new BitmapTexture(this.getTextureManager(),
                            () -> getAssets().open("fire.png")),
                    new BitmapTexture(this.getTextureManager(),
                            () -> getAssets().open("smoke.png")),
                    new BitmapTexture(this.getTextureManager(),
                            () -> getAssets().open("voltage.png"))
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

    /**
     * Handles touch events corresponding to map shifting.
     * @param pSceneTouchEvent touch event to handle.
     */
    protected void moveMap(@NonNull TouchEvent pSceneTouchEvent) {
        mPinchZoomDetector.onTouchEvent(pSceneTouchEvent);
        if (pSceneTouchEvent.isActionMove()) {
            final MotionEvent event = pSceneTouchEvent.getMotionEvent();
            if (event.getHistorySize() == 0) {
                return;
            }
            final float touchOffsetX = event.getHistoricalX(0) - event.getX();
            final float touchOffsetY = event.getHistoricalY(0) - event.getY();
            Camera mCamera = getEngine().getCamera();
            mCamera.setCenter(mCamera.getCenterX() + touchOffsetX,
                    mCamera.getCenterY() + touchOffsetY);
        }
    }

    /**
     * Initialises zoom detector.
     */
    protected void initPinchZoomDetector() {
        mPinchZoomDetector = new PinchZoomDetector(
                new PinchZoomDetector.IPinchZoomDetectorListener() {

                    float mInitialTouchZoomFactor;

                    @Override
                    public void onPinchZoomStarted(@NonNull PinchZoomDetector pPinchZoomDetector,
                                                   @NonNull TouchEvent pSceneTouchEvent) {
                        ZoomCamera mCamera = (ZoomCamera) getEngine().getCamera();
                        mInitialTouchZoomFactor = mCamera.getZoomFactor();
                    }

                    @Override
                    public void onPinchZoom(@NonNull PinchZoomDetector pPinchZoomDetector,
                                            @NonNull TouchEvent pTouchEvent,
                                            float pZoomFactor) {
                        float newZoomFactor = mInitialTouchZoomFactor * pZoomFactor;
                        ZoomCamera mCamera = (ZoomCamera) getEngine().getCamera();
                        newZoomFactor = Geometry.bringValueToBounds(newZoomFactor,
                                0.8f * mCameraZoomFactor, 6.0f * mCameraZoomFactor);
                        mCamera.setZoomFactor(newZoomFactor);
                    }

                    @Override
                    public void onPinchZoomFinished(@NonNull PinchZoomDetector pPinchZoomDetector,
                                                    @NonNull TouchEvent pTouchEvent,
                                                    float pZoomFactor) {
                        onPinchZoom(pPinchZoomDetector, pTouchEvent, pZoomFactor);
                    }
                });
        mPinchZoomDetector.setEnabled(true);
    }

    /**
     * Initialises static fields in MapObjectSprite classes.
     */
    protected void initSprites() {
        MapObjectSprite.setVertexBufferObjectManager(getVertexBufferObjectManager());
        RoomSprite.setVertexBufferObjectManager(getVertexBufferObjectManager());
        RoomSprite.setFont(FontFactory.create(getEngine().getFontManager(),
                getEngine().getTextureManager(), 512, 512,
                Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL),
                100f, true, Color.WHITE_ABGR_PACKED_INT));
    }

    /**
     * Initialises the map with objects.
     * @param pScene scene where the map will be drawn.
     */
    protected void initMap(@NonNull Scene pScene) {
        if (mToOpenMap != null) {
            mMap = new Map(mToOpenMap, pScene);
        }
        if (mMap == null) {
            mMap = new Map();
        }
        Map.setGridSize(mGridSize);
        MapObjectSprite.setMap(mMap);
        for (MapObjectSprite o : mMap.getObjects()) {
            pScene.attachChild(o);
            pScene.registerTouchArea(o);
        }
    }

}
