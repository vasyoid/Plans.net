package ru.spbau.mit.plansnet.constructor.objects;

import android.support.annotation.NonNull;

import org.andengine.opengl.texture.region.ITextureRegion;

import ru.spbau.mit.plansnet.data.objects.Wall;

/**
 * Sprite for Door elements.
 */
public class WallSprite extends MapObjectLinear {

    private static ITextureRegion textureRegion;

    /**
     * Void constructor.
     */
    public WallSprite() {
        super(textureRegion);
    }

    /**
     * Constructor that uses data.Wall element.
     * Used when a Map object is initialised to fit a FloorMap object.
     * @param pWall data.Wall element.
     */
    public WallSprite(@NonNull Wall pWall) {
        super(textureRegion);
        setPosition(pWall.getX(), pWall.getY(), pWall.getX2(), pWall.getY2());
    }

    /**
     * Texture setter.
     * @param pTexture texture that will be set to all walls on the map.
     * Used only once when the activity is initialized.
     */
    public static void setTexture(@NonNull ITextureRegion pTexture) {
        textureRegion = pTexture;
    }

}
