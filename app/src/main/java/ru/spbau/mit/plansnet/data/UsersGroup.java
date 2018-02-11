package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;

/**
 *  Class for users group
 */

public class UsersGroup extends AbstractDataContainer<Building> {
    private boolean isPrivate;
    @NonNull
    private String visibleName;

    public UsersGroup(@NonNull String name) {
        super(name);
        visibleName = name;
    }

    public void setVisibleName(@NonNull String visibleName) {
        this.visibleName = visibleName;
    }

    @Override
    public String toString() {
        return visibleName;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}
