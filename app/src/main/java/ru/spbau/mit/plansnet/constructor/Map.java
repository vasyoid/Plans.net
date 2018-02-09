package ru.spbau.mit.plansnet.constructor;

import android.graphics.PointF;

import org.andengine.engine.Engine;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.scene.Scene;
import org.andengine.input.touch.TouchEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import ru.spbau.mit.plansnet.data.FloorMap;
import ru.spbau.mit.plansnet.data.objects.Door;
import ru.spbau.mit.plansnet.data.objects.MapObject;
import ru.spbau.mit.plansnet.data.objects.Room;
import ru.spbau.mit.plansnet.data.objects.Sticker;
import ru.spbau.mit.plansnet.data.objects.Wall;
import ru.spbau.mit.plansnet.data.objects.Window;

import static ru.spbau.mit.plansnet.constructor.ConstructorActivity.ActionState.ADD;

public class Map implements Serializable {

    private List<MapObjectSprite> objects = new LinkedList<>();
    private List<RoomSprite> rooms = new LinkedList<>();
    private List<MapObjectSprite> removedObjects = new LinkedList<>();
    private List<RoomSprite> removedRooms = new LinkedList<>();
    private HashMap<PointF, HashSet<MapObjectLinear>> linearObjectsByCell = new HashMap<>();
    private ConstructorActivity.ActionState touchState = ADD;
    private static int gridSize = 0;
    private static int gridCols = 0;
    private static int gridRows = 0;

    Map() { }

