package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kostya55516 on 14.10.17.
 */

public class FloorMap extends AbstractNamedData implements Serializable {
    @NonNull
    private ArrayList<MapObject> objects;
    @NonNull
    private String buildingName;
    @NonNull
    private String groupName;

    public FloorMap(@NonNull String name, @NonNull String groupName, @NonNull String buildingName) {
        super(name);
        objects = new ArrayList<>();

        this.groupName = groupName;
        this.buildingName = buildingName;
    }

    public void setPath(@NonNull String groupName, @NonNull String buildingName) {
        this.groupName = groupName;
        this.buildingName = buildingName;
    }

    @NonNull
    public String getBuildingName() {
        return buildingName;
    }

    @NonNull
    public String getGroupName() {
        return groupName;
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
