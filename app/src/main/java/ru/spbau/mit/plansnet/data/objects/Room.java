package ru.spbau.mit.plansnet.data.objects;

import android.support.annotation.NonNull;
import android.util.Log;

import org.andengine.util.color.Color;

import java.io.Serializable;

import ru.spbau.mit.plansnet.constructor.objects.RoomSprite;

/**
 * Room class for map
 */

public class Room extends MapObject implements Serializable {
    private float x;
    private float y;
    private Color color;
    private CharSequence title;
    private CharSequence description;
    public Room(@NonNull final RoomSprite room) {
        x = (int) room.getmInitialX();
        y = (int) room.getmInitialY();
        color = room.getColor();
        title = room.getmTitle();
        description = room.getmDescription();
        Log.d("VASYOID", "Room.color: " + color.toString());
    }

    public CharSequence getDescription() {
        return description;
    }

    public CharSequence getTitle() {

        return title;
    }

    public Color getColor() {

        return color;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
