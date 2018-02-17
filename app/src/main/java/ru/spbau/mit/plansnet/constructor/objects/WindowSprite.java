package ru.spbau.mit.plansnet.constructor.objects;


import android.support.annotation.NonNull;

import org.andengine.opengl.texture.region.ITextureRegion;

import ru.spbau.mit.plansnet.data.objects.Window;

/**
 * Sprite for Window elements.
 */
public class WindowSprite extends MapObjectLinear {

    private static ITextureRegion textureRegion;

    /**
     * Void constructor.
     */
    public WindowSprite() {
        super(textureRegion);
    }

    /**
     * Constructor that uses data.Window element.
     * Used when a Map object is initialised to fit a FloorMap object.
     * @param pWindow data.Window element.
     */
    public WindowSprite(@NonNull Window pWindow) {
        super(textureRegion);
        setPosition(pWindow.getX(), pWindow.getY(), pWindow.getX2(), pWindow.getY2());
    }

    /**
     * Texture setter.
     * @param pTexture texture that will be set to all windows on the map.
     * Used only once when the activity is initialized.
     */
    public static void setTexture(@NonNull ITextureRegion pTexture) {
        textureRegion = pTexture;
    }

}
