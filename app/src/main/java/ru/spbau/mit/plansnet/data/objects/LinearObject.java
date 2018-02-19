package ru.spbau.mit.plansnet.data.objects;

import android.support.annotation.NonNull;

import org.andengine.entity.primitive.Line;

import java.io.Serializable;

import ru.spbau.mit.plansnet.constructor.objects.MapObjectLinear;

public class LinearObject extends MapObject implements Serializable {
    private PointF p1, p2;

    public LinearObject(@NonNull final MapObjectLinear object) {
        Line objCoord = object.getPosition();

        p1 = new PointF(objCoord.getX1(), objCoord.getY1());
        p2 = new PointF(objCoord.getX2(), objCoord.getY2());
    }

    public float getX() {
        return p1.getX();
    }

    public float getY() {
        return p1.getY();
    }

    public float getX2() {
        return p2.getX();
    }

    public float getY2() {
        return p2.getY();
    }

}
