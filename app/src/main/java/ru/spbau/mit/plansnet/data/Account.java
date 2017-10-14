package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

/**
 * Created by kostya55516 on 14.10.17.
 */

public class Account {
    @NonNull
    private String name;
    @NonNull
    private ArrayList<UsersGroup> groups;

    public Account(@NonNull String name) {
        this.name = name;
        groups = new ArrayList<>();
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public ArrayList<UsersGroup> getGroups() {
        return groups;
    }

    /**
     * Adds a group to the users group
     *
     * @param group a group to adding
     * @return an added group
     */
    @NonNull
    public UsersGroup addBuilding(@NonNull UsersGroup group) {
        groups.add(group);
        return groups.get(groups.size() - 1);
    }

    /**
     * Searches a group by name
     *
     * @param groupName name of a group for searching
     * @return a group if it have found or null otherwise
     */
    @Nullable
    public UsersGroup findByName(String groupName) {
        for (UsersGroup group : groups) {
            if (group.getName().equals(groupName)) {
                return group;
            }
        }

        return null;
    }
}
