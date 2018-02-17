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

public class Map implements Serializable {

    private static int gridSize = 0;

    private List<MapObjectSprite> mObjects = new LinkedList<>();
    private List<RoomSprite> mRooms = new LinkedList<>();
    private List<MapObjectSprite> mRemovedObjects = new LinkedList<>();
    private List<RoomSprite> mRemovedRooms = new LinkedList<>();
    private HashMap<PointF, HashSet<MapObjectLinear>> mLinearObjectsByCell = new HashMap<>();
    private ConstructorActivity.ActionState mTouchState = ADD;
    private Sprite mBackgroundSprite = null;

    public Map() { }

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
                roomSprite.setmTitle(room.getTitle().toString());
                roomSprite.setmDescription(room.getDescription().toString());
                roomSprite.setColor(room.getColor());
            }
        }
    }

    public static @NonNull List<PointF> getGridPolygon() {
        List<PointF> result = new ArrayList<>();
        result.add(new PointF(-1.0f, -1.0f));
        result.add(new PointF(-1.0f, MAP_HEIGHT + 1.0f));
        result.add(new PointF(MAP_WIDTH + 1.0f, MAP_HEIGHT + 1.0f));
        result.add(new PointF(MAP_WIDTH + 1.0f, -1.0f));
        return result;
    }

    public static int getGridSize() {
        return gridSize;
    }

    public static void setGridSize(int pSize) {
        gridSize = pSize;
    }

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

    public boolean isBackgroundSet() {
        return mBackgroundSprite != null;
    }

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

    public void setActionState(@NonNull ActionState pState) {
        mTouchState = pState;
    }

    public @NonNull ActionState getTouchState() {
        return mTouchState;
    }

    public @NonNull List<MapObjectSprite> getmObjects() {
        return mObjects;
    }

    public @NonNull List<RoomSprite> getmRooms() {
        return mRooms;
    }

    private void addObjectToHashTable(@NonNull PointF pPoint, @NonNull MapObjectLinear pObject) {
        PointF key = new PointF(pPoint.x, pPoint.y);
        if (!mLinearObjectsByCell.containsKey(pPoint)) {
            mLinearObjectsByCell.put(key, new HashSet<>());
        }
        mLinearObjectsByCell.get(pPoint).add(pObject);
    }

    private void removeObjectFromHashTable(@NonNull PointF pPoint,
                                           @NonNull MapObjectLinear pObject) {
        if (mLinearObjectsByCell.containsKey(pPoint)) {
            mLinearObjectsByCell.get(pPoint).remove(pObject);
        }
    }

    public void updateMovedObject(@NonNull PointF pFirstPoint1,
                                  @NonNull PointF pFirstPoint2,
                                  @NonNull MapObjectLinear pObject) {
        removeObjectFromHashTable(pFirstPoint1, pObject);
        removeObjectFromHashTable(pFirstPoint2, pObject);
        addObjectToHashTable(pObject.getmPoint1(), pObject);
        addObjectToHashTable(pObject.getmPoint2(), pObject);
    }

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

    public void moveObjects(@NonNull PointF pFrom, @NonNull PointF pTo) {
        if (!mLinearObjectsByCell.containsKey(pFrom)) {
            return;
        }
        for (MapObjectLinear object : mLinearObjectsByCell.get(pFrom)) {
            if (object.getmPoint1().equals(pFrom)) {
                object.changeDirection();
            }
            object.setmPoint2(pTo);
        }
    }

    public void updateRooms(@NonNull Scene pScene) {
        for (RoomSprite room : mRooms) {
            room.detachSelf();
            room.updateShape();
            room.attachSelf(pScene);
        }
        pScene.sortChildren();
    }

    public void updateObjects(@NonNull PointF pAt) {
        if (!mLinearObjectsByCell.containsKey(pAt)) {
            return;
        }
        List<MapObjectLinear> tmp = new ArrayList<>(mLinearObjectsByCell.get(pAt));
        mLinearObjectsByCell.get(pAt).clear();
        for (MapObjectLinear object : tmp) {
            addObjectToHashTable(object.getmPoint1(), object);
            addObjectToHashTable(object.getmPoint2(), object);
        }
    }

    public void addObject(@NonNull MapObjectSprite pObject) {
        mRemovedObjects.remove(pObject);
        mObjects.add(pObject);
        if (pObject instanceof MapObjectLinear) {
            MapObjectLinear objectLinear = (MapObjectLinear) pObject;
            addObjectToHashTable(objectLinear.getmPoint1(), objectLinear);
            addObjectToHashTable(objectLinear.getmPoint2(), objectLinear);
        }
    }

    public void addRoom(@NonNull RoomSprite pRoom) {
        mRemovedRooms.remove(pRoom);
        mRooms.add(pRoom);
    }

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

    public void removeRoomsBySection(@NonNull PointF pPoint1, @NonNull PointF pPoint2) {
        for (RoomSprite room : findRoomsBySection(pPoint1, pPoint2)) {
            removeRoom(room);
        }
    }

    public void removeObject(@NonNull MapObjectSprite pObject) {
        mObjects.remove(pObject);
        if (pObject instanceof MapObjectLinear) {
            MapObjectLinear objectLinear = (MapObjectLinear) pObject;
            if (mLinearObjectsByCell.get(objectLinear.getmPoint1()).contains(pObject)) {
                mLinearObjectsByCell.get(objectLinear.getmPoint1()).remove(pObject);
            }
            if (mLinearObjectsByCell.get(objectLinear.getmPoint2()).contains(pObject)) {
                mLinearObjectsByCell.get(objectLinear.getmPoint2()).remove(pObject);
            }
        }
        mRemovedObjects.add(pObject);
    }

    public void removeRoom(@NonNull RoomSprite pRoom) {
        mRooms.remove(pRoom);
        mRemovedRooms.add(pRoom);
    }

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

    public void clear() {
        mRemovedObjects.addAll(mObjects);
        mRemovedRooms.addAll(mRooms);
        mObjects.clear();
        mRooms.clear();
        mLinearObjectsByCell.clear();
    }

    public boolean hasIntersections(@NonNull PointF pPoint) {
        if (!mLinearObjectsByCell.containsKey(pPoint)) {
            return false;
        }
        for (MapObjectLinear object : mLinearObjectsByCell.get(pPoint)) {
            if (hasIntersections(object) || Geometry.length(object.getmPosition()) == 0) {
                return true;
            }
        }
        return false;
    }

    public boolean hasIntersections(@NonNull MapObjectLinear pObject) {
        for (MapObjectSprite o : mObjects) {
            if (!(o instanceof MapObjectLinear)) {
                continue;
            }
            MapObjectLinear ol = (MapObjectLinear) o;
            if (ol.equals(pObject)) {
                continue;
            }
            if (Geometry.linesIntersect(ol.getmPosition(), pObject.getmPosition())) {
                return true;
            }
        }
        return false;
    }

    public @Nullable RoomSprite getRoomTouchedOrNull(@NonNull TouchEvent pTouchEvent) {
        PointF touchPoint = new PointF(pTouchEvent.getX(), pTouchEvent.getY());
        for (RoomSprite r : mRooms) {
            if (Geometry.isPointInsidePolygon(r.getmPolygon(), touchPoint)) {
                return r;
            }
        }
        return null;
    }

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
