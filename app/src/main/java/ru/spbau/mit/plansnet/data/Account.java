package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for user accounts
 */

public class Account extends AbstractDataContainer<UsersGroup> {
    private String ID;
    @NonNull
    private final Map<String, UsersGroup> downloadedGroups = new HashMap<>();

    @NonNull
    public Map<String, UsersGroup> getDownloadedGroups() {
        return downloadedGroups;
    }

    @NonNull
    public List<String> getListOfDownloadedGroupsNames() {
        ArrayList<String> list = new ArrayList<>();
        for (Object name : downloadedGroups.keySet().toArray())  {
            list.add((String) name);
        }
        return list;
    }

    /**
     * Adds an element to the data
     * @return previous value or null
     */
    @NonNull
    public UsersGroup addDownloadedGroup(@NonNull UsersGroup element) {
        return downloadedGroups.put(element.getName(), element);
    }

    @Nullable
    public UsersGroup findDownloadedGroup(String groupName) {
        return downloadedGroups.get(groupName);
    }



    public String getID() {
        return ID;
    }


    public Account(@NonNull final String name, @NonNull final String ID) {
        super(name);
        this.ID = ID;
    }
}
