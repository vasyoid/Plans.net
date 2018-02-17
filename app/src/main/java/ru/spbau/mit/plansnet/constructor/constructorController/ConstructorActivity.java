package ru.spbau.mit.plansnet.constructor.constructorController;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.andengine.entity.primitive.Line;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.background.Background;
import org.andengine.input.touch.TouchEvent;
import org.andengine.entity.scene.Scene;

import ru.spbau.mit.plansnet.R;
import ru.spbau.mit.plansnet.constructor.objects.DoorSprite;
import ru.spbau.mit.plansnet.constructor.Geometry;
import ru.spbau.mit.plansnet.constructor.Map;
import ru.spbau.mit.plansnet.constructor.objects.MapObjectLinear;
import ru.spbau.mit.plansnet.constructor.objects.RoomSprite;
import ru.spbau.mit.plansnet.constructor.objects.StickerSprite;
import ru.spbau.mit.plansnet.constructor.objects.WallSprite;
import ru.spbau.mit.plansnet.constructor.objects.WindowSprite;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;
import static android.graphics.Color.TRANSPARENT;

import static ru.spbau.mit.plansnet.constructor.objects.StickerSprite.StickerType.EXIT;
import static ru.spbau.mit.plansnet.constructor.objects.StickerSprite.StickerType.FIRE;
import static ru.spbau.mit.plansnet.constructor.objects.StickerSprite.StickerType.LIFT;
import static ru.spbau.mit.plansnet.constructor.objects.StickerSprite.StickerType.SMOKE;
import static ru.spbau.mit.plansnet.constructor.objects.StickerSprite.StickerType.STAIRS;
import static ru.spbau.mit.plansnet.constructor.objects.StickerSprite.StickerType.VOLTAGE;
import static ru.spbau.mit.plansnet.constructor.objects.StickerSprite.StickerType.WC;

/**
 * Activity for plans editor.
 * Displays a plan with all elements and provides an interface to add, move, remove elements, etc.
 */
public class ConstructorActivity extends BaseConstructorActivity {

    private static final int PICK_IMAGE_TOKEN = 42;

    private ActionState mState = ActionState.ADD;
    private MapItem mItem = MapItem.WALL;
    private StickerSprite.StickerType mCurrentSticker = EXIT;
    private List<Line> mGrid = new LinkedList<>();

    /**
     * Removes an existing grid from the current scene.
     */
    private void removeGrid() {
        Semaphore mutex = new Semaphore(0);
        getEngine().runOnUpdateThread(() -> {
            for (Line l : mGrid) {
                l.detachSelf();
            }
            mGrid.clear();
            mutex.release();
        });
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Creates a grid on a scene.
     * @param pScene scene where to draw the grid.
     */
    private void createGrid(@NonNull Scene pScene) {
        for (int i = 0; i <= MAP_WIDTH; i += mGridSize) {
            Line line = new Line(i, 0, i, MAP_HEIGHT, 3,
                    getVertexBufferObjectManager());
            line.setColor(0.7f, 0.7f, 0.7f);
            line.setZIndex(-1);
            mGrid.add(line);
            pScene.attachChild(line);
        }
        for (int i = 0; i <= MAP_HEIGHT; i += mGridSize) {
            Line line = new Line(0, i, MAP_WIDTH, i, 3,
                    getVertexBufferObjectManager());
            line.setColor(0.7f, 0.7f, 0.7f);
            line.setZIndex(-1);
            mGrid.add(line);
            pScene.attachChild(line);
        }
        pScene.sortChildren();
    }

    /**
     * Sets onSceneTouchListener to a scene.
     * @param pScene scene which the listener is set to.
     */
    private void initScene(@NonNull Scene pScene) {
        pScene.setOnSceneTouchListener(new IOnSceneTouchListener() {

            private PointF mFirstPoint = new PointF();
            private PointF mCurrentPoint = new PointF();
            private PointF mPreviousPoint = new PointF();
            private Line mCurrentLine = new Line(0, 0, 0, 0,
                    getVertexBufferObjectManager());
            private MapObjectLinear mCurrentAdded;

            /**
             * Creates a new room at the touched point if is does not already exist.
             * @param pTouchedRoom if not null then no room will be created.
             * @param pSceneTouchEvent touch event containing the touched point.
             */
            public void createRoom(@Nullable RoomSprite pTouchedRoom,
                                   @NonNull TouchEvent pSceneTouchEvent) {
                if (!pSceneTouchEvent.isActionDown() || pTouchedRoom != null) {
                    return;
                }
                RoomSprite currentRoom = mMap.createRoomOrNull(pSceneTouchEvent.getX(),
                        pSceneTouchEvent.getY(), pScene);
                if (currentRoom != null) {
                    showParams(currentRoom);
                }
            }

            /**
             * Handles touch events corresponding to wall corner shifts.
             * @param pSceneTouchEvent touch event to handle.
             */
            private void moveWall(@NonNull TouchEvent pSceneTouchEvent) {
                switch (pSceneTouchEvent.getAction()) {
                    case TouchEvent.ACTION_DOWN:
                        PointF nearestWall = mMap.getNearestWallOrNull(mCurrentPoint);
                        if (nearestWall == null) {
                            mCurrentPoint.set(-1, -1);
                        } else {
                            mCurrentPoint.set(nearestWall);
                        }
                        mFirstPoint.set(mCurrentPoint);
                        mPreviousPoint.set(mCurrentPoint);
                        mMap.setScaleByPoint(mCurrentPoint, 1.0f, 1.4f);
                        break;
                    case TouchEvent.ACTION_MOVE:
                        if (mPreviousPoint.equals(mCurrentPoint)) {
                            break;
                        }
                        mMap.moveObjects(mFirstPoint, mCurrentPoint);
                        try {
                            mMap.updateRooms(pScene);
                        } catch (com.earcutj.exception.EarcutException ignored) {}
                        mPreviousPoint.set(mCurrentPoint);
                        break;
                    case TouchEvent.ACTION_UP:
                        mMap.detachRemoved(getEngine());
                        if (mFirstPoint == null) {
                            break;
                        }
                        mMap.setScaleByPoint(mFirstPoint, 1.0f, 1.0f);
                        if (mFirstPoint.equals(mCurrentPoint)) {
                            break;
                        }
                        mCurrentPoint.set(Math.round(mCurrentPoint.x / mGridSize) * mGridSize,
                                Math.round(mCurrentPoint.y / mGridSize) * mGridSize);
                        if (mMap.hasIntersections(mFirstPoint)) {
                            mMap.moveObjects(mFirstPoint, mFirstPoint);
                            mMap.updateRooms(pScene);
                        } else {
                            mMap.updateObjects(mFirstPoint);
                        }
                        break;
                    default:
                        break;
                }
            }

            /**
             * Adds a new sticker to the map.
             * @param pSceneTouchEvent point where to put the sticker.
             */
            public void addSticker(@NonNull TouchEvent pSceneTouchEvent) {
                if (pSceneTouchEvent.getAction() != TouchEvent.ACTION_DOWN) {
                    return;
                }
                StickerSprite sticker = new StickerSprite(mCurrentSticker, mCurrentPoint);
                mMap.addObject(sticker);
                pScene.attachChild(sticker);
                pScene.registerTouchArea(sticker);
                pScene.sortChildren();
            }

            /**
             * Adds a new linear object (wall, door or window) to the map.
             * @param pSceneTouchEvent point where to put the sticker.
             */
            public void addLinear(@NonNull TouchEvent pSceneTouchEvent) {
                switch (pSceneTouchEvent.getAction()) {
                    case TouchEvent.ACTION_DOWN:
                        mFirstPoint.set(mCurrentPoint);
                        mPreviousPoint.set(mCurrentPoint);
                        mCurrentLine.setPosition(mFirstPoint.x, mFirstPoint.y,
                                mCurrentPoint.x, mCurrentPoint.y);
                        switch (mItem) {
                            case WALL:
                                mCurrentAdded = new WallSprite();
                                break;
                            case DOOR:
                                mCurrentAdded = new DoorSprite();
                                break;
                            case WINDOW:
                                mCurrentAdded = new WindowSprite();
                                break;
                            default:
                                Log.e("VASYOID", "invalid item in addLinear function");
                        }
                        mCurrentAdded.setObjectPosition(mCurrentLine);
                        pScene.attachChild(mCurrentAdded);
                        pScene.registerTouchArea(mCurrentAdded);
                        pScene.sortChildren();
                        break;
                    case TouchEvent.ACTION_MOVE:
                        mCurrentLine.setPosition(mFirstPoint.x, mFirstPoint.y,
                                mCurrentPoint.x, mCurrentPoint.y);
                        mCurrentAdded.setObjectPosition(mCurrentLine);
                        mPreviousPoint.set(mCurrentPoint);
                        break;
                    case TouchEvent.ACTION_UP:
                        if (!mCurrentPoint.equals(mFirstPoint) &&
                                !mMap.hasIntersections(mCurrentAdded)) {
                            mMap.addObject(mCurrentAdded);
                            pScene.sortChildren();
                            mMap.detachRemoved(getEngine());
                        } else {
                            pScene.unregisterTouchArea(mCurrentAdded);
                            pScene.detachChild(mCurrentAdded);
                        }
                        mCurrentLine.setPosition(0, 0, 0, 0);
                        break;
                    default:
                        break;
                }
            }

            /**
             *
             * @param pScene The scene that the touch event has been dispatched to.
             * @param pSceneTouchEvent The touch event
             *                         object containing full information about the event.
             *
             * @return true if the touch was accepted, false otherwise.
             */
            @Override
            public boolean onSceneTouchEvent(final Scene pScene, TouchEvent pSceneTouchEvent) {
                RoomSprite room = null;
                if (pSceneTouchEvent.isActionDown()) {
                    room = mMap.getRoomTouchedOrNull(pSceneTouchEvent);
                }
                float currentX = pSceneTouchEvent.getX();
                float currentY = pSceneTouchEvent.getY();
                if (mState == ActionState.MOVE_OBJECT && !pSceneTouchEvent.isActionMove()) {
                    currentX = Math.round(currentX / GRID_SIZE_MIN) * GRID_SIZE_MIN;
                    currentY = Math.round(currentY / GRID_SIZE_MIN) * GRID_SIZE_MIN;
                } else {
                    currentX = Math.round(currentX / mGridSize) * mGridSize;
                    currentY = Math.round(currentY / mGridSize) * mGridSize;
                }
                currentX = Geometry.bringValueToBounds(currentX, 0, MAP_WIDTH);
                currentY = Geometry.bringValueToBounds(currentY, 0, MAP_HEIGHT);
                currentY = Math.max(Math.min(currentY, MAP_HEIGHT), 0);
                mCurrentPoint.set(currentX, currentY);
                switch (mState) {
                    case MOVE_MAP:
                        moveMap(pSceneTouchEvent);
                        break;
                    case DEL:
                        if (room != null) {
                            mMap.removeRoom(room);
                        }
                        mMap.detachRemoved(mEngine);
                        break;
                    case SHOW_PARAMS:
                        if (room == null) {
                            break;
                        }
                        showParams(room);
                        break;
                    case CREATE_ROOM:
                        createRoom(room, pSceneTouchEvent);
                        break;
                    case MOVE_OBJECT:
                        moveWall(pSceneTouchEvent);
                        break;
                    case ADD:
                        if (mItem == MapItem.STICKER) {
                            addSticker(pSceneTouchEvent);
                        } else {
                            addLinear(pSceneTouchEvent);
                        }
                        break;
                    default:
                        Log.e("VASYOID", "invalid state in onSceneTouchEvent function.");
                }
                return false;
            }
        });
        pScene.setTouchAreaBindingOnActionDownEnabled(true);
        pScene.setTouchAreaBindingOnActionMoveEnabled(true);
        pScene.setOnSceneTouchListenerBindingOnActionDownEnabled(true);
    }

    /**
     * Creates a new scene and initializes classes that will be placed on the scene.
     * @return created scene.
     */
    @Override
    protected @NonNull Scene onCreateScene() {
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

    /**
     * Callback function called when a user presses the Back button.
     */
    @Override
    public void onBackPressed() {
        if (mToOpenMap == null) {
            Log.e("VASYOID", "map is null!");
        } else {
            Intent intent = new Intent();
            intent.putExtra("toSaveMap", mToOpenMap.addObjectsFromMap(mMap));
            setResult(1, intent);
        }
        super.onBackPressed();
    }

    /**
     * Layout ID getter.
     * @return layout ID.
     */
    @Override
    protected int getLayoutID() {
        return R.layout.activity_constructor;
    }

    /**
     * Surface view ID getter.
     * @return surface view ID.
     */
    @Override
    protected int getRenderSurfaceViewID() {
        return R.id.constructorView;
    }

    /**
     * hides the screen keyboard after a user finishes typing.
     */
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

    /**
     * Shows room information (name, description).
     * @param pRoom room which information will be shown.
     */
    public void showParams(@NonNull RoomSprite pRoom) {
        if (!findViewById(R.id.constructorView).isEnabled()) {
            return;
        }
        Semaphore mutex = new Semaphore(0);
        runOnUiThread(() -> {
            disableAll();
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
                enableAll();
            });
            mutex.release();
        });
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets current sticker depending on a button pressed by a user.
     * @param pView button pressed by the user.
     */
    public void setSticker(@NonNull View pView) {
        if (!findViewById(R.id.constructorView).isEnabled()) {
            return;
        }
        ((ImageView) findViewById(R.id.imageExit)).setColorFilter(TRANSPARENT);
        ((ImageView) findViewById(R.id.imageLift)).setColorFilter(TRANSPARENT);
        ((ImageView) findViewById(R.id.imageStairs)).setColorFilter(TRANSPARENT);
        ((ImageView) findViewById(R.id.imageWC)).setColorFilter(TRANSPARENT);
        ((ImageView) findViewById(R.id.imageFire)).setColorFilter(TRANSPARENT);
        ((ImageView) findViewById(R.id.imageSmoke)).setColorFilter(TRANSPARENT);
        ((ImageView) findViewById(R.id.imageVoltage)).setColorFilter(TRANSPARENT);
        ((ImageView) pView).setColorFilter(GREEN, PorterDuff.Mode.ADD);
        switch (pView.getId()) {
            case R.id.imageExit:
                mCurrentSticker = EXIT;
                break;
            case R.id.imageLift:
                mCurrentSticker = LIFT;
                break;
            case R.id.imageStairs:
                mCurrentSticker = STAIRS;
                break;
            case R.id.imageWC:
                mCurrentSticker = WC;
                break;
            case R.id.imageFire:
                mCurrentSticker = FIRE;
                break;
            case R.id.imageSmoke:
                mCurrentSticker = SMOKE;
                break;
            case R.id.imageVoltage:
                mCurrentSticker = VOLTAGE;
                break;
            default:
                Log.e("VASYOID", "wrong view id in setSticker function");
        }
    }

    /**
     * Sets current item (wall, door, window or sticker) depending on a button pressed by a user.
     * @param pView button pressed by the user.
     */
    public void setItem(@NonNull View pView) {
        if (!findViewById(R.id.constructorView).isEnabled()) {
            return;
        }
        if (pView.getId() != R.id.buttonSticker) {
            findViewById(R.id.stickersLayout).setVisibility(View.GONE);
        } else {
            findViewById(R.id.stickersLayout).setVisibility(View.VISIBLE);
        }
        ((Button) findViewById(R.id.buttonDoor)).setTextColor(BLACK);
        ((Button) findViewById(R.id.buttonWall)).setTextColor(BLACK);
        ((Button) findViewById(R.id.buttonWindow)).setTextColor(BLACK);
        ((Button) findViewById(R.id.buttonSticker)).setTextColor(BLACK);
        ((Button) pView).setTextColor(RED);
        switch (pView.getId()) {
            case R.id.buttonWall:
                mItem = MapItem.WALL;
                break;
            case R.id.buttonDoor:
                mItem = MapItem.DOOR;
                break;
            case R.id.buttonWindow:
                mItem = MapItem.WINDOW;
                break;
            case R.id.buttonSticker:
                mItem = MapItem.STICKER;
                break;
            default:
                Log.e("VASYOID", "wrong view id setItem function");
        }
    }

    /**
     * Sets current touch state (add, remove, etc) depending on a button pressed by a user.
     * @param pView button pressed by user.
     */
    public void setState(@NonNull View pView) {
        if (!findViewById(R.id.constructorView).isEnabled()) {
            return;
        }
        if (pView.getId() != R.id.buttonAdd) {
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
        ((Button) pView).setTextColor(RED);
        switch (pView.getId()) {
            case R.id.buttonAdd:
                mState = ActionState.ADD;
                break;
            case R.id.buttonDel:
                mState = ActionState.DEL;
                break;
            case R.id.buttonMove:
                mState = ActionState.MOVE_MAP;
                break;
            case R.id.buttonMoveWall:
                mState = ActionState.MOVE_OBJECT;
                break;
            case R.id.buttonColor:
                mState = ActionState.CREATE_ROOM;
                break;
            case R.id.buttonParams:
                mState = ActionState.SHOW_PARAMS;
                break;
            default:
                Log.e("VASYOID", "wrong view id in setState function");
        }
        mMap.setActionState(mState);
    }

    /**
     * Disables all interface elements and darkens them.
     */
    private void disableAll() {
        View darkener = findViewById(R.id.darkenerRect);
        darkener.animate().alpha(0.5f);
        findViewById(R.id.constructorView).setEnabled(false);
    }

    /**
     * Enables all interface elements.
     */
    private void enableAll() {
        View darkener = findViewById(R.id.darkenerRect);
        darkener.animate().alpha(0);
        findViewById(R.id.constructorView).setEnabled(true);
    }

    /**
     * Removes all elements from the map.
     * @param pView button pressed.
     */
    public void clearMap(@NonNull View pView) {
        if (!findViewById(R.id.constructorView).isEnabled()) {
            return;
        }
        Semaphore mutex = new Semaphore(0);
        runOnUiThread(() -> {
            disableAll();
            View confirmClearView = findViewById(R.id.confirmClearView);
            confirmClearView.setVisibility(View.VISIBLE);
            confirmClearView.findViewById(R.id.confirmClearOk).setOnClickListener(v1 -> {
                mMap.clear();
                mMap.detachRemoved(getEngine());
                confirmClearView.setVisibility(View.GONE);
                enableAll();
            });
            confirmClearView.findViewById(R.id.confirmClearCancel).setOnClickListener(v1 -> {
                confirmClearView.setVisibility(View.GONE);
                enableAll();
            });
            mutex.release();
        });
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param pUri uri for the activity.
     * @return A bitmap of the chosen image.
     */
    private @Nullable Bitmap readImage(@NonNull Uri pUri) {
        BitmapFactory.Options options;
        try (InputStream imageStream = getContentResolver().openInputStream(pUri)) {
            options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(imageStream, null, options);
        } catch (IOException e) {
            return null;
        }
        int scale = Math.max(options.outHeight / MAP_HEIGHT * 4, options.outWidth / MAP_WIDTH * 4);
        try (InputStream imageStream = getContentResolver().openInputStream(pUri)) {
            options = new BitmapFactory.Options();
            options.inSampleSize = scale;
            return BitmapFactory.decodeStream(imageStream, null, options);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Gets the image chosen by a user and sets it as a background of the map.
     * @param pRequestCode activity request code.
     * @param pResultCode activity result code.
     * @param pData the
     */
    @Override
    protected void onActivityResult(int pRequestCode, int pResultCode, @NonNull Intent pData) {
        switch (pRequestCode) {
            case PICK_IMAGE_TOKEN:
                if (pResultCode != RESULT_OK) {
                    break;
                }
                Uri imageUri = pData.getData();
                if (imageUri == null) {
                    break;
                }
                ((Button) findViewById(R.id.addBackground)).setText(R.string.delBkgnd);
                Bitmap image = readImage(imageUri);
                if (image == null) {
                    break;
                }
                mMap.setBackground(image, getEngine());
        }

    }

    /**
     * Calls a system activity to choose an image file from the device storage.,
     * @param pView button pressed
     */
    public void setBackground(@NonNull View pView) {
        if (!findViewById(R.id.constructorView).isEnabled()) {
            return;
        }
        if (!mMap.isBackgroundSet()) {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, PICK_IMAGE_TOKEN);
        } else {
            ((Button) pView).setText(R.string.addBkgnd);
            mMap.removeBackground(getEngine());
        }
    }

    /**
     * Shows an interface to change the size of the grid.
     * @param pView button pressed.
     */
    public void setGridSize(@NonNull View pView) {
        if (!findViewById(R.id.constructorView).isEnabled()) {
            return;
        }
        Semaphore mutex = new Semaphore(0);
        runOnUiThread(() -> {
            disableAll();
            View gridSizeView = findViewById(R.id.gridSizeView);
            gridSizeView.setVisibility(View.VISIBLE);
            SeekBar sizeSeekBar = gridSizeView.findViewById(R.id.sizeSeekBar);
            sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mGridSize = GRID_SIZE_MIN << progress;
                    Map.setGridSize(mGridSize);
                    removeGrid();
                    createGrid(getEngine().getScene());
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}

            });
            gridSizeView.findViewById(R.id.gridSizeOk).setOnClickListener(v1 -> {
                gridSizeView.setVisibility(View.GONE);
                enableAll();
            });
            mutex.release();
        });
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * State that determines how to handle touch events.
     */
    public enum ActionState {
        ADD, DEL, MOVE_MAP, MOVE_OBJECT, CREATE_ROOM, SHOW_PARAMS
    }

    /**
     * Current item (object) that will be added to the map next.
     */
    private enum MapItem {
        WALL, DOOR, WINDOW, STICKER
    }

}
