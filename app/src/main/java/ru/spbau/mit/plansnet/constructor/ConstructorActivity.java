package ru.spbau.mit.plansnet.constructor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.concurrent.Semaphore;

import org.andengine.entity.primitive.Line;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.background.Background;
import org.andengine.input.touch.TouchEvent;
import org.andengine.engine.camera.Camera;
import org.andengine.entity.scene.Scene;

import ru.spbau.mit.plansnet.R;
import ru.spbau.mit.plansnet.data.FloorMap;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;
import static android.graphics.Color.TRANSPARENT;
import static ru.spbau.mit.plansnet.constructor.StickerSprite.StickerType.EXIT;
import static ru.spbau.mit.plansnet.constructor.StickerSprite.StickerType.LIFT;
import static ru.spbau.mit.plansnet.constructor.StickerSprite.StickerType.STAIRS;
import static ru.spbau.mit.plansnet.constructor.StickerSprite.StickerType.WC;

public class ConstructorActivity extends BaseConstructorActivity {

    private ActionState state = ActionState.ADD;
    private MapItem item = MapItem.WALL;
    private StickerSprite.StickerType currentSticker = EXIT;

    private void createGrid(Scene pScene) {
        for (int i = 0; i <= GRID_COLS; i++) {
            Line line = new Line(GRID_SIZE * i, 0,
                    GRID_SIZE * i, GRID_SIZE * GRID_ROWS,
                    3, getVertexBufferObjectManager());
            line.setColor(0.7f, 0.7f, 0.7f);
            pScene.attachChild(line);
        }
        for (int i = 0; i <= GRID_ROWS; i++) {
            Line line = new Line(0, GRID_SIZE * i,
                    GRID_SIZE * GRID_COLS, GRID_SIZE * i,
                    3, getVertexBufferObjectManager());
            line.setColor(0.7f, 0.7f, 0.7f);
            pScene.attachChild(line);
        }
    }

