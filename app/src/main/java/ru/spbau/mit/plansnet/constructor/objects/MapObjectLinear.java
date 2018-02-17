package ru.spbau.mit.plansnet.constructor.objects;

import android.graphics.PointF;
import android.support.annotation.NonNull;

import org.andengine.entity.primitive.Line;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;

import java.util.List;

import ru.spbau.mit.plansnet.constructor.Geometry;
import ru.spbau.mit.plansnet.constructor.Map;

import static ru.spbau.mit.plansnet.constructor.constructorController.ConstructorActivity.ActionState.DEL;
import static ru.spbau.mit.plansnet.constructor.constructorController.ConstructorActivity.ActionState.MOVE_OBJECT;

public abstract class MapObjectLinear extends MapObjectSprite {

    private static final int THICKNESS = 100;

    private Line mPosition;
    private PointF mPoint1 = new PointF(), mPoint2 = new PointF();
    private PointF mFirstPoint1, mFirstPoint2;
    private float mFirstTouchX, mFirstTouchY;
    private List<RoomSprite> mRoomsToRemove;

    public MapObjectLinear(@NonNull ITextureRegion pTextureRegion) {
        super(pTextureRegion, vertexBufferObjectManager);
        setHeight(THICKNESS);
    }

    public void changeDirection() {
        PointF tmp = mPoint1;
        mPoint1 = mPoint2;
        mPoint2 = tmp;
        setPosition(mPoint1.x, mPoint1.y, mPoint2.x, mPoint2.y);
    }

    public @NonNull PointF getPoint1() {
        return mPoint1;
    }

    public @NonNull PointF getPoint2() {
        return mPoint2;
    }

    public void setPoint1(@NonNull PointF pPoint) {
        setPosition(pPoint.x, pPoint.y, mPoint2.x, mPoint2.y);
    }

    public void setPoint2(@NonNull PointF pPoint) {
        setPosition(mPoint1.x, mPoint1.y, pPoint.x, pPoint.y);
    }

    public void setObjectPosition(@NonNull Line pLine) {
        mPosition = Geometry.copy(pLine);
        mPoint1.set(pLine.getX1(), pLine.getY1());
        mPoint2.set(pLine.getX2(), pLine.getY2());
        float angle = (float)Math.atan2(pLine.getY2() - pLine.getY1(),
                pLine.getX2() - pLine.getX1());
        float length = (float)Math.sqrt(Math.pow(pLine.getY2() - pLine.getY1(), 2) +
                Math.pow(pLine.getX2() - pLine.getX1(), 2));
        setWidth(length);
        super.setPosition(pLine.getX1() + (float)Math.sin(angle) * getHeight() / 2,
                pLine.getY1() - (float)Math.cos(angle) * getHeight() / 2);
        setRotationCenter(0, 0);
        setRotation((float)Math.toDegrees(angle));
    }

    public void setPosition(float pX1, float pY1, float pX2, float pY2) {
        setObjectPosition(new Line(pX1, pY1, pX2, pY2, null));
    }

    public @NonNull Line getPosition() {
        return mPosition;
    }

    @Override
    public boolean onAreaTouched(@NonNull TouchEvent pSceneTouchEvent,
                                 float pTouchAreaLocalX,
                                 float pTouchAreaLocalY) {
        if (MAP.getTouchState() == DEL) {
            MAP.removeObject(this);
            MAP.removeRoomsBySection(mPoint1, mPoint2);
            return false;
        }
        if (MAP.getTouchState() != MOVE_OBJECT) {
            return false;
        }
        float currentTouchX = Math.round(pSceneTouchEvent.getX() / Map.getGridSize())
                * Map.getGridSize();
        float currentTouchY = Math.round(pSceneTouchEvent.getY() / Map.getGridSize())
                * Map.getGridSize();
        switch (pSceneTouchEvent.getAction()) {
            case TouchEvent.ACTION_DOWN:
                PointF currentPoint = new PointF(pSceneTouchEvent.getX(),
                        pSceneTouchEvent.getY());
                if (Geometry.distance(currentPoint, mPoint1) < THICKNESS ||
                        Geometry.distance(currentPoint, mPoint2) < THICKNESS) {
                    return false;
                }
                mFirstPoint1 = new PointF(mPoint1.x, mPoint1.y);
                mFirstPoint2 = new PointF(mPoint2.x, mPoint2.y);
                mFirstTouchX = currentTouchX;
                mFirstTouchY = currentTouchY;
                mRoomsToRemove = MAP.findRoomsBySection(mPoint1, mPoint2);
                setScale(1.0f, 1.4f);
                return true;
            case TouchEvent.ACTION_MOVE:
                if (mFirstPoint1 == null) {
                    return false;
                }
                PointF currentPoint1 = new PointF(mFirstPoint1.x + currentTouchX - mFirstTouchX,
                        mFirstPoint1.y + currentTouchY - mFirstTouchY);
                PointF currentPoint2 = new PointF(mFirstPoint2.x + currentTouchX - mFirstTouchX,
                        mFirstPoint2.y + currentTouchY - mFirstTouchY);
                if (Geometry.isPointInsidePolygon(Map.getGridPolygon(), currentPoint1) &&
                        Geometry.isPointInsidePolygon(Map.getGridPolygon(), currentPoint2)) {
                    setPoint1(currentPoint1);
                    setPoint2(currentPoint2);
                }
                return true;
            case TouchEvent.ACTION_UP:
                if (mFirstPoint1 == null) {
                    return false;
                }
                if (MAP.hasIntersections(this)) {
                    setPoint1(mFirstPoint1);
                    setPoint2(mFirstPoint2);
                }
                if (!mPoint1.equals(mFirstPoint1)) {
                    for (RoomSprite room : mRoomsToRemove) {
                        MAP.removeRoom(room);
                    }
                }
                MAP.updateMovedObject(mFirstPoint1, mFirstPoint2, this);
                setScale(1.0f);
                mFirstPoint1 = mFirstPoint2 = null;
                return false;
            default:
                return false;
        }
    }

}
