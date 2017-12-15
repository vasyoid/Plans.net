package ru.spbau.mit.plansnet.constructor;

import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;

import ru.spbau.mit.plansnet.data.objects.Wall;

import static ru.spbau.mit.plansnet.constructor.ConstructorActivity.ActionState.DEL;

public class WallSprite extends MapObjectLinear {

    private static ITextureRegion textureRegion;

    public MapObjectSprite copy() {
        MapObjectSprite result = new WallSprite();
        result.setPosition(getPosition());
        return result;
    }

    public WallSprite() {
        super(textureRegion);
    }

    public WallSprite(Wall pWall) {
        super(textureRegion);
        setPosition(pWall.getX(), pWall.getY(), pWall.getX2(), pWall.getY2());
    }

    public static void setTexture(ITextureRegion texture) {
        textureRegion = texture;
    }

    @Override
    public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
        if (MAP.getTouchState() != DEL) {
            return false;
        } else {
            MAP.removeObject(this);
        }
        return false;
    }
}
