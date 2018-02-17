package ru.spbau.mit.plansnet.data.objects;

import android.support.annotation.NonNull;

import org.andengine.entity.primitive.Line;

import java.io.Serializable;

import ru.spbau.mit.plansnet.constructor.objects.MapObjectLinear;

/**
 * Created by kostya55516 on 26.11.17.
 */

public class LinearObject extends MapObject implements Serializable {
    private PointF coord;
    private PointF secondCoord;

    public LinearObject(@NonNull final MapObjectLinear object) {
        Line objCoord = object.getmPosition();

        coord = new PointF(objCoord.getX1(), objCoord.getY1());
        secondCoord = new PointF(objCoord.getX2(), objCoord.getY2());
    }

    public float getX() {
        return coord.getX();
    }

    public float getY() {
        return coord.getY();
    }

    public float getX2() {
        return secondCoord.getX();
    }

    public float getY2() {
        return secondCoord.getY();
    }

}
