package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;

/**
 *  Class for users group
 */

public class UsersGroup extends AbstractDataContainer<Building> {
    private boolean isPrivate = false;
    private boolean isEditable = false;

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

    public boolean isEditable() {
        return isEditable;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public void setEditable(boolean isEditable) {
        this.isEditable = isEditable;
    }
}
