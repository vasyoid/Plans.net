package ru.spbau.mit.plansnet.data.objects;

import android.support.annotation.NonNull;

import org.andengine.entity.primitive.Line;

import java.io.Serializable;

import ru.spbau.mit.plansnet.constructor.MapObjectLinear;

/**
 * Created by kostya55516 on 26.11.17.
 */

public class LinearObject extends MapObject implements Serializable {
    private Point coord;
    private Point secondCoord;

    public LinearObject(@NonNull final MapObjectLinear object) {
        Line objCoord = object.getPosition();
        coord.x = (int)objCoord.getX1();
        coord.y = (int)objCoord.getY1();

        secondCoord.x = (int)objCoord.getX2();
        secondCoord.y = (int)objCoord.getY2();
    }

    public int getX() {
        return coord.x;
    }

    public int getY() {
        return coord.y;
    }

    public int getX2() {
        return secondCoord.x;
    }

    public int getY2() {
        return secondCoord.y;
    }

    private static class Point implements Serializable {
        private int y;
        private int x;
    }
}
