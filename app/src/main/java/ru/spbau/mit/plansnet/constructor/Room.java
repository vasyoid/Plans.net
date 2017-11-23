package ru.spbau.mit.plansnet.constructor;

import android.graphics.PointF;

import org.andengine.entity.primitive.DrawMode;
import org.andengine.entity.primitive.Mesh;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import java.util.List;


public class Room extends Mesh {

    private static Map MAP;
    private List<PointF> polygon;

    public static void setMap(Map pMap) {
        MAP = pMap;
    }

    public Room(List<PointF> pPolygon, float pX, float pY, float[] pBufferData, int pVertexCount, DrawMode pDrawMode, VertexBufferObjectManager pVertexBufferObjectManager) {
        super(pX, pY, pBufferData, pVertexCount, pDrawMode, pVertexBufferObjectManager);
        polygon = pPolygon;
    }

    public List<PointF> getPolygon() {
        return polygon;
    }

    public boolean onTouch(TouchEvent pSceneTouchEvent) {
        switch (MAP.getTouchState()) {
            case 1:
                MAP.removeRoom(this);
                return false;
            case 3:
                return true;
            default:
                return false;
        }
    }
}