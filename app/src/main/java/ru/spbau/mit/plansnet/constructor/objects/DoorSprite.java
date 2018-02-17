package ru.spbau.mit.plansnet.constructor.objects;

import android.support.annotation.NonNull;

import org.andengine.opengl.texture.region.ITextureRegion;

import ru.spbau.mit.plansnet.data.objects.Door;

/**
 * Sprite for Door elements.
 */
public class DoorSprite extends MapObjectLinear {

    private static ITextureRegion textureRegion;

    /**
     * Void constructor.
     */
    public DoorSprite() {
        super(textureRegion);
    }

    /**
     * Constructor that uses data.Door element.
     * Used when a Map object is initialised to fit a FloorMap object.
     * @param pDoor data.Door element.
     */
    public DoorSprite(@NonNull Door pDoor) {
        super(textureRegion);
        setPosition(pDoor.getX(), pDoor.getY(), pDoor.getX2(), pDoor.getY2());
    }

    /**
     * Texture setter.
     * @param pTexture texture that will be set to all doors on the map.
     * Used only once when the activity is initialized.
     */
    public static void setTexture(@NonNull ITextureRegion pTexture) {
        textureRegion = pTexture;
    }

}
