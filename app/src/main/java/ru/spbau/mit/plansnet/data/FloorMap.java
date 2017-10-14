package ru.spbau.mit.plansnet.data;

import android.support.annotation.Nullable;
import android.support.annotation.NonNull;

import java.util.ArrayList;

/**
 * Created by kostya55516 on 14.10.17.
 */

public class FloorMap {
    @NonNull
    private String name;
    @NonNull
    private ArrayList<MapObject> objects;

    FloorMap(@NonNull String name) {
        this.name = name;
        objects = new ArrayList<>();
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public ArrayList<MapObject> getObjects() {
        return objects;
    }
}
