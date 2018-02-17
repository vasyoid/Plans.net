package ru.spbau.mit.plansnet.constructor.objects;

import android.support.annotation.NonNull;

import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import ru.spbau.mit.plansnet.constructor.Map;

public abstract class MapObjectSprite extends Sprite {

    protected static Map MAP;
    protected static VertexBufferObjectManager vertexBufferObjectManager;

    public static void setVertexBufferObjectManager(@NonNull VertexBufferObjectManager
                                                            pVertexBufferObjectManager) {
        vertexBufferObjectManager = pVertexBufferObjectManager;
    }

    public MapObjectSprite(@NonNull ITextureRegion pTextureRegion,
                           @NonNull VertexBufferObjectManager pVertexBufferObjectManager) {
        this(0, 0, pTextureRegion, pVertexBufferObjectManager);
    }

    public MapObjectSprite(float pX,
                           float pY,
                           @NonNull ITextureRegion pTextureRegion,
                           @NonNull VertexBufferObjectManager pVertexBufferObjectManager) {
        super(pX, pY, pTextureRegion, pVertexBufferObjectManager);
    }

    public static void setMap(@NonNull Map pMap) {
        MAP = pMap;
    }

}
