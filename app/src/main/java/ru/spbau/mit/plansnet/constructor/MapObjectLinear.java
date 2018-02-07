package ru.spbau.mit.plansnet.constructor;

import android.graphics.PointF;

import org.andengine.entity.primitive.Line;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;

import java.util.List;

import static ru.spbau.mit.plansnet.constructor.ConstructorActivity.ActionState.DEL;
import static ru.spbau.mit.plansnet.constructor.ConstructorActivity.ActionState.MOVE_WALL;

public abstract class MapObjectLinear extends MapObjectSprite {

    private static int THICKNESS = 10;
    private Line position;
    private PointF point1 = new PointF(), point2 = new PointF();
    private PointF firstPoint1, firstPoint2;
    private float firstTouchX, firstTouchY;
    private List<RoomSprite> roomsToRemove;

    public MapObjectLinear(ITextureRegion pTextureRegion) {
        super(pTextureRegion, vertexBufferObjectManager);
        setHeight(THICKNESS);
    }

    public static void setThickness(int thickness) {
        THICKNESS = thickness;
    }

    public void changeDirection() {
        PointF tmp = point1;
        point1 = point2;
        point2 = tmp;
        setPosition(point1.x, point1.y, point2.x, point2.y);
    }

    public PointF getPoint1() {
        return point1;
    }

    public PointF getPoint2() {
        return point2;
    }

    public void setPoint1(PointF point) {
        setPosition(point.x, point.y, point2.x, point2.y);
    }

    public void setPoint2(PointF point) {
        setPosition(point1.x, point1.y, point.x, point.y);
    }

    public void setPosition(Line line) {
        position = Geometry.copy(line);
        point1.set(line.getX1(), line.getY1());
        point2.set(line.getX2(), line.getY2());
        float angle = (float)Math.atan2(line.getY2() - line.getY1(), line.getX2() - line.getX1());
        float length = (float)Math.sqrt(Math.pow(line.getY2() - line.getY1(), 2) +
                Math.pow(line.getX2() - line.getX1(), 2));
        setWidth(length);
        super.setPosition(line.getX1() + (float)Math.sin(angle) * getHeight() / 2,
                line.getY1() - (float)Math.cos(angle) * getHeight() / 2);
        setRotationCenter(0, 0);
        setRotation((float)Math.toDegrees(angle));
    }

    public void setPosition(float pX1, float pY1, float pX2, float pY2) {
        setPosition(new Line(pX1, pY1, pX2, pY2, null));
    }

    public Line getPosition() {
        return position;
    }

    @Override
    public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
        if (MAP.getTouchState() == DEL) {
            MAP.removeObject(this);
            MAP.removeRoomsBySection(point1, point2);
        } else if (MAP.getTouchState() == MOVE_WALL) {
            float currentTouchX = Math.round(pSceneTouchEvent.getX() / Map.getGridSize()) * Map.getGridSize();
            float currentTouchY = Math.round(pSceneTouchEvent.getY() / Map.getGridSize()) * Map.getGridSize();
            PointF currentPoint = new PointF(currentTouchX, currentTouchY);
            switch (pSceneTouchEvent.getAction()) {
                case TouchEvent.ACTION_DOWN:
                    if (currentPoint.equals(point1) || currentPoint.equals(point2)) {
                        return false;
                    }
                    firstPoint1 = new PointF(point1.x, point1.y);
                    firstPoint2 = new PointF(point2.x, point2.y);
                    firstTouchX = currentTouchX;
                    firstTouchY = currentTouchY;
                    roomsToRemove = MAP.findRoomsBySection(point1, point2);
                    setScale(1.0f, 1.4f);
                    return true;
                case TouchEvent.ACTION_MOVE:
                    if (firstPoint1 != null) {
                        PointF currentPoint1 = new PointF(firstPoint1.x + currentTouchX - firstTouchX,
                                firstPoint1.y + currentTouchY - firstTouchY);
                        PointF currentPoint2 = new PointF(firstPoint2.x + currentTouchX - firstTouchX,
                                firstPoint2.y + currentTouchY - firstTouchY);
                        if (Geometry.isPointInsidePolygon(Map.getGridPolygon(), currentPoint1) &&
                                Geometry.isPointInsidePolygon(Map.getGridPolygon(), currentPoint2)) {
                            setPoint1(currentPoint1);
                            setPoint2(currentPoint2);
                        }
                        return true;
                    } else {
                        return false;
                    }
                case TouchEvent.ACTION_UP:
                    if (firstPoint1 != null) {
                        if (MAP.hasIntersections(this)) {
                            setPoint1(firstPoint1);
                            setPoint2(firstPoint2);
                        }
                        if (!point1.equals(firstPoint1)) {
                            for (RoomSprite room : roomsToRemove) {
                                MAP.removeRoom(room);
                            }
                        }
                        MAP.updateMovedObject(firstPoint1, firstPoint2, this);
                        setScale(1.0f);
                        firstPoint1 = firstPoint2 = null;
                    }
                    return false;
                default:
                    break;
            }
        }
        return false;
    }
}
