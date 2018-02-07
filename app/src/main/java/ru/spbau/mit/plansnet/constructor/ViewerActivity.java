package ru.spbau.mit.plansnet.constructor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.SmoothCamera;
import org.andengine.engine.camera.ZoomCamera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
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

import ru.spbau.mit.plansnet.R;
import ru.spbau.mit.plansnet.data.FloorMap;

public class ViewerActivity extends SimpleLayoutGameActivity {
    private static int CAMERA_WIDTH = 0;
    private static int CAMERA_HEIGHT = 0;

    private final static int GRID_COLS = 30;
    private final static int GRID_ROWS = 20;
    private static int GRID_SIZE;

    private Map map;
    private FloorMap toOpenMap;

    private PinchZoomDetector mPinchZoomDetector;

    private void setCameraResolution() {
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
        final SmoothCamera camera = new SmoothCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT, CAMERA_WIDTH, CAMERA_HEIGHT, 50);
        camera.setCenter(GRID_SIZE * GRID_COLS / 2, GRID_SIZE * GRID_ROWS / 2);
        camera.setBounds(0, 0, GRID_SIZE * GRID_COLS, GRID_SIZE * GRID_ROWS);
        camera.setBoundsEnabled(true);
        return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new FillResolutionPolicy(), camera);
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
            ITextureRegion[] stickersTextureRegions = new ITextureRegion[4];
            for (int i = 0; i < 4; i++) {
                stickersTextures[i].load();
                stickersTextureRegions[i] = TextureRegionFactory
                        .extractFromTexture(stickersTextures[i]);
            }
            StickerSprite.setTextureRegions(stickersTextureRegions);
        } catch (IOException e) {
            Debug.e(e);
        }
    }

    @Override
    protected Scene onCreateScene() {
        MapObjectSprite.setVertexBufferObjectManager(getVertexBufferObjectManager());
        RoomSprite.setVertexBufferObjectManager(getVertexBufferObjectManager());
        RoomSprite.setFont(FontFactory.create(getEngine().getFontManager(),
                getEngine().getTextureManager(), 256, 256,
                Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL),
                100f, true, Color.WHITE_ABGR_PACKED_INT));
        final Scene scene = new Scene();
        scene.setBackground(new Background(0.95f, 0.95f, 1f));

        for (int i = 0; i <= GRID_COLS; i += GRID_COLS) {
            Line line = new Line(GRID_SIZE * i, 0,
                    GRID_SIZE * i, GRID_SIZE * GRID_ROWS,
                    3, getVertexBufferObjectManager());
            line.setColor(0.7f, 0.7f, 0.7f);
            scene.attachChild(line);
        }
        for (int i = 0; i <= GRID_ROWS; i += GRID_ROWS) {
            Line line = new Line(0, GRID_SIZE * i,
                    GRID_SIZE * GRID_COLS, GRID_SIZE * i,
                    3, getVertexBufferObjectManager());
            line.setColor(0.7f, 0.7f, 0.7f);
            scene.attachChild(line);
        }

        scene.setOnSceneTouchListener(new IOnSceneTouchListener() {
            private float mInitialTouchX;
            private float mInitialTouchY;

            @Override
            public boolean onSceneTouchEvent(final Scene pScene, TouchEvent pSceneTouchEvent) {

            mPinchZoomDetector.onTouchEvent(pSceneTouchEvent);
            if (pSceneTouchEvent.isActionDown()) {
                RoomSprite room = map.getRoomTouched(pSceneTouchEvent);
                if (room != null) {
                    runOnUiThread(() -> showParams(room));
                }
                mInitialTouchX = pSceneTouchEvent.getX();
                mInitialTouchY = pSceneTouchEvent.getY();
            } else if (pSceneTouchEvent.isActionMove()){
                final float touchOffsetX = mInitialTouchX - pSceneTouchEvent.getX();
                final float touchOffsetY = mInitialTouchY - pSceneTouchEvent.getY();
                Camera mCamera = getEngine().getCamera();
                mCamera.setCenter(mCamera.getCenterX() + touchOffsetX, mCamera.getCenterY() + touchOffsetY);
            }
            return false;
        }});
        scene.setTouchAreaBindingOnActionDownEnabled(true);
        scene.setTouchAreaBindingOnActionMoveEnabled(true);
        scene.setOnSceneTouchListenerBindingOnActionDownEnabled(true);
        mPinchZoomDetector = new PinchZoomDetector(new PinchZoomDetector.IPinchZoomDetectorListener() {

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

        if (toOpenMap != null) {
            map = new Map(toOpenMap, scene);
        }
        if (map == null) {
            map = new Map();
        }
        MapObjectSprite.setMap(map);
        RoomSprite.setMap(map);

        for (MapObjectSprite o : map.getObjects()) {
            scene.attachChild(o);
            scene.registerTouchArea(o);
        }

        scene.sortChildren();
        return scene;
    }

    @Override
    protected int getLayoutID() {
        return R.layout.activity_viewer;
    }

    @Override
    protected int getRenderSurfaceViewID() {
        return R.id.viewerView;
    }

    public void showParams(RoomSprite pRoom) {
        findViewById(R.id.viewerView).setEnabled(false);
        View paramsView = findViewById(R.id.roomPropertiesView);
        paramsView.setVisibility(View.VISIBLE);
        TextView roomName = findViewById(R.id.roomPropertiesName);
        TextView roomDescription = findViewById(R.id.roomPropertiesDescription);
        roomName.setText(pRoom.getTitle());
        roomDescription.setText(pRoom.getDescription());
        paramsView.findViewById(R.id.roomPropertiesOk).setOnClickListener(v1 -> {
            paramsView.setVisibility(View.GONE);
            findViewById(R.id.viewerView).setEnabled(true);
        });
    }

}