package ru.spbau.mit.plansnet.constructor;

import android.graphics.PointF;

import com.earcutj.Earcut;

import org.andengine.entity.primitive.DrawMode;
import org.andengine.entity.primitive.Mesh;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import java.util.List;
import java.util.Random;

import ru.spbau.mit.plansnet.data.objects.Room;


public class RoomSprite extends Mesh {

    private static VertexBufferObjectManager vertexBufferObjectManager;
    private static Map MAP;
    private List<PointF> polygon;
    private float initialX, initialY;

    public RoomSprite(List<PointF> pPolygon, float pX, float pY) {
        this(pPolygon, pX, pY, Geometry.makeTriangles(pPolygon));
        Random rand = new Random();
        setColor(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
        setZIndex(-1);
    }

    public RoomSprite(List<PointF> pPolygon, float pX, float pY, float[] pBufferData) {
        super(0, 0, pBufferData, pBufferData.length / 3,
                DrawMode.TRIANGLES, vertexBufferObjectManager);
        initialX = pX;
        initialY = pY;
        polygon = pPolygon;
    }

/*
 *   public RoomSprite(Room pRoom) {
 *       this(Geometry.roomPolygon(MAP.getObjects(),
 *               new PointF(pRoom.getX(), pRoom.getY())), pRoom.getX(), pRoom.getY());
 *   }
 */

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