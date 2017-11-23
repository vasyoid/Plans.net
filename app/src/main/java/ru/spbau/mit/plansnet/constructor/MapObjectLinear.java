package ru.spbau.mit.plansnet.constructor;

import org.andengine.entity.primitive.Line;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

public abstract class MapObjectLinear extends MapObject {

    private static int THICKNESS = 10;
    private Line position;

    public static void setThickness(int thickness) {
        THICKNESS = thickness;
    }

    public MapObjectLinear(ITextureRegion pTextureRegion, VertexBufferObjectManager pVertexBufferObjectManager) {
        super(pTextureRegion, pVertexBufferObjectManager);
        setHeight(THICKNESS);
    }

    void setPosition(Line line) {
        position = LineHelper.copy(line);
        float angle = (float)Math.atan2(line.getY2() - line.getY1(), line.getX2() - line.getX1());
        float length = (float)Math.sqrt(Math.pow(line.getY2() - line.getY1(), 2) +
                Math.pow(line.getX2() - line.getX1(), 2));
        setWidth(length);
        setPosition(line.getX1() + (float)Math.sin(angle) * getHeight() / 2,
                line.getY1() - (float)Math.cos(angle) * getHeight() / 2);
        setRotationCenter(0, 0);
        setRotation((float)Math.toDegrees(angle));
    }

    Line getPosition() {
        return position;
    }

}
