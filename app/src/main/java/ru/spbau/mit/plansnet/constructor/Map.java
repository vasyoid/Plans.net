package ru.spbau.mit.plansnet.constructor;

import android.graphics.PointF;

import com.earcutj.Earcut;

import org.andengine.entity.primitive.DrawMode;
import org.andengine.entity.primitive.Line;
import org.andengine.entity.scene.Scene;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Map implements Serializable {

    private List<MapObject> objects;
    private List<Room> rooms;
    private List<MapObject> removedObjects;
    private List<Room> removedRooms;
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

    public List<MapObject> getObjects() {
        return objects;
    }

    public void addObject(MapObject object) {
        removedObjects.remove(object);
        objects.add(object);
    }
    public void addRoom(Room room) {
        removedRooms.remove(room);
        rooms.add(room);
    }

    public void removeObject(MapObject object) {
        objects.remove(object);
        removedObjects.add(object);
    }

    public void removeRoom(Room room) {
        rooms.remove(room);
        removedRooms.add(room);
    }

    public void detachRemoved() {
        for (MapObject o : removedObjects) {
            o.detachSelf();
        }
        for (Room r : removedRooms) {
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
        for (MapObject o : objects) {
            if (!(o instanceof MapObjectLinear)) {
                continue;
            }
            MapObjectLinear ol = (MapObjectLinear) o;
            if (LineHelper.linesIntersect(ol.getPosition(), pObject.getPosition())) {
                return true;
            }
        }
        return false;
    }

    public void joinAll(MapObjectLinear pObject) {
        for (MapObject o : objects) {
            if (!(o instanceof MapObjectLinear)) {
                continue;
            }
            MapObjectLinear ol = (MapObjectLinear) o;
            Line joined = LineHelper.join(ol.getPosition(), pObject.getPosition());
            if (joined != null) {
                pObject.setPosition(joined);
                removedObjects.add(o);
            }
        }
        objects.removeAll(removedObjects);
    }

    private List<PointF> roomPolygon(PointF point) {
        List<PointF> polygon = new ArrayList<>();
        MapObjectLinear currentObject = null;
        float curX = -1e5f;
        Line ray = new Line(-1e5f, point.y, point.x, point.y, null);
        for (MapObject o : objects) {
            if (!(o instanceof MapObjectLinear)) {
                continue;
            }
            MapObjectLinear ol = (MapObjectLinear) o;
            PointF tmp = LineHelper.getIntersectionPoint(
                    ray, ol.getPosition(), true);
            if (tmp != null && curX < tmp.x) {
                currentObject = (MapObjectLinear) o;
                curX = tmp.x;
            }
        }
        if (currentObject == null) {
            return null;
        }
        if (currentObject.getPosition().getY1() > currentObject.getPosition().getY2()) {
            LineHelper.changeDirection(currentObject.getPosition());
        }
        polygon.add(new PointF(currentObject.getPosition().getX1(),
                currentObject.getPosition().getY1()));
        while (true) {
            PointF curPoint = new PointF(currentObject.getPosition().getX2(),
                    currentObject.getPosition().getY2());
            if (curPoint.equals(polygon.get(0))) {
                break;
            }
            polygon.add(curPoint);
            MapObjectLinear nextObject = null;
            float currentAngle = 10;
            for (MapObject o : objects) {
                if (!(o instanceof MapObjectLinear)) {
                    continue;
                }
                MapObjectLinear ol = (MapObjectLinear) o;
                if (LineHelper.linesJoinable(ol.getPosition(), currentObject.getPosition())) {
                    continue;
                }
                if (LineHelper.lineEndsWith(ol.getPosition(), curPoint)) {
                    LineHelper.changeDirection(ol.getPosition());
                }
                if (LineHelper.lineStartsWith(ol.getPosition(), curPoint)) {
                    if (currentAngle > LineHelper.getAngle(currentObject.getPosition(), ol.getPosition())) {
                        currentAngle = LineHelper.getAngle(currentObject.getPosition(), ol.getPosition());
                        nextObject = (MapObjectLinear) o;
                    }
                }
            }
            if (nextObject == null) {
                return null;
            }
            currentObject = nextObject;
        }
        return polygon;
    }

    private boolean isPointInsidePolygon(List<PointF> polygon, PointF point) {
        int cntIntersections = 0;
        Line line = new Line(point.x, point.y, point.x, -1e5f, null);
        polygon.add(polygon.get(0));
        for (int i = 0; i < polygon.size() - 1; i++) {
            Line side = new Line(polygon.get(i).x, polygon.get(i).y,
                    polygon.get(i + 1).x, polygon.get(i + 1).y, null);
            if (side.collidesWith(line)) {
                cntIntersections++;
            }
        }
        return cntIntersections % 2 == 1;
    }

    public boolean checkRoomTouched(TouchEvent pTouchEvent) {
        PointF touchPoint = new PointF(pTouchEvent.getX(), pTouchEvent.getY());
        for (Room r : rooms) {
            if (isPointInsidePolygon(r.getPolygon(), touchPoint)) {
                return r.onTouch(pTouchEvent);
            }
        }
        return false;
    }

    public void createRoom(float pX, float pY, Scene pScene,
                           VertexBufferObjectManager pVertexBufferObjectManager) {

        pX = (float)(Math.floor(pX / gridSize) + 0.5f) * gridSize;
        pY = (float)(Math.floor(pY / gridSize) + 0.5f) * gridSize;

        List<PointF> polygon = roomPolygon(new PointF(pX, pY));
        if (polygon == null || !isPointInsidePolygon(polygon, new PointF(pX, pY))) {
            return;
        }

        float[][][] vertices = new float[1][polygon.size()][2];

        for (int i = 0; i < polygon.size(); i++) {
            vertices[0][i][0] = polygon.get(i).x;
            vertices[0][i][1] = polygon.get(i).y;
        }

        List<float[][]> triangles = Earcut.earcut(vertices, true);

        float[] vertexData = new float[triangles.size() * 9];

        for (int i = 0; i < triangles.size(); i++) {
            for (int t = 0; t < 3; ++t) {
                vertexData[9 * i + 3 * t] = triangles.get(i)[t][0];
                vertexData[9 * i + 3 * t + 1] = triangles.get(i)[t][1];
            }
        }

        Random rand = new Random();

        Room room = new Room(polygon, 0, 0,
                vertexData, vertexData.length / 3,
                DrawMode.TRIANGLES, pVertexBufferObjectManager);
        room.setColor(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
        addRoom(room);
        room.setZIndex(-1);
        pScene.attachChild(room);
        pScene.sortChildren();
    }
}
