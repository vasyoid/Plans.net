package ru.spbau.mit.plansnet.data.objects;

import android.support.annotation.NonNull;

import org.andengine.util.color.Color;

import java.io.Serializable;

import ru.spbau.mit.plansnet.constructor.RoomSprite;

/**
 * Created by kostya55516 on 26.11.17.
 */

public class Room extends MapObject implements Serializable {
    private int x;
    private int y;
    private Color color;
    private CharSequence title;
    private CharSequence description;
    public Room(@NonNull final RoomSprite room) {
        x = (int) room.getInitialX();
        y = (int) room.getInitialY();
        color = room.getColor();
        title = room.getTitle();
        description = room.getDescription();
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

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
