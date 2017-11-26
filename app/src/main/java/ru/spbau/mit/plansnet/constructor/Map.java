package ru.spbau.mit.plansnet.constructor;

import android.graphics.PointF;

import com.earcutj.Earcut;

import org.andengine.entity.primitive.DrawMode;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.scene.Scene;
import org.andengine.input.touch.TouchEvent;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Map implements Serializable {

    private List<MapObjectSprite> objects;
    private List<RoomSprite> rooms;
    private List<MapObjectSprite> removedObjects;
    private List<RoomSprite> removedRooms;
    private int touchState;
    private int gridSize;
    private int gridCols;
    private int gridRows;

    Map(int gridSize, int gridCols, int gridRows) {
        this.gridSize = gridSize;
        this.gridCols = gridCols;
        this.gridRows = gridRows;
        objects = new LinkedList<>();
        rooms = new LinkedList<>();
        removedObjects = new LinkedList<>();
        removedRooms = new LinkedList<>();
    }

    public void setTouchState(int state) {
        touchState = state;
    }

    public int getTouchState() {
        return touchState;
    }

    public List<MapObjectSprite> getObjects() {
        return objects;
    }

    public List<RoomSprite> getRooms() {
        return rooms;
    }

    public void addObject(MapObjectSprite object) {
        removedObjects.remove(object);
        objects.add(object);
    }
    public void addRoom(RoomSprite room) {
        removedRooms.remove(room);
        rooms.add(room);
    }

    public void removeObject(MapObjectSprite object) {
        objects.remove(object);
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
            r.detachSelf();
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

    public boolean checkIntersections(MapObjectLinear pObject) {
        for (MapObjectSprite o : objects) {
            if (!(o instanceof MapObjectLinear)) {
                continue;
            }
            MapObjectLinear ol = (MapObjectLinear) o;
            if (Geometry.linesIntersect(ol.getPosition(), pObject.getPosition())) {
                return true;
            }
        }
        return false;
    }

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
        pScene.attachChild(room);
        pScene.sortChildren();
    }
}
