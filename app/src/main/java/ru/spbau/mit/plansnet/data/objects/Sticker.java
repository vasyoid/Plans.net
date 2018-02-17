package ru.spbau.mit.plansnet.data.objects;

import java.io.Serializable;

import ru.spbau.mit.plansnet.constructor.objects.StickerSprite;

public class Sticker extends MapObject implements Serializable {
    private int type;
    private PointF position;
    private float size;

    public Sticker(StickerSprite obj) {
        type = obj.getType();
        position = new PointF(obj.getmPosition().x, obj.getmPosition().y);
        size = obj.getmSize();
    }

    public int getType() {
        return type;
    }

    public PointF getPosition() {
        return position;
    }

    public float getSize() {
        return size;
    }
}
