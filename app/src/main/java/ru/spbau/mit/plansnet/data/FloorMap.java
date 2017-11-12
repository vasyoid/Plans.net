package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kostya55516 on 14.10.17.
 */

public class FloorMap extends AbstractNamedData {
    private ArrayList<MapObject> objects;

    FloorMap(@NonNull String name) {
        super(name);
    }


    @NonNull
    public List<MapObject> getArrayData() {
        return objects;
    }

    public MapObject addData(MapObject element) {
        objects.add(element);
        return objects.get(objects.size() - 1);
    }
}
