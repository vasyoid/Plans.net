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
import java.util.stream.Collectors;

import ru.spbau.mit.plansnet.data.objects.Room;


public class RoomSprite {

    private static VertexBufferObjectManager vertexBufferObjectManager;
    private static Map MAP;
    private List<PointF> polygon;
    private float initialX, initialY;
    private Mesh mesh;
    private Color roomColor;

    public RoomSprite(List<PointF> pPolygon, float pX, float pY) {
        initialX = pX;
        initialY = pY;
        polygon = new ArrayList<>();
        polygon.addAll(pPolygon);
        Random rand = new Random();
        roomColor = new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
        reshape(polygon);
    }

    public RoomSprite(Room  pRoom) {
       this(Geometry.roomPolygon(MAP.getObjects(),
               new PointF(pRoom.getX(), pRoom.getY())), pRoom.getX(), pRoom.getY());
    }

    public Mesh getMesh() {
        return mesh;
    }

    private void reshape(List<PointF> pPolygon) {
        reshape(Geometry.makeTriangles(pPolygon));
    }

    private void reshape(float[] pBufferData) {
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