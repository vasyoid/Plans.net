package ru.spbau.mit.plansnet.constructor;

import android.graphics.Point;
import android.graphics.PointF;

import org.andengine.entity.primitive.Line;
import org.andengine.entity.sprite.Sprite;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Scanner;

public abstract class MapObject extends Sprite {

    protected static Map MAP;

    public abstract MapObject copy();

    public MapObject(ITextureRegion pTextureRegion, VertexBufferObjectManager pVertexBufferObjectManager) {
        this(0, 0, pTextureRegion, pVertexBufferObjectManager);
    }

    public MapObject(float pX, float pY, ITextureRegion pTextureRegion, VertexBufferObjectManager pVertexBufferObjectManager) {
        super(pX, pY, pTextureRegion, pVertexBufferObjectManager);
    }

    public static void setMap(Map map) {
        MAP = map;
    }

}
