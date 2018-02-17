package ru.spbau.mit.plansnet.constructor.objects;

import android.support.annotation.NonNull;

import org.andengine.opengl.texture.region.ITextureRegion;

import ru.spbau.mit.plansnet.data.objects.Door;

public class DoorSprite extends MapObjectLinear {

    private static ITextureRegion textureRegion;

    public DoorSprite() {
        super(textureRegion);
    }

    public DoorSprite(@NonNull Door pDoor) {
        super(textureRegion);
        setPosition(pDoor.getX(), pDoor.getY(), pDoor.getX2(), pDoor.getY2());
    }

    public static void setTexture(@NonNull ITextureRegion pTexture) {
        textureRegion = pTexture;
    }

}
