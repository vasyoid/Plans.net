package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

/**
 * Created by kostya55516 on 14.10.17.
 */

public class Building {
    @NonNull
    private String name;
    @NonNull
    private ArrayList<FloorMap> floors;

    Building(@NonNull String name) {
        this.name = name;
        floors = new ArrayList<>();
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @Nullable
    public ArrayList<FloorMap> getFloors() {
        return floors;
    }

    /**
     * Adds a floor to the building
     * @param map a floor to adding
     * @return an added floor
     */
    @NonNull
    public FloorMap addFloorMap(@NonNull FloorMap map) {
        floors.add(map);
        return floors.get(floors.size() - 1);
    }

    /**
     * Searches a floor by name
     * @param floorName name of a floor for searching
     * @return a floor if it have found or null otherwise
     */
    @Nullable
    public FloorMap findByName(String floorName) {
        for (FloorMap floor : floors) {
            if (floor.getName().equals(floorName)) {
                return floor;
            }
        }

        return null;
    }

}
