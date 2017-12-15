package ru.spbau.mit.plansnet.constructor;

import android.graphics.PointF;

import org.andengine.entity.primitive.Line;
import org.andengine.entity.scene.Scene;
import org.andengine.input.touch.TouchEvent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import ru.spbau.mit.plansnet.data.FloorMap;
import ru.spbau.mit.plansnet.data.objects.Door;
import ru.spbau.mit.plansnet.data.objects.MapObject;
import ru.spbau.mit.plansnet.data.objects.Room;
import ru.spbau.mit.plansnet.data.objects.Wall;

public class Map implements Serializable {

    private List<MapObjectSprite> objects;
    private List<RoomSprite> rooms;
    private List<MapObjectSprite> removedObjects;
    private List<RoomSprite> removedRooms;
    private HashMap<PointF, HashSet<MapObjectLinear>> linearObjectsByCell;
    private ConstructorActivity.ActionState touchState;
    private static int gridSize;

    Map() {
        objects = new LinkedList<>();
        rooms = new LinkedList<>();
        removedObjects = new LinkedList<>();
        removedRooms = new LinkedList<>();
        linearObjectsByCell = new HashMap<>();
    }

    Map(FloorMap pMap) {
        this();
        for (MapObject o : pMap.getArrayData()) {
            if (o instanceof Door) {
                addObject(new DoorSprite((Door) o));
            } else if (o instanceof Wall) {
                addObject(new WallSprite((Wall) o));
            } else if (o instanceof Room) {
                addRoom(new RoomSprite((Room) o));
            }
        }
    }

    public static void setGridSize(int pSize) {
        gridSize = pSize;
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
            pScene.detachChild(room.getMesh());
            room.updateShape();
            pScene.attachChild(room.getMesh());
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

    public void addObject(MapObjectLinear object) {
        removedObjects.remove(object);
        objects.add(object);
        addObjectToHashTable(object.getPoint1(), object);
        addObjectToHashTable(object.getPoint2(), object);
    }

    public void addRoom(RoomSprite room) {
        removedRooms.remove(room);
        rooms.add(room);
    }

    public void removeObject(MapObjectLinear object) {
        objects.remove(object);
        linearObjectsByCell.get(object.getPoint1()).remove(object);
        linearObjectsByCell.get(object.getPoint2()).remove(object);
        removedObjects.add(object);
    }

    public void removeRoom(RoomSprite room) {
        rooms.remove(room);
        removedRooms.add(room);
    }

    public void detachRemoved() {
        for (MapObjectSprite o : removedObjects) {
            o.detachSelf();
        }
        for (RoomSprite r : removedRooms) {
            r.getMesh().detachSelf();
        }
        removedObjects.clear();
        removedRooms.clear();
    }

    public void clear() {
        removedObjects.addAll(objects);
        removedRooms.addAll(rooms);
        objects.clear();
        rooms.clear();
    }

    public boolean checkIntersections(PointF pPoint) {
        if (!linearObjectsByCell.containsKey(pPoint)) {
            return false;
        }
        for (MapObjectLinear object : linearObjectsByCell.get(pPoint)) {
            if (checkIntersections(object)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkIntersections(MapObjectLinear pObject) {
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

    public boolean checkRoomTouched(TouchEvent pTouchEvent) {
        PointF touchPoint = new PointF(pTouchEvent.getX(), pTouchEvent.getY());
        for (RoomSprite r : rooms) {
            if (Geometry.isPointInsidePolygon(r.getPolygon(), touchPoint)) {
                return r.onTouch(pTouchEvent);
            }
        }
        return false;
    }

    public void createRoom(float pX, float pY, Scene pScene) {
        pX = (float)(Math.floor(pX / gridSize) + 0.5f) * gridSize;
        pY = (float)(Math.floor(pY / gridSize) + 0.5f) * gridSize;

        List<PointF> polygon = Geometry.roomPolygon(objects, new PointF(pX, pY));
        if (polygon == null || !Geometry.isPointInsidePolygon(polygon, new PointF(pX, pY))) {
            return;
        }
        RoomSprite room = new RoomSprite(polygon, pX, pY);
        addRoom(room);
        pScene.attachChild(room.getMesh());
        pScene.sortChildren();
    }
}
