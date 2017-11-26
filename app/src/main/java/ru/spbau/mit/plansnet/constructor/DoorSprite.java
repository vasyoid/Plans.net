package ru.spbau.mit.plansnet.constructor;

import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

public class DoorSprite extends MapObjectLinear {

    private static ITextureRegion textureRegion;

    public MapObjectSprite copy() {
        MapObjectSprite result = new DoorSprite(getVertexBufferObjectManager());
        result.setPosition(getPosition());
        return result;
    }

    public DoorSprite(VertexBufferObjectManager vertexBufferObjectManager) {
        super(textureRegion, vertexBufferObjectManager);
    }

    public static void setTexture(ITextureRegion texture) {
        textureRegion = texture;
    }

    @Override
    public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {

        if (MAP.getTouchState() != 1) {
            return false;
        } else {
            detachSelf();
            MAP.removeObject(this);
        }
        return true;
    }
}
