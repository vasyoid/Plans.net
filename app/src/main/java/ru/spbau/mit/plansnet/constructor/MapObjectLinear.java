package ru.spbau.mit.plansnet.constructor;

import org.andengine.entity.primitive.Line;
import org.andengine.opengl.texture.region.ITextureRegion;

public abstract class MapObjectLinear extends MapObjectSprite {

    private static int THICKNESS = 10;
    private Line position;

    public MapObjectLinear(ITextureRegion pTextureRegion) {
        super(pTextureRegion, vertexBufferObjectManager);
        setHeight(THICKNESS);
    }

    public static void setThickness(int thickness) {
        THICKNESS = thickness;
    }

    public void setPosition(Line line) {
        position = Geometry.copy(line);
        float angle = (float)Math.atan2(line.getY2() - line.getY1(), line.getX2() - line.getX1());
        float length = (float)Math.sqrt(Math.pow(line.getY2() - line.getY1(), 2) +
                Math.pow(line.getX2() - line.getX1(), 2));
        setWidth(length);
        setPosition(line.getX1() + (float)Math.sin(angle) * getHeight() / 2,
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

}
