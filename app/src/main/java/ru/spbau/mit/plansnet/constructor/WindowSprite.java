package ru.spbau.mit.plansnet.constructor;


import org.andengine.opengl.texture.region.ITextureRegion;

import ru.spbau.mit.plansnet.data.objects.Window;

public class WindowSprite extends MapObjectLinear {

    private static ITextureRegion textureRegion;

    public MapObjectSprite copy() {
        MapObjectSprite result = new WindowSprite();
        result.setPosition(getPosition());
        return result;
    }

    public WindowSprite() {
        super(textureRegion);
    }

    public WindowSprite(Window pWindow) {
        super(textureRegion);
        setPosition(pWindow.getX(), pWindow.getY(), pWindow.getX2(), pWindow.getY2());
    }

    public static void setTexture(ITextureRegion texture) {
        textureRegion = texture;
    }

}
