package ru.spbau.mit.plansnet.constructor.objects;

import android.graphics.PointF;
import android.support.annotation.NonNull;

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

import ru.spbau.mit.plansnet.constructor.Geometry;


public class RoomSprite {

    private static VertexBufferObjectManager vertexBufferObjectManager;
    private static Font font;

    private List<PointF> mPolygon = new ArrayList<>();
    private float mInitialX, mInitialY;
    private Mesh mMesh;
    private Color mRoomColor;
    private Text mTitle;
    private String mDescription;

    public RoomSprite(@NonNull List<PointF> pPolygon) {
        mPolygon.addAll(pPolygon);
        Random rand = new Random();
        mRoomColor = new Color(rand.nextFloat() * 0.5f + 0.5f,
                rand.nextFloat() * 0.5f + 0.5f, rand.nextFloat() * 0.5f + 0.5f);
        mTitle = new Text(mInitialX, mInitialY, font, "", 30,
                vertexBufferObjectManager);
        mTitle.setZIndex(2);
        mTitle.setColor(Color.BLACK);
        reshape(mPolygon);
    }

    public static void setFont(@NonNull Font pFont) {
        font = pFont;
        font.load();
    }

    public void setColor(@NonNull Color pColor) {
        mRoomColor.set(pColor.getRed(), pColor.getGreen(), pColor.getBlue(), 1.0f);
        mMesh.setColor(mRoomColor);
    }

    public void setTitle(@NonNull String pTitle) {
        mTitle.setText(pTitle);
        setTitlePosition();
    }

    public @NonNull CharSequence getTitle() {
        return mTitle.getText();
    }

    public @NonNull Color getColor() {
        return mRoomColor;
    }

    public @NonNull CharSequence getDescription() {
        return mDescription;
    }

    public void setDescription(@NonNull String pDescription) {
        mDescription = pDescription;
    }

    public void attachSelf(@NonNull Scene pScene) {
        pScene.attachChild(mMesh);
        pScene.attachChild(mTitle);
    }

    public void detachSelf() {
        mMesh.detachSelf();
        mTitle.detachSelf();
    }

    private void setTitlePosition() {
        mTitle.setPosition(mInitialX - mTitle.getWidth() / 2,
                mInitialY - mTitle.getHeight() / 2);
    }

    private void reshape(@NonNull List<PointF> pPolygon) {
        reshape(Geometry.makeTriangles(pPolygon));
    }

    private void reshape(@NonNull float[] pBufferData) {
        PointF inside = Geometry.getPointInside(pBufferData);
        mInitialX = inside.x;
        mInitialY = inside.y;
        setTitlePosition();
        mMesh = new Mesh(0, 0, pBufferData, pBufferData.length / 3,
                DrawMode.TRIANGLES, vertexBufferObjectManager);
        mMesh.setColor(mRoomColor);
        mMesh.setZIndex(-2);
    }

    public void updateShape() {
        reshape(mPolygon);
    }

    public static void setVertexBufferObjectManager(@NonNull VertexBufferObjectManager
                                                            pVertexBufferObjectManager) {
        vertexBufferObjectManager = pVertexBufferObjectManager;
    }

    public float getInitialX() {
        return mInitialX;
    }

    public float getInitialY() {
        return mInitialY;
    }

    public List<PointF> getPolygon() {
        return mPolygon;
    }

    public boolean contains(@NonNull PointF pPoint1, @NonNull PointF pPoint2) {
        for (int i = 0; i < mPolygon.size(); i++) {
            if (mPolygon.get(i).equals(pPoint1) &&
                    mPolygon.get((i + 1) % mPolygon.size()).equals(pPoint2) ||
                    mPolygon.get(i).equals(pPoint2) &&
                    mPolygon.get((i + 1) % mPolygon.size()).equals(pPoint1)) {
                return true;
            }
        }
        return false;
    }

}