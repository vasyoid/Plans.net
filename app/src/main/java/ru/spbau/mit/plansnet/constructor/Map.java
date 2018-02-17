package ru.spbau.mit.plansnet.constructor;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.andengine.engine.Engine;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import ru.spbau.mit.plansnet.constructor.objects.DoorSprite;
import ru.spbau.mit.plansnet.constructor.objects.MapObjectLinear;
import ru.spbau.mit.plansnet.constructor.objects.MapObjectSprite;
import ru.spbau.mit.plansnet.constructor.objects.RoomSprite;
import ru.spbau.mit.plansnet.constructor.objects.StickerSprite;
import ru.spbau.mit.plansnet.constructor.objects.WallSprite;
import ru.spbau.mit.plansnet.constructor.objects.WindowSprite;
import ru.spbau.mit.plansnet.constructor.constructorController.ConstructorActivity;
import ru.spbau.mit.plansnet.constructor.constructorController.ConstructorActivity.ActionState;
import ru.spbau.mit.plansnet.data.FloorMap;
import ru.spbau.mit.plansnet.data.objects.Door;
import ru.spbau.mit.plansnet.data.objects.MapObject;
import ru.spbau.mit.plansnet.data.objects.Room;
import ru.spbau.mit.plansnet.data.objects.Sticker;
import ru.spbau.mit.plansnet.data.objects.Wall;
import ru.spbau.mit.plansnet.data.objects.Window;

import static ru.spbau.mit.plansnet.constructor.constructorController.ConstructorActivity.GRID_SIZE_MIN;
import static ru.spbau.mit.plansnet.constructor.constructorController.ConstructorActivity.MAP_HEIGHT;
import static ru.spbau.mit.plansnet.constructor.constructorController.ConstructorActivity.MAP_WIDTH;
import static ru.spbau.mit.plansnet.constructor.constructorController.ConstructorActivity.ActionState.ADD;

/**
 * Map is a container of all plan elements with relevant methods
 * that help an activity interact with the elements.
 */
public class Map implements Serializable {

    private static int gridSize = 0;

    private List<MapObjectSprite> mObjects = new LinkedList<>();
    private List<RoomSprite> mRooms = new LinkedList<>();
    private List<MapObjectSprite> mRemovedObjects = new LinkedList<>();
    private List<RoomSprite> mRemovedRooms = new LinkedList<>();
    private HashMap<PointF, HashSet<MapObjectLinear>> mLinearObjectsByCell = new HashMap<>();
    private ConstructorActivity.ActionState mTouchState = ADD;
    private Sprite mBackgroundSprite = null;

    /**
     * Void constructor.
     */
    public Map() { }

    /**
     * Constructor that takes a data.FloorMap object.
     * This is used to open maps that were loaded from the server.
     * @param pMap Floor map object containing all the information about plan objects.
     * @param pScene scene where all the objects will be drawn.
     */
    public Map(@NonNull FloorMap pMap, @NonNull  Scene pScene) {
        for (MapObject o : pMap.getArrayData()) {
            if (o instanceof Door) {
                addObject(new DoorSprite((Door) o));
            } else if (o instanceof Wall) {
                addObject(new WallSprite((Wall) o));
            } else if (o instanceof Window) {
                addObject(new WindowSprite((Window) o));
            } else if (o instanceof Sticker) {
                addObject(new StickerSprite((Sticker) o));
            } else if (o instanceof Room) {
                Room room = (Room) o;
                RoomSprite roomSprite = createRoomOrNull(room.getX(), room.getY(), pScene);
                if (roomSprite == null) {
                    continue;
                }
                roomSprite.setTitle(room.getTitle().toString());
                roomSprite.setDescription(room.getDescription().toString());
                roomSprite.setColor(room.getColor());
            }
        }
    }

    /**
     * Returns a polygon representing the map metrics.
     * @return grid polygon.
     */
    public static @NonNull List<PointF> getGridPolygon() {
        List<PointF> result = new ArrayList<>();
        result.add(new PointF(-1.0f, -1.0f));
        result.add(new PointF(-1.0f, MAP_HEIGHT + 1.0f));
        result.add(new PointF(MAP_WIDTH + 1.0f, MAP_HEIGHT + 1.0f));
        result.add(new PointF(MAP_WIDTH + 1.0f, -1.0f));
        return result;
    }

    /**
     * Grid size getter.
     * @return grid size.
     */
    public static int getGridSize() {
        return gridSize;
    }

    /**
     * Grid size setter.
     * @param pSize grid size.
     */
    public static void setGridSize(int pSize) {
        gridSize = pSize;
    }

    /**
     * Takes a bitmap and places it on the scene under the grid.
     * @param pBackground bitmap to draw
     * @param pEngine engine that helps to visualize the bitmap.
     */
    public void setBackground(@NonNull Bitmap pBackground, @NonNull Engine pEngine) {
        float ratio = Math.max((float) MAP_WIDTH / pBackground.getWidth(),
                (float) MAP_HEIGHT / pBackground.getHeight());
        if (ratio < 1) {
            pBackground = Bitmap.createScaledBitmap(pBackground,
                    (int) (pBackground.getWidth() * ratio),
                    (int) (pBackground.getHeight() * ratio), false);
            ratio = 1;
        }
        Rect area = new Rect(0, 0, (int) (MAP_WIDTH / ratio), (int) (MAP_HEIGHT / ratio));
        area.offset((int) (pBackground.getWidth() - MAP_WIDTH / ratio) / 2,
                (int) (pBackground.getHeight() - MAP_HEIGHT / ratio) / 2);
        BitmapTextureAtlasSource source = new BitmapTextureAtlasSource(pBackground, area);
        BitmapTextureAtlas textureAtlas = new BitmapTextureAtlas(pEngine.getTextureManager(),
                area.width(), area.height());
        textureAtlas.addTextureAtlasSource(source, 0, 0);
        textureAtlas.load();
        TextureRegion backgroundTexture = TextureRegionFactory.createFromSource(textureAtlas,
                source, 0, 0);
        mBackgroundSprite = new Sprite(0, 0, backgroundTexture,
                pEngine.getVertexBufferObjectManager());
        mBackgroundSprite.setPosition((MAP_WIDTH - mBackgroundSprite.getWidth()) / 2,
                (MAP_HEIGHT - mBackgroundSprite.getHeight()) / 2);
        mBackgroundSprite.setScale(ratio);
        mBackgroundSprite.setZIndex(-3);
        pEngine.getScene().attachChild(mBackgroundSprite);
        pEngine.getScene().sortChildren();
    }

    /**
     * Says if a background is present.
     * @return true if the map has a background, false otherwise.
     */
    public boolean isBackgroundSet() {
        return mBackgroundSprite != null;
    }

    /**
     * Removes the background from the map.
     * @param pEngine engine that helps to visualize the background.
     */
    public void removeBackground(@NonNull Engine pEngine) {
        Semaphore mutex = new Semaphore(0);
        pEngine.runOnUpdateThread(() -> {
            mBackgroundSprite.detachSelf();
            mutex.release();
        });
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBackgroundSprite = null;
    }

    /**
     * Action state setter.
     * @param pState action state.
     */
    public void setActionState(@NonNull ActionState pState) {
        mTouchState = pState;
    }

    /**
     * Action state getter.
     * return action state.
     */
    public @NonNull ActionState getTouchState() {
        return mTouchState;
    }

    /**
     * Objects getter.
     * @return list of objects.
     */
    public @NonNull List<MapObjectSprite> getObjects() {
        return mObjects;
    }

    /**
     * Rooms getter.
     * @return list of rooms.
     */
    public @NonNull List<RoomSprite> getRooms() {
        return mRooms;
    }

    /**
     * Adds a linear object to the hash table.
     * @param pPoint a key for the hash table -- one of the object's ends.
     * @param pObject object to add.
     */
    private void addObjectToHashTable(@NonNull PointF pPoint, @NonNull MapObjectLinear pObject) {
        PointF key = new PointF(pPoint.x, pPoint.y);
        if (!mLinearObjectsByCell.containsKey(pPoint)) {
            mLinearObjectsByCell.put(key, new HashSet<>());
        }
        mLinearObjectsByCell.get(pPoint).add(pObject);
    }

    /**
     * Removes a linear object from the hash table.
     * @param pPoint a key for the hash table -- one of the object's ends.
     * @param pObject object to remove.
     */
    private void removeObjectFromHashTable(@NonNull PointF pPoint,
                                           @NonNull MapObjectLinear pObject) {
        if (mLinearObjectsByCell.containsKey(pPoint)) {
            mLinearObjectsByCell.get(pPoint).remove(pObject);
        }
    }

    /**
     * Updates a linear object in the hash table according to the object's new position.
     * @param pFirstPoint1 previous position of the object's first end.
     * @param pFirstPoint2 previous position of the object's second end.
     */
    public void updateMovedObject(@NonNull PointF pFirstPoint1,
                                  @NonNull PointF pFirstPoint2,
                                  @NonNull MapObjectLinear pObject) {
        removeObjectFromHashTable(pFirstPoint1, pObject);
        removeObjectFromHashTable(pFirstPoint2, pObject);
        addObjectToHashTable(pObject.getPoint1(), pObject);
        addObjectToHashTable(pObject.getPoint2(), pObject);
    }

    /**
     * Sets a given scale to all linear objects ending at a given point.
     * @param pAt point of objects.
     * @param pSx x scale factor.
     * @param pSy y scale factor.
     */
    @SuppressWarnings("SameParameterValue")
    public void setScaleByPoint(PointF pAt, float pSx, float pSy) {
        if (!mLinearObjectsByCell.containsKey(pAt)) {
            return;
        }
        for (MapObjectLinear object : mLinearObjectsByCell.get(pAt)) {
            object.setScaleCenter(object.getWidth() / 2,
                    object.getHeight() / 2);
            object.setScale(pSx, pSy);
        }
    }

    /**
     * Updates a linear object in the hash table.
     * @param pFrom previous key in the hash table.
     * @param pTo new key in the hash table.
     */
    public void moveObjects(@NonNull PointF pFrom, @NonNull PointF pTo) {
        if (!mLinearObjectsByCell.containsKey(pFrom)) {
            return;
        }
        for (MapObjectLinear object : mLinearObjectsByCell.get(pFrom)) {
            if (object.getPoint1().equals(pFrom)) {
                object.changeDirection();
            }
            object.setPoint2(pTo);
        }
    }

    /**
     * Make all rooms update their shapes.
     * @param pScene scene where the rooms are drawn.
     */
    public void updateRooms(@NonNull Scene pScene) {
        for (RoomSprite room : mRooms) {
            room.detachSelf();
            room.updateShape();
            room.attachSelf(pScene);
        }
        pScene.sortChildren();
    }

    /**
     * Updates a linear object in the hash table according to their real positions.
     * @param pAt point of the objects.
     */
    public void updateObjects(@NonNull PointF pAt) {
        if (!mLinearObjectsByCell.containsKey(pAt)) {
            return;
        }
        List<MapObjectLinear> tmp = new ArrayList<>(mLinearObjectsByCell.get(pAt));
        mLinearObjectsByCell.get(pAt).clear();
        for (MapObjectLinear object : tmp) {
            addObjectToHashTable(object.getPoint1(), object);
            addObjectToHashTable(object.getPoint2(), object);
        }
    }

    /**
     * Adds a new map object to the list of objects.
     * @param pObject object to add.
     */
    public void addObject(@NonNull MapObjectSprite pObject) {
        mRemovedObjects.remove(pObject);
        mObjects.add(pObject);
        if (pObject instanceof MapObjectLinear) {
            MapObjectLinear objectLinear = (MapObjectLinear) pObject;
            addObjectToHashTable(objectLinear.getPoint1(), objectLinear);
            addObjectToHashTable(objectLinear.getPoint2(), objectLinear);
        }
    }

    /**
     * Adds a new room to the list of rooms.
     * @param pRoom room to add.
     */
    public void addRoom(@NonNull RoomSprite pRoom) {
        mRemovedRooms.remove(pRoom);
        mRooms.add(pRoom);
    }

    /**
     * Finds all rooms containing a given segment as a side.
     * @param pPoint1 first segment point.
     * @param pPoint2 second segment point.
     * @return list of found rooms.
     */
    public @NonNull List<RoomSprite> findRoomsBySection(@NonNull PointF pPoint1,
                                                        @NonNull PointF pPoint2) {
        List<RoomSprite> result = new ArrayList<>();
        for (RoomSprite room : mRooms) {
            if (room.contains(pPoint1, pPoint2)) {
                result.add(room);
            }
        }
        return result;
    }

    /**
     * Removes all rooms containing a given segment as a side.
     * @param pPoint1 first segment point.
     * @param pPoint2 second segment point.
     */
    public void removeRoomsBySection(@NonNull PointF pPoint1, @NonNull PointF pPoint2) {
        for (RoomSprite room : findRoomsBySection(pPoint1, pPoint2)) {
            removeRoom(room);
        }
    }

    /**
     * Removes a map object from the list of objects.
     * @param pObject object to remove.
     */
    public void removeObject(@NonNull MapObjectSprite pObject) {
        mObjects.remove(pObject);
        if (pObject instanceof MapObjectLinear) {
            MapObjectLinear objectLinear = (MapObjectLinear) pObject;
            if (mLinearObjectsByCell.get(objectLinear.getPoint1()).contains(pObject)) {
                mLinearObjectsByCell.get(objectLinear.getPoint1()).remove(pObject);
            }
            if (mLinearObjectsByCell.get(objectLinear.getPoint2()).contains(pObject)) {
                mLinearObjectsByCell.get(objectLinear.getPoint2()).remove(pObject);
            }
        }
        mRemovedObjects.add(pObject);
    }

    /**
     * Removes a room from the list of rooms.
     * @param pRoom room to remove.
     */
    public void removeRoom(@NonNull RoomSprite pRoom) {
        mRooms.remove(pRoom);
        mRemovedRooms.add(pRoom);
    }

    /**
     * Detaches all removed objects from the scene.
     * @param pEngine engine containing scene.
     */
    public void detachRemoved(@NonNull Engine pEngine) {
        if (mRemovedObjects.isEmpty() && mRemovedRooms.isEmpty()) {
            return;
        }
        Semaphore mutex = new Semaphore(1);
        pEngine.runOnUpdateThread(() -> {
            for (MapObjectSprite o : mRemovedObjects) {
                o.detachSelf();
            }
            for (RoomSprite r : mRemovedRooms) {
                r.detachSelf();
            }
            mRemovedObjects.clear();
            mRemovedRooms.clear();
            mutex.release();
        });
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes all elements from the map.
     */
    public void clear() {
        mRemovedObjects.addAll(mObjects);
        mRemovedRooms.addAll(mRooms);
        mObjects.clear();
        mRooms.clear();
        mLinearObjectsByCell.clear();
    }

    /**
     * Says if any of the linear objects ending in a given point has an intersection
     * with any other linear object.
     * @param pPoint position of objects.
     * @return true if there is at least one intersection, false otherwise.
     */
    public boolean hasIntersections(@NonNull PointF pPoint) {
        if (!mLinearObjectsByCell.containsKey(pPoint)) {
            return false;
        }
        for (MapObjectLinear object : mLinearObjectsByCell.get(pPoint)) {
            if (hasIntersections(object) || Geometry.length(object.getPosition()) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Says if a linear given object has an intersection
     * with any other linear object.
     * @param pObject object to check.
     * @return true if there is at least one intersection, false otherwise.
     */
    public boolean hasIntersections(@NonNull MapObjectLinear pObject) {
        for (MapObjectSprite o : mObjects) {
            if (!(o instanceof MapObjectLinear)) {
                continue;
            }
            MapObjectLinear ol = (MapObjectLinear) o;
            if (ol.equals(pObject)) {
                continue;
            }
            if (Geometry.linesIntersect(ol.getPosition(), pObject.getPosition())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a room that was touched with a given touch event if there is any.
     * @param pTouchEvent touch event.
     * @return the touched room if it exists, null otherwise.
     */
    public @Nullable RoomSprite getRoomTouchedOrNull(@NonNull TouchEvent pTouchEvent) {
        PointF touchPoint = new PointF(pTouchEvent.getX(), pTouchEvent.getY());
        for (RoomSprite r : mRooms) {
            if (Geometry.isPointInsidePolygon(r.getPolygon(), touchPoint)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Creates a room at a given position if it is possible.
     * @param pX x position of the room.
     * @param pY y position of the room.
     * @param pScene scene to draw the room on.
     * @return new room if created successfully, null otherwise.
     */
    public @Nullable RoomSprite createRoomOrNull(float pX, float pY, @NonNull Scene pScene) {
        List<PointF> polygon = Geometry.roomPolygonOrNull(mObjects, new PointF(pX, pY));
        if (polygon == null || !Geometry.isPointInsidePolygon(polygon, new PointF(pX, pY))) {
            return null;
        }
        RoomSprite room = new RoomSprite(polygon);
        addRoom(room);
        room.attachSelf(pScene);
        pScene.sortChildren();
        return room;
    }

    /**
     * Returns a linear object nearest to a given point if there is any.
     * @param pCurrentPoint point to check.
     * @return the nearest linear object if it exists, null otherwise.
     */
    public @Nullable PointF getNearestWallOrNull(@NonNull PointF pCurrentPoint) {
        int nearestDist = 3 * GRID_SIZE_MIN;
        PointF result = null;
        for (int i = -GRID_SIZE_MIN; i <= GRID_SIZE_MIN; i += GRID_SIZE_MIN) {
            for (int j = -GRID_SIZE_MIN; j <= GRID_SIZE_MIN; j += GRID_SIZE_MIN) {
                PointF pnt = new PointF(pCurrentPoint.x - j, pCurrentPoint.y - i);
                int dist = Math.abs(i) + Math.abs(j);
                if (mLinearObjectsByCell.containsKey(pnt) &&
                        !mLinearObjectsByCell.get(pnt).isEmpty() &&
                        dist <= nearestDist) {
                    result = dist == nearestDist ? null : pnt;
                    nearestDist = dist;
                }
            }
        }
        return result;
    }

}