    Map(FloorMap pMap, Scene scene) {
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
                RoomSprite roomSprite = createRoom(room.getX(), room.getY(), scene);
                roomSprite.setTitle(room.getTitle().toString());
                roomSprite.setDescription(room.getDescription().toString());
                roomSprite.setColor(room.getColor());
            }
        }
    }

    public static List<PointF> getGridPolygon() {
        List<PointF> result = new ArrayList<>();
        result.add(new PointF(-1.0f, -1.0f));
        result.add(new PointF(-1.0f, gridRows * gridSize + 1.0f));
        result.add(new PointF(gridCols * gridSize + 1.0f, gridRows * gridSize + 1.0f));
        result.add(new PointF(gridCols * gridSize + 1.0f, -1.0f));
        return result;
    }

    public static void setGridSize(int pSize) {
        gridSize = pSize;
    }
    public static void setGridCols(int pCols) {
        gridCols = pCols;
    }
    public static void setGridRows(int pRows) {
        gridRows = pRows;
    }

    public static int getGridSize() {
        return gridSize;
    }

    public void setActionState(ConstructorActivity.ActionState state) {
        touchState = state;
    }

    public ConstructorActivity.ActionState getTouchState() {
        return touchState;
    }

    public List<MapObjectSprite> getObjects() {
        return objects;
    }

    public List<RoomSprite> getRooms() {
        return rooms;
    }

    private void addObjectToHashTable(PointF point, MapObjectLinear object) {
        PointF key = new PointF(point.x, point.y);
        if (!linearObjectsByCell.containsKey(point)) {
            linearObjectsByCell.put(key, new HashSet<>());
        }
        linearObjectsByCell.get(point).add(object);
    }

    private void removeObjectFromHashTable(PointF point, MapObjectLinear object) {
        PointF key = new PointF(point.x, point.y);
        if (linearObjectsByCell.containsKey(point)) {
            linearObjectsByCell.get(point).remove(object);
        }
    }

    public void updateMovedObject(PointF firstPoint1, PointF firstPoint2, MapObjectLinear object) {
        removeObjectFromHashTable(firstPoint1, object);
        removeObjectFromHashTable(firstPoint2, object);
        addObjectToHashTable(object.getPoint1(), object);
        addObjectToHashTable(object.getPoint2(), object);
    }

    public void setScaleByPoint(PointF at, float sx, float sy) {
        if (!linearObjectsByCell.containsKey(at)) {
            return;
        }
        for (MapObjectLinear object : linearObjectsByCell.get(at)) {
            object.setScale(sx, sy);
        }
    }

    public void moveObjects(PointF at, PointF from, PointF to) {
        if (!linearObjectsByCell.containsKey(at)) {
            return;
        }
        for (MapObjectLinear object : linearObjectsByCell.get(at)) {
            if (object.getPoint1().equals(from)) {
                object.setPoint1(to);
            } else {
                object.setPoint2(to);
            }
        }
    }

    void updateRooms(Scene pScene) {
        for (RoomSprite room : rooms) {
            room.detachSelf();
            room.updateShape();
            room.attachSelf(pScene);
        }
        pScene.sortChildren();
    }

    void updateObjects(PointF at) {
        if (!linearObjectsByCell.containsKey(at)) {
            return;
        }
        for (MapObjectLinear object : linearObjectsByCell.get(at)) {
            addObjectToHashTable(object.getPoint1(), object);
            addObjectToHashTable(object.getPoint2(), object);
        }
        linearObjectsByCell.get(at).clear();
    }

    public void addObject(MapObjectSprite object) {
        removedObjects.remove(object);
        objects.add(object);
        if (object instanceof MapObjectLinear) {
            MapObjectLinear objectLinear = (MapObjectLinear) object;
            addObjectToHashTable(objectLinear.getPoint1(), objectLinear);
            addObjectToHashTable(objectLinear.getPoint2(), objectLinear);
        }
    }

    public void addRoom(RoomSprite room) {
        removedRooms.remove(room);
        rooms.add(room);
    }

    public List<RoomSprite> findRoomsBySection(PointF point1, PointF point2) {
        List<RoomSprite> result = new ArrayList<>();
        for (RoomSprite room : rooms) {
            if (room.contains(point1, point2)) {
                result.add(room);
            }
        }
        return result;
    }

    public void removeRoomsBySection(PointF point1, PointF point2) {
        for (RoomSprite room : findRoomsBySection(point1, point2)) {
            removeRoom(room);
        }
    }

    public void removeObject(MapObjectSprite object) {
        objects.remove(object);
        if (object instanceof MapObjectLinear) {
        MapObjectLinear objectLinear = (MapObjectLinear) object;
            linearObjectsByCell.get(objectLinear.getPoint1()).remove(object);
            linearObjectsByCell.get(objectLinear.getPoint2()).remove(object);
        }
        removedObjects.add(object);
    }

    public void removeRoom(RoomSprite room) {
        rooms.remove(room);
        removedRooms.add(room);
    }

    public void detachRemoved(Engine pEngine) {
        if (removedObjects.isEmpty() && removedRooms.isEmpty()) {
            return;
        }
        Semaphore mutex = new Semaphore(1);
        pEngine.runOnUpdateThread(() -> {
            for (MapObjectSprite o : removedObjects) {
                o.detachSelf();
            }
            for (RoomSprite r : removedRooms) {
                r.detachSelf();
            }
            removedObjects.clear();
            removedRooms.clear();
            mutex.release();
        });
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void clear() {
        removedObjects.addAll(objects);
        removedRooms.addAll(rooms);
        objects.clear();
        rooms.clear();
    }

    public boolean hasIntersections(PointF pPoint) {
        if (!linearObjectsByCell.containsKey(pPoint)) {
            return false;
        }
        for (MapObjectLinear object : linearObjectsByCell.get(pPoint)) {
            if (hasIntersections(object)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasIntersections(MapObjectLinear pObject) {
        for (MapObjectSprite o : objects) {
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

    @Deprecated
    public void joinAll(MapObjectLinear pObject) {
        for (MapObjectSprite o : objects) {
            if (!(o instanceof MapObjectLinear)) {
                continue;
            }
            MapObjectLinear ol = (MapObjectLinear) o;
            Line joined = Geometry.join(ol.getPosition(), pObject.getPosition());
            if (joined != null) {
                pObject.setPosition(joined);
                removedObjects.add(o);
            }
        }
        objects.removeAll(removedObjects);
    }

    public RoomSprite getRoomTouched(TouchEvent pTouchEvent) {
        PointF touchPoint = new PointF(pTouchEvent.getX(), pTouchEvent.getY());
        for (RoomSprite r : rooms) {
            if (Geometry.isPointInsidePolygon(r.getPolygon(), touchPoint)) {
                return r;
            }
        }
        return null;
    }

    public RoomSprite createRoom(float pX, float pY, Scene pScene) {
        List<PointF> polygon = Geometry.roomPolygon(objects, new PointF(pX, pY));
        if (polygon == null || !Geometry.isPointInsidePolygon(polygon, new PointF(pX, pY))) {
            return null;
        }
        RoomSprite room = new RoomSprite(polygon);
        addRoom(room);
        room.attachSelf(pScene);
        pScene.sortChildren();
        return room;
    }
}
