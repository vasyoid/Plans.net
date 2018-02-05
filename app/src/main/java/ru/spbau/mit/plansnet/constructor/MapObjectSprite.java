package ru.spbau.mit.plansnet.constructor;

import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

public abstract class MapObjectSprite extends Sprite {

    protected static Map MAP;
    protected static VertexBufferObjectManager vertexBufferObjectManager;

    public static void setVertexBufferObjectManager(VertexBufferObjectManager
                                                            pVertexBufferObjectManager) {
        vertexBufferObjectManager = pVertexBufferObjectManager;
    }

    public MapObjectSprite(ITextureRegion pTextureRegion, VertexBufferObjectManager pVertexBufferObjectManager) {
        this(0, 0, pTextureRegion, pVertexBufferObjectManager);
    }

    public MapObjectSprite(float pX, float pY, ITextureRegion pTextureRegion, VertexBufferObjectManager pVertexBufferObjectManager) {
        super(pX, pY, pTextureRegion, pVertexBufferObjectManager);
    }

    public static void setMap(Map map) {
        MAP = map;
    }

}
