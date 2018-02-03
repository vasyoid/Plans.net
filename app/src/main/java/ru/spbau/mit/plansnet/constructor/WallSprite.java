package ru.spbau.mit.plansnet.constructor;

import org.andengine.opengl.texture.region.ITextureRegion;

import ru.spbau.mit.plansnet.data.objects.Wall;

public class WallSprite extends MapObjectLinear {

    private static ITextureRegion textureRegion;

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

}
