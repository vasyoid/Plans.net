package ru.spbau.mit.plansnet.constructor;

import android.graphics.PointF;
import android.util.Log;

import org.andengine.entity.primitive.DrawMode;
import org.andengine.entity.primitive.Mesh;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.color.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class RoomSprite {

    private static VertexBufferObjectManager vertexBufferObjectManager;
    private static Map MAP;
    private List<PointF> polygon;
    private float initialX, initialY;
    private PointF titlePos;
    private Mesh mesh;
    private Color roomColor;

    public RoomSprite(List<PointF> pPolygon) {
        polygon = new ArrayList<>();
        polygon.addAll(pPolygon);
        Random rand = new Random();
        roomColor = new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
        reshape(polygon);
    }

    public Mesh getMesh() {
        return mesh;
    }

    private void reshape(List<PointF> pPolygon) {
        reshape(Geometry.makeTriangles(pPolygon));
    }

    private void reshape(float[] pBufferData) {
        PointF inside = Geometry.getPointInside(pBufferData);
        initialX = inside.x;
        initialY = inside.y;
        mesh = new Mesh(0, 0, pBufferData, pBufferData.length / 3,
                DrawMode.TRIANGLES, vertexBufferObjectManager);
        mesh.setColor(roomColor);
        mesh.setZIndex(-1);
    }

    public void updateShape() {
        reshape(polygon);
    }

    public static void setVertexBufferObjectManager(VertexBufferObjectManager
                                                            pVertexBufferObjectManager) {
        vertexBufferObjectManager = pVertexBufferObjectManager;
    }

    public static void setMap(Map pMap) {
        MAP = pMap;
    }

    public float getInitialX() {
        return initialX;
    }

    public float getInitialY() {
        return initialY;
    }

    public List<PointF> getPolygon() {
        return polygon;
    }

    public boolean contains(PointF point1, PointF point2) {
        polygon.add(polygon.get(0));
        for (int i = 0; i < polygon.size() - 1; i++) {
            if (polygon.get(i).equals(point1) &&
                    polygon.get(i + 1).equals(point2) ||
                    polygon.get(i).equals(point2) &&
                    polygon.get(i + 1).equals(point1)) {
                polygon.remove(polygon.size() - 1);
                return true;
            }
        }
        polygon.remove(polygon.size() - 1);
        return false;
    }

    public boolean onTouch(TouchEvent pSceneTouchEvent) {
        switch (MAP.getTouchState()) {
            case DEL:
                MAP.removeRoom(this);
                return false;
            case COLOR:
                return true;
            default:
                return false;
        }
    }
}