package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;

import java.util.ArrayList;

/**
 * Created by kostya55516 on 14.10.17.
 */

public class FloorMap extends AbstractNamedData {
    private ArrayList<MapObject> objects;

    FloorMap(@NonNull String name) {
        super(name);
    }


    @NonNull
    public ArrayList<MapObject> getObjects() {
        return objects;
    }
}