    private void initScene(Scene pScene) {
        pScene.setOnSceneTouchListener(new IOnSceneTouchListener() {
            private float mInitialTouchX;
            private float mInitialTouchY;
            private PointF firstPoint = new PointF();
            private PointF currentPoint = new PointF();
            private PointF previousPoint = new PointF();
            private Line currentLine = new Line(0, 0, 0, 0,
                    getVertexBufferObjectManager());
            private MapObjectLinear currentAdded;

            private void moveMap(TouchEvent pSceneTouchEvent) {
                mPinchZoomDetector.onTouchEvent(pSceneTouchEvent);
                if (pSceneTouchEvent.isActionDown()) {
                    mInitialTouchX = pSceneTouchEvent.getX();
                    mInitialTouchY = pSceneTouchEvent.getY();
                } else if (pSceneTouchEvent.isActionMove()){
                    final float touchOffsetX = mInitialTouchX - pSceneTouchEvent.getX();
                    final float touchOffsetY = mInitialTouchY - pSceneTouchEvent.getY();
                    Camera mCamera = getEngine().getCamera();
                    mCamera.setCenter(mCamera.getCenterX() + touchOffsetX,
                            mCamera.getCenterY() + touchOffsetY);
                }
            }

            void createRoom(RoomSprite pTouchedRoom, TouchEvent pSceneTouchEvent) {
                if (!pSceneTouchEvent.isActionDown() || pTouchedRoom != null) {
                    return;
                }
                RoomSprite currentRoom = map.createRoom(pSceneTouchEvent.getX(),
                        pSceneTouchEvent.getY(), pScene);
                if (currentRoom != null) {
                    showParams(currentRoom);
                }
            }

            private void moveWall(TouchEvent pSceneTouchEvent) {
                switch (pSceneTouchEvent.getAction()) {
                    case TouchEvent.ACTION_DOWN:
                        firstPoint.set(currentPoint);
                        previousPoint.set(currentPoint);
                        map.setScaleByPoint(currentPoint, 1.0f, 1.4f);
                        break;
                    case TouchEvent.ACTION_MOVE:
                        if (previousPoint.equals(currentPoint)) {
                            break;
                        }
                        map.moveObjects(firstPoint, previousPoint, currentPoint);
                        try {
                            map.updateRooms(pScene);
                        } catch (com.earcutj.exception.EarcutException ignored) {}
                        previousPoint.set(currentPoint);
                        break;
                    case TouchEvent.ACTION_UP:
                        map.detachRemoved(mEngine);
                        map.setScaleByPoint(firstPoint, 1.0f, 1.0f);
                        if (firstPoint.equals(currentPoint)) {
                            break;
                        }
                        if (map.hasIntersections(firstPoint)) {
                            map.moveObjects(firstPoint, currentPoint, firstPoint);
                            map.updateRooms(pScene);
                        } else {
                            map.updateObjects(firstPoint);
                        }
                        break;
                    default:
                        break;
                }
            }

            void addSticker(TouchEvent pSceneTouchEvent) {
                if (pSceneTouchEvent.getAction() != TouchEvent.ACTION_DOWN) {
                    return;
                }
                StickerSprite sticker = new StickerSprite(currentSticker, currentPoint);
                map.addObject(sticker);
                pScene.attachChild(sticker);
                pScene.registerTouchArea(sticker);
                pScene.sortChildren();
            }

            void addLinear(TouchEvent pSceneTouchEvent) {
                switch (pSceneTouchEvent.getAction()) {
                    case TouchEvent.ACTION_DOWN:
                        firstPoint.set(currentPoint);
                        previousPoint.set(currentPoint);
                        currentLine.setPosition(firstPoint.x, firstPoint.y,
                                currentPoint.x, currentPoint.y);
                        switch (item) {
                            case WALL:
                                currentAdded = new WallSprite();
                                break;
                            case DOOR:
                                currentAdded = new DoorSprite();
                                break;
                            case WINDOW:
                                currentAdded = new WindowSprite();
                                break;
                            default:
                                Log.e("VASYOID", "invalid item in addLinear function");
                        }
                        currentAdded.setPosition(currentLine);
                        pScene.attachChild(currentAdded);
                        pScene.registerTouchArea(currentAdded);
                        pScene.sortChildren();
                        break;
                    case TouchEvent.ACTION_MOVE:
                        currentLine.setPosition(firstPoint.x, firstPoint.y,
                                currentPoint.x, currentPoint.y);
                        currentAdded.setPosition(currentLine);
                        previousPoint.set(currentPoint);
                        break;
                    case TouchEvent.ACTION_UP:
                        if (!currentPoint.equals(firstPoint) &&
                                !map.hasIntersections(currentAdded)) {
                            map.addObject(currentAdded);
                            pScene.sortChildren();
                            map.detachRemoved(mEngine);
                        } else {
                            pScene.detachChild(currentAdded);
                        }
                        currentLine.setPosition(0, 0, 0, 0);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public boolean onSceneTouchEvent(final Scene pScene, TouchEvent pSceneTouchEvent) {
                RoomSprite room = null;
                if (pSceneTouchEvent.isActionDown()) {
                    room = map.getRoomTouched(pSceneTouchEvent);
                }
                float currentX = Math.round(pSceneTouchEvent.getX() / GRID_SIZE) * GRID_SIZE;
                float currentY = Math.round(pSceneTouchEvent.getY() / GRID_SIZE) * GRID_SIZE;
                currentX = Math.max(Math.min(currentX, GRID_SIZE * GRID_COLS), 0);
                currentY = Math.max(Math.min(currentY, GRID_SIZE * GRID_ROWS), 0);
                currentPoint.set(currentX, currentY);
                switch (state) {
                    case MOVE_STICKER:
                        break;
                    case MOVE_MAP:
                        moveMap(pSceneTouchEvent);
                        break;
                    case DEL:
                        if (room != null) {
                            map.removeRoom(room);
                        }
                        map.detachRemoved(mEngine);
                        break;
                    case SHOW_PARAMS:
                        showParams(room);
                        break;
                    case CREATE_ROOM:
                        createRoom(room, pSceneTouchEvent);
                        break;
                    case MOVE_WALL:
                        moveWall(pSceneTouchEvent);
                        break;
                    case ADD:
                        if (item == MapItem.STICKER) {
                            addSticker(pSceneTouchEvent);
                        } else {
                            addLinear(pSceneTouchEvent);
                        }
                        break;
                    default:
                        Log.d("VASYOID", "invalid state in onSceneTouchEvent function.");
                }
                return false;
            }
        });
        pScene.setTouchAreaBindingOnActionDownEnabled(true);
        pScene.setTouchAreaBindingOnActionMoveEnabled(true);
        pScene.setOnSceneTouchListenerBindingOnActionDownEnabled(true);
    }

    @Override
    protected Scene onCreateScene() {
        final Scene scene = new Scene();
        scene.setBackground(new Background(0.95f, 0.95f, 1f));
        createGrid(scene);
        initScene(scene);
        initPinchZoomDetector();
        initSprites();
        initMap(scene);
        scene.sortChildren();
        ((ImageView) (findViewById(R.id.imageExit))).setColorFilter(GREEN, PorterDuff.Mode.ADD);
        return scene;
    }

    @Override
    public void onBackPressed() {
        if (toOpenMap == null) {
            Log.d("VASYOID", "map is null!");
        } else {
            Intent intent = new Intent();
            intent.putExtra("toSaveMap", new FloorMap(toOpenMap.getGroupName(),
                    toOpenMap.getBuildingName(), toOpenMap.getName(), map));
            setResult(1, intent);
        }
        super.onBackPressed();
    }

    @Override
    protected int getLayoutID() {
        return R.layout.activity_constructor;
    }

    @Override
    protected int getRenderSurfaceViewID() {
        return R.id.constructorView;
    }

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager != null) {
            View currentFocus = getCurrentFocus();
            if (currentFocus != null) {
                inputManager.hideSoftInputFromWindow(currentFocus.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    public void showParams(RoomSprite pRoom) {
        Semaphore mutex = new Semaphore(1);
        runOnUiThread(() -> {
            findViewById(R.id.constructorView).setEnabled(false);
            View paramsView = findViewById(R.id.roomParamsView);
            paramsView.setVisibility(View.VISIBLE);
            EditText roomName = findViewById(R.id.roomName);
            EditText roomDescription = findViewById(R.id.roomDescription);
            roomName.setText(pRoom.getTitle());
            roomDescription.setText(pRoom.getDescription());
            paramsView.findViewById(R.id.roomParamsOk).setOnClickListener(v1 -> {
                hideKeyboard();
                String title = roomName.getText().toString();
                String description = roomDescription.getText().toString();
                pRoom.setTitle(title);
                pRoom.setDescription(description);
                paramsView.setVisibility(View.GONE);
                findViewById(R.id.constructorView).setEnabled(true);
            });
            mutex.release();
        });
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setSticker(View v) {
        ((ImageView) findViewById(R.id.imageExit)).setColorFilter(TRANSPARENT);
        ((ImageView) findViewById(R.id.imageLift)).setColorFilter(TRANSPARENT);
        ((ImageView) findViewById(R.id.imageStairs)).setColorFilter(TRANSPARENT);
        ((ImageView) findViewById(R.id.imageWC)).setColorFilter(TRANSPARENT);
        ((ImageView) v).setColorFilter(GREEN, PorterDuff.Mode.ADD);
        switch (v.getId()) {
            case R.id.imageExit:
                currentSticker = EXIT;
                break;
            case R.id.imageLift:
                currentSticker = LIFT;
                break;
            case R.id.imageStairs:
                currentSticker = STAIRS;
                break;
            case R.id.imageWC:
                currentSticker = WC;
                break;
            default:
                Log.e("VASYOID", "wrong view id in setSticker function");

        }
    }

    public void setItem(View v) {
        if (v.getId() != R.id.buttonSticker) {
            findViewById(R.id.stickersLayout).setVisibility(View.GONE);
        } else {
            findViewById(R.id.stickersLayout).setVisibility(View.VISIBLE);
        }
        ((Button) findViewById(R.id.buttonDoor)).setTextColor(BLACK);
        ((Button) findViewById(R.id.buttonWall)).setTextColor(BLACK);
        ((Button) findViewById(R.id.buttonWindow)).setTextColor(BLACK);
        ((Button) findViewById(R.id.buttonSticker)).setTextColor(BLACK);
        ((Button) v).setTextColor(RED);
        switch (v.getId()) {
            case R.id.buttonWall:
                item = MapItem.WALL;
                break;
            case R.id.buttonDoor:
                item = MapItem.DOOR;
                break;
            case R.id.buttonWindow:
                item = MapItem.WINDOW;
                break;
            case R.id.buttonSticker:
                item = MapItem.STICKER;
                break;
            default:
                Log.e("VASYOID", "wrong view id setItem function");
        }
    }

    public void setState(View v) {
        if (v.getId() != R.id.buttonAdd) {
            findViewById(R.id.itemsLayout).setVisibility(View.GONE);
        } else {
            findViewById(R.id.itemsLayout).setVisibility(View.VISIBLE);
        }
        ((Button) findViewById(R.id.buttonColor)).setTextColor(BLACK);
        ((Button) findViewById(R.id.buttonDel)).setTextColor(BLACK);
        ((Button) findViewById(R.id.buttonMove)).setTextColor(BLACK);
        ((Button) findViewById(R.id.buttonMoveWall)).setTextColor(BLACK);
        ((Button) findViewById(R.id.buttonAdd)).setTextColor(BLACK);
        ((Button) findViewById(R.id.buttonParams)).setTextColor(BLACK);
        ((Button) findViewById(R.id.buttonMoveSticker)).setTextColor(BLACK);
        ((Button) v).setTextColor(RED);
        switch (v.getId()) {
            case R.id.buttonAdd:
                state = ActionState.ADD;
                break;
            case R.id.buttonDel:
                state = ActionState.DEL;
                break;
            case R.id.buttonMove:
                state = ActionState.MOVE_MAP;
                break;
            case R.id.buttonMoveWall:
                state = ActionState.MOVE_WALL;
                break;
            case R.id.buttonMoveSticker:
                state = ActionState.MOVE_STICKER;
                break;
            case R.id.buttonColor:
                state = ActionState.CREATE_ROOM;
                break;
            case R.id.buttonParams:
                state = ActionState.SHOW_PARAMS;
                break;
            default:
                Log.e("VASYOID", "wrong view id in setState function");
        }
        map.setActionState(state);
    }

    public void clearField(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Clear the map?").setTitle("Clear").setPositiveButton(R.string.ok,
                (dialog, which) -> {
                    map.clear();
                    map.detachRemoved(mEngine);
                }).setNegativeButton(R.string.cancel, (dialog, which) -> {}).create().show();
    }



    public enum ActionState {
        ADD, DEL, MOVE_MAP, MOVE_WALL, MOVE_STICKER, CREATE_ROOM, SHOW_PARAMS
    }

    private enum MapItem {
        WALL, DOOR, WINDOW, STICKER
    }
}
