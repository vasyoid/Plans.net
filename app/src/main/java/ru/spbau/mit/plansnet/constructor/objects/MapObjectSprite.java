package ru.spbau.mit.plansnet.constructor.objects;

import android.support.annotation.NonNull;

import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import ru.spbau.mit.plansnet.constructor.Map;

/**
 * Base class for all map objects.
 */
public abstract class MapObjectSprite extends Sprite {

    protected static Map MAP;
    protected static VertexBufferObjectManager vertexBufferObjectManager;

    /**
     * VertexBufferObjectManager setter.
     * @param pVertexBufferObjectManager value to set.
     */
    public static void setVertexBufferObjectManager(@NonNull VertexBufferObjectManager
                                                            pVertexBufferObjectManager) {
        vertexBufferObjectManager = pVertexBufferObjectManager;
    }

    /**
     * Constructor taking a texture.
     * @param pTextureRegion texture of the object.
     * @param pVertexBufferObjectManager vertex buffer object manager.
     */
    public MapObjectSprite(@NonNull ITextureRegion pTextureRegion,
                           @NonNull VertexBufferObjectManager pVertexBufferObjectManager) {
        this(0, 0, pTextureRegion, pVertexBufferObjectManager);
    }

    /**
     * Constructor taking a position and a texture.
     * @param pX position x.
     * @param pY position y.
     * @param pTextureRegion texture of the object.
     * @param pVertexBufferObjectManager vertex buffer object manager.
     */
    public MapObjectSprite(float pX,
                           float pY,
                           @NonNull ITextureRegion pTextureRegion,
                           @NonNull VertexBufferObjectManager pVertexBufferObjectManager) {
        super(pX, pY, pTextureRegion, pVertexBufferObjectManager);
    }

    /**
     * map setter.
     * @param pMap value to set.
     */
    public static void setMap(@NonNull Map pMap) {
        MAP = pMap;
    }

}
