package ru.spbau.mit.plansnet.constructor;

import android.graphics.PointF;
import android.util.Log;

import org.andengine.entity.primitive.DrawMode;
import org.andengine.entity.primitive.Mesh;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.text.Text;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.color.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class RoomSprite {

    private static VertexBufferObjectManager vertexBufferObjectManager;
    private static Map MAP;
    private static Font font;
    private List<PointF> polygon = new ArrayList<>();
    private float initialX, initialY;
    private Mesh mesh;
    private Color roomColor;
    private Text title;
    private String description;
    private Random rand = new Random();

    public RoomSprite(List<PointF> pPolygon) {
        polygon.addAll(pPolygon);
        roomColor = new Color(rand.nextFloat() * 0.5f + 0.5f,
                rand.nextFloat() * 0.5f + 0.5f, rand.nextFloat() * 0.5f + 0.5f);
        title = new Text(initialX, initialY, font, "", 30, vertexBufferObjectManager);
        title.setZIndex(2);
        title.setColor(Color.BLACK);
        reshape(polygon);
    }

    public static void setFont(Font pFont) {
        font = pFont;
        font.load();
    }

    public void setColor(Color pColor) {
        roomColor.set(pColor.getRed(), pColor.getGreen(), pColor.getBlue(), 1.0f);
        mesh.setColor(roomColor);
    }

    public void setTitle(String pTitle) {
        title.setText(pTitle);
        setTitlePosition();
    }

    public CharSequence getTitle() {
        return title.getText();
    }

    public Color getColor() {
        return roomColor;
    }

    public CharSequence getDescription() {
        return description;
    }
    public void setDescription(String pDescription) {
        description = pDescription;
    }

    public void attachSelf(Scene pScene) {
        pScene.attachChild(mesh);
        pScene.attachChild(title);
    }

    public void detachSelf() {
        mesh.detachSelf();
        title.detachSelf();
    }

    private void setTitlePosition() {
        title.setPosition(initialX - title.getWidth() / 2, initialY - title.getHeight() / 2);
    }

    private void reshape(List<PointF> pPolygon) {
        reshape(Geometry.makeTriangles(pPolygon));
    }

    private void reshape(float[] pBufferData) {
        PointF inside = Geometry.getPointInside(pBufferData);
        initialX = inside.x;
        initialY = inside.y;
        setTitlePosition();
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
        for (int i = 0; i < polygon.size(); i++) {
            if (polygon.get(i).equals(point1) &&
                    polygon.get((i + 1) % polygon.size()).equals(point2) ||
                    polygon.get(i).equals(point2) &&
                    polygon.get((i + 1) % polygon.size()).equals(point1)) {
                return true;
            }
        }
        return false;
    }
}