package ru.spbau.mit.plansnet.constructor.objects;


import android.support.annotation.NonNull;

import org.andengine.opengl.texture.region.ITextureRegion;

import ru.spbau.mit.plansnet.data.objects.Window;

public class WindowSprite extends MapObjectLinear {

    private static ITextureRegion textureRegion;

    public WindowSprite() {
        super(textureRegion);
    }

    public WindowSprite(@NonNull Window pWindow) {
        super(textureRegion);
        setPosition(pWindow.getX(), pWindow.getY(), pWindow.getX2(), pWindow.getY2());
    }

    public static void setTexture(@NonNull ITextureRegion pTexture) {
        textureRegion = pTexture;
    }

}
