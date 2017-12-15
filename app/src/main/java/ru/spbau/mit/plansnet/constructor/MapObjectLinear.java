package ru.spbau.mit.plansnet.constructor;

import android.graphics.PointF;

import org.andengine.entity.primitive.Line;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;

import static ru.spbau.mit.plansnet.constructor.ConstructorActivity.ActionState.DEL;

public abstract class MapObjectLinear extends MapObjectSprite {

    private static int THICKNESS = 10;
    private Line position;
    private PointF point1, point2;

    public MapObjectLinear(ITextureRegion pTextureRegion) {
        super(pTextureRegion, vertexBufferObjectManager);
        point1 = new PointF();
        point2 = new PointF();
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
            MAP.removeRoomsByObject(this);
        }
        return false;
    }
}
