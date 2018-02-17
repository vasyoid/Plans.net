package ru.spbau.mit.plansnet.constructor.objects;

import android.support.annotation.NonNull;

import org.andengine.opengl.texture.region.ITextureRegion;

import ru.spbau.mit.plansnet.data.objects.Wall;

public class WallSprite extends MapObjectLinear {

    private static ITextureRegion textureRegion;

    public WallSprite() {
        super(textureRegion);
    }

    public WallSprite(@NonNull Wall pWall) {
        super(textureRegion);
        setPosition(pWall.getX(), pWall.getY(), pWall.getX2(), pWall.getY2());
    }

    public static void setTexture(@NonNull ITextureRegion pTexture) {
        textureRegion = pTexture;
    }

}
