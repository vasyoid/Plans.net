package ru.spbau.mit.plansnet.data.objects;

import android.support.annotation.NonNull;

import java.io.Serializable;

import ru.spbau.mit.plansnet.constructor.RoomSprite;

/**
 * Created by kostya55516 on 26.11.17.
 */

public class Room extends MapObject implements Serializable {
    private int x;
    private int y;
    public Room(@NonNull final RoomSprite room) {
        x = (int) room.getInitialX();
        y = (int) room.getInitialY();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
