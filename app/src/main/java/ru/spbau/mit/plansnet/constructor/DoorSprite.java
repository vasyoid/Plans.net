package ru.spbau.mit.plansnet.constructor;

import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import ru.spbau.mit.plansnet.data.objects.Door;

import static ru.spbau.mit.plansnet.constructor.ConstructorActivity.ActionState.DEL;

public class DoorSprite extends MapObjectLinear {

    private static ITextureRegion textureRegion;

    public DoorSprite() {
        super(textureRegion);
    }

    public DoorSprite(Door pDoor) {
        super(textureRegion);
        setPosition(pDoor.getX(), pDoor.getY(), pDoor.getX2(), pDoor.getY2());
    }

    public MapObjectSprite copy() {
        MapObjectSprite result = new DoorSprite();
        result.setPosition(getPosition());
        return result;
    }

    public static void setTexture(ITextureRegion texture) {
        textureRegion = texture;
    }


    @Override
    public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {

        if (MAP.getTouchState() != DEL) {
            return false;
        } else {
            detachSelf();
            MAP.removeObject(this);
        }
        return true;
    }
}
