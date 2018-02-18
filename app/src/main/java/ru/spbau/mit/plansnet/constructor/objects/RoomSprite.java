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


/**
 * Sprite for rooms.
 */
public class RoomSprite {

    private static VertexBufferObjectManager vertexBufferObjectManager;
    private static Font font;

    private List<PointF> mPolygon = new ArrayList<>();
    private float mInitialX, mInitialY;
    private Mesh mMesh;
    private Color mRoomColor;
    private Text mTitle;
    private String mDescription;

    /**
     * Constructor.
     * @param pPolygon points representing the corners of the room.
     */
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

    /**
     * Font setter
     * Used only once when an activity is initialised.
     * @param pFont A font to be used for all rooms' names.
     */
    public static void setFont(@NonNull Font pFont) {
        font = pFont;
        font.load();
    }

    /**
     * Color setter
     * @param pColor room color.
     */
    public void setColor(@NonNull Color pColor) {
        mRoomColor.set(pColor.getRed(), pColor.getGreen(), pColor.getBlue(), 1.0f);
        mMesh.setColor(mRoomColor);
    }

    /**
     * Title setter
     * @param pTitle room title.
     */
    public void setTitle(@NonNull String pTitle) {
        mTitle.setText(pTitle);
        setTitlePosition();
    }

    /**
     * Sets title position automatically according to the room shape.
     */
    private void setTitlePosition() {
        mTitle.setPosition(mInitialX - mTitle.getWidth() / 2,
                mInitialY - mTitle.getHeight() / 2);
    }

    /**
     * Description setter.
     * @param pDescription room description.
     */
    public void setDescription(@NonNull String pDescription) {
        mDescription = pDescription;
    }

    /**
     * Title getter
     * @return room title.
     */
    public @NonNull CharSequence getTitle() {
        return mTitle.getText();
    }

    /**
     * Color getter
     * @return room color.
     */
    public @NonNull Color getColor() {
        return mRoomColor;
    }

    /**
     * Description getter
     * @return room description.
     */
    public @NonNull CharSequence getDescription() {
        return mDescription;
    }

    /**
     * Attaches the room to a scene.
     * @param pScene scene to attach the room to.
     */
    public void attachSelf(@NonNull Scene pScene) {
        pScene.attachChild(mMesh);
        pScene.attachChild(mTitle);
    }

    /**
     * Detaches the room from the scene.
     */
    public void detachSelf() {
        mMesh.detachSelf();
        mTitle.detachSelf();
    }

    /**
     * Brings the room to the shape of a given polygon.
     * @param pPolygon polygon representing the new shape.
     */
    private void reshape(@NonNull List<PointF> pPolygon) {
        reshape(Geometry.makeTriangles(pPolygon));
    }

    /**
     * Brings the room to the given shape.
     * @param pBufferData array of coordinates representing the new shape.
     */
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

    /**
     * Automatically reshapes the room.
     */
    public void updateShape() {
        reshape(mPolygon);
    }

    /**
     * Vertex buffer object manager setter
     * @param pVertexBufferObjectManager new value.
     */
    public static void setVertexBufferObjectManager(@NonNull VertexBufferObjectManager
                                                            pVertexBufferObjectManager) {
        vertexBufferObjectManager = pVertexBufferObjectManager;
    }

    /**
     * Initial x coordinate getter.
     * @return initial x coordinate of the room.
     */
    public float getInitialX() {
        return mInitialX;
    }

    /**
     * Initial y coordinate getter.
     * @return initial y coordinate of the room.
     */
    public float getInitialY() {
        return mInitialY;
    }

    /**
     * Polygon getter.
     * @return polygon representing the shape of the room.
     */
    public List<PointF> getPolygon() {
        return mPolygon;
    }

    /**
     * Says if a segment lies in the room perimeter.
     * @param pPoint1 first segment end.
     * @param pPoint2 second segment end.
     * @return true if the segment is one of the room polygon sides, false otherwise.
     */
    public boolean containsSide(@NonNull PointF pPoint1, @NonNull PointF pPoint2) {
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