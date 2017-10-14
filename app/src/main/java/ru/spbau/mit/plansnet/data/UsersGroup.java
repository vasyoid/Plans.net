package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Created by kostya55516 on 14.10.17.
 */

public class UsersGroup {
    private boolean isPrivate;

    @NotNull
    private String name;
    @NonNull
    private ArrayList<Building> buildings;

    UsersGroup(@NonNull String name) {
        this.name = name;
        buildings = new ArrayList<>();
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public ArrayList<Building> getBuildings() {
        return buildings;
    }

    /**
     * Adds a building to the users group
     * @param building a building to adding
     * @return an added building
     */
    @NonNull
    public Building addBuilding(@NonNull Building building) {
        buildings.add(building);
        return buildings.get(buildings.size() - 1);
    }

    /**
     * Searches a building by name
     * @param buildingName name of a building for searching
     * @return a building if it have found or null otherwise
     */
    @Nullable
    public Building findByName(String buildingName) {
        for (Building building : buildings) {
            if (building.getName().equals(buildingName)) {
                return building;
            }
        }

        return null;
    }
}
