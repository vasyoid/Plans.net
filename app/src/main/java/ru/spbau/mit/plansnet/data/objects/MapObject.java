package ru.spbau.mit.plansnet.data.objects;

import java.io.Serializable;

/**
 * Parent class for objects on map.
 */

public class MapObject implements Serializable {

    public static class PointF implements Serializable {
        private float y;
        private float x;

        PointF(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }
    }

}

