package ru.spbau.mit.plansnet.constructor;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.io.IOException;

import org.andengine.engine.camera.SmoothCamera;
import org.andengine.engine.camera.ZoomCamera;
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.background.Background;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.detector.PinchZoomDetector;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.ui.activity.SimpleLayoutGameActivity;
import org.andengine.util.debug.Debug;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.entity.scene.Scene;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.bitmap.BitmapTexture;

import ru.spbau.mit.plansnet.R;
import ru.spbau.mit.plansnet.data.FloorMap;

public class ConstructorActivity extends SimpleLayoutGameActivity {
	private static int CAMERA_WIDTH = 0;
    private static int CAMERA_HEIGHT = 0;

    private final static int GRID_COLS = 30;
    private final static int GRID_ROWS = 20;
    private static int GRID_SIZE;

    private ActionState state = ActionState.ADD;
    private int item = 0;
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
        toOpenMap = (FloorMap) intent.getSerializableExtra("toOpenMap");
    }

    @Override
    public void onBackPressed() {
        if (toOpenMap == null) {
            Log.d("VASYOID", "map is null!");
        } else {
            Intent intent = new Intent();
            intent.putExtra("toSaveMap", new FloorMap(toOpenMap.getName(),
                    toOpenMap.getGroupName(), toOpenMap.getBuildingName(), map));
            setResult(1, intent);
        }
        super.onBackPressed();
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
            ITexture doorTexture = new BitmapTexture(this.getTextureManager(),
                    () -> getAssets().open("door.png"));
            doorTexture.load();
            DoorSprite.setTexture(TextureRegionFactory.extractFromTexture(doorTexture));

        } catch (IOException e) {
            Debug.e(e);
        }
    }

	@Override
	protected Scene onCreateScene() {
        MapObjectSprite.setVertexBufferObjectManager(getVertexBufferObjectManager());
        RoomSprite.setVertexBufferObjectManager(getVertexBufferObjectManager());
        final Scene scene = new Scene();
		scene.setBackground(new Background(0.9f, 1, 0.6f));
        for (int i = 0; i <= GRID_COLS; i++) {
            Line line = new Line(GRID_SIZE * i, 0,
                    GRID_SIZE * i, GRID_SIZE * GRID_ROWS,
                    3, getVertexBufferObjectManager());
            line.setColor(0.7f, 0.7f, 0.7f);
            scene.attachChild(line);
        }
        for (int i = 0; i <= GRID_ROWS; i++) {
            Line line = new Line(0, GRID_SIZE * i,
                    GRID_SIZE * GRID_COLS, GRID_SIZE * i,
                    3, getVertexBufferObjectManager());
            line.setColor(0.7f, 0.7f, 0.7f);
            scene.attachChild(line);
        }
        scene.setOnSceneTouchListener(new IOnSceneTouchListener() {
            private float mInitialTouchX;
            private float mInitialTouchY;
            private float firstX, firstY;
            private float previousX, previousY;
            private Line currentLine = new Line(0, 0, 0, 0,
                    getVertexBufferObjectManager());
            private MapObjectLinear currentAdded;

            @Override
            public boolean onSceneTouchEvent(final Scene pScene, TouchEvent pSceneTouchEvent) {
                if (!pSceneTouchEvent.isActionMove() && map.checkRoomTouched(pSceneTouchEvent)) {
                    if (state == ActionState.ADD) {
                        runOnUpdateThread(() -> map.detachRemoved());
                        return false;
                    }
                    return true;
                }
                if (state == ActionState.DEL) {
                    runOnUpdateThread(() -> map.detachRemoved());
                    return false;
                }
                if (state == ActionState.MOVE) {
                    mPinchZoomDetector.onTouchEvent(pSceneTouchEvent);
                    if (pSceneTouchEvent.isActionDown()) {
                        mInitialTouchX = pSceneTouchEvent.getX();
                        mInitialTouchY = pSceneTouchEvent.getY();
                    }

                    if (pSceneTouchEvent.isActionMove()){
                        final float touchOffsetX = mInitialTouchX - pSceneTouchEvent.getX();
                        final float touchOffsetY = mInitialTouchY - pSceneTouchEvent.getY();
                        Camera mCamera = getEngine().getCamera();
                        mCamera.setCenter(mCamera.getCenterX() + touchOffsetX, mCamera.getCenterY() + touchOffsetY);
                    }
                    return false;
                }
                if (state == ActionState.COLOR) {
                    if (pSceneTouchEvent.isActionDown()) {
                        map.createRoom(pSceneTouchEvent.getX(), pSceneTouchEvent.getY(), pScene);
                    }
                    return false;
                }
                float currentX = Math.round(pSceneTouchEvent.getX() / GRID_SIZE) * GRID_SIZE;
                float currentY = Math.round(pSceneTouchEvent.getY() / GRID_SIZE) * GRID_SIZE;
                currentX = Math.min(currentX, GRID_SIZE * GRID_COLS);
                currentY = Math.min(currentY, GRID_SIZE * GRID_ROWS);
                currentX = Math.max(currentX, 0);
                currentY = Math.max(currentY, 0);
                PointF firstPoint = new PointF(firstX, firstY);
                PointF previousPoint = new PointF(previousX, previousY);
                PointF currentPoint = new PointF(currentX, currentY);

                switch (pSceneTouchEvent.getAction()) {
                    case TouchEvent.ACTION_DOWN:
                        firstX = previousX = currentX;
                        firstY = previousY = currentY;
                        if (state != ActionState.MOVE_WALL) {
                            currentLine.setPosition(firstX, firstY,
                                    currentX, currentY);
                            if (item == 0) {
                                currentAdded = new WallSprite();
                            } else {
                                currentAdded = new DoorSprite();
                            }
                            currentAdded.setPosition(currentLine);
                            pScene.attachChild(currentAdded);
                            pScene.registerTouchArea(currentAdded);
                        }
                        break;
                    case TouchEvent.ACTION_MOVE:
                        if (state == ActionState.MOVE_WALL) {
                            if (!previousPoint.equals(currentPoint)) {
                                map.moveObjects(firstPoint, previousPoint, currentPoint);
                                try {
                                    map.updateRooms(scene);
                                } catch (com.earcutj.exception.EarcutException ignored) {}
                            }
                        } else {
                            currentLine.setPosition(firstX, firstY, currentX, currentY);
                            currentAdded.setPosition(currentLine);
                        }
                        previousX = currentX;
                        previousY = currentY;
                        break;
                    case TouchEvent.ACTION_UP:
                        if (state == ActionState.MOVE_WALL) {
                            runOnUpdateThread(() -> map.detachRemoved());
                            if (!firstPoint.equals(currentPoint)) {
                                if (map.checkIntersections(firstPoint)) {
                                    map.moveObjects(firstPoint, currentPoint, firstPoint);
                                    map.updateRooms(scene);
                                } else {
                                    map.updateObjects(firstPoint);
                                }
                            }
                        } else {
                            if ((currentX != firstX || currentY != firstY) &&
                                    !map.checkIntersections(currentAdded)) {
                                map.addObject(currentAdded);
                                runOnUpdateThread(() -> map.detachRemoved());
                            } else {
                                pScene.detachChild(currentAdded);
                            }
                            currentLine.setPosition(0, 0, 0, 0);
                        }
                        break;
                }
                return false;
            }
        });
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
        return R.layout.activity_constructor;
    }

    @Override
    protected int getRenderSurfaceViewID() {
        return R.id.renderSurfaceView;
    }

    private void clearField() {
	    map.clear();
	    runOnUpdateThread(() -> map.detachRemoved());
    }

    public void showParams(View v) {
        findViewById(R.id.renderSurfaceView).setEnabled(false);
        View paramsView = findViewById(R.id.roomParamsView);
        paramsView.setVisibility(View.VISIBLE);
        paramsView.findViewById(R.id.roomParamsOk).setOnClickListener(v1 -> {
            paramsView.setVisibility(View.GONE);
            findViewById(R.id.renderSurfaceView).setEnabled(true);
        });
    }

    public void setItem(View v) {
        switch (v.getId()) {
            case R.id.buttonWall:
                item = 0;
                v.setEnabled(false);
                findViewById(R.id.buttonDoor).setEnabled(true);
                break;
            case R.id.buttonDoor:
                item = 1;
                v.setEnabled(false);
                findViewById(R.id.buttonWall).setEnabled(true);
                break;
        }
    }

    public void setState(View v) {

        if (v.getId() != R.id.buttonClear) {
            findViewById(R.id.buttonColor).setEnabled(true);
            findViewById(R.id.buttonDel).setEnabled(true);
            findViewById(R.id.buttonMove).setEnabled(true);
            findViewById(R.id.buttonMoveWall).setEnabled(true);
            findViewById(R.id.buttonAdd).setEnabled(true);
            v.setEnabled(false);
        }

        switch (v.getId()) {
            case R.id.buttonAdd:
                state = ActionState.ADD;
                break;
            case R.id.buttonDel:
                state = ActionState.DEL;
                break;
            case R.id.buttonMove:
                state = ActionState.MOVE;
                break;
            case R.id.buttonMoveWall:
                state = ActionState.MOVE_WALL;
                break;
            case R.id.buttonColor:
                state = ActionState.COLOR;
                break;
            case R.id.buttonClear:
                runOnUpdateThread(this::clearField);
                break;
        }
        map.setActionState(state);
    }

    public enum ActionState {
        ADD, DEL, MOVE, MOVE_WALL, COLOR
    }

}