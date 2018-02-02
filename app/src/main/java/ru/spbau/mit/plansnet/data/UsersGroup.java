package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;

/**
 * Created by kostya55516 on 14.10.17.
 */

public class UsersGroup extends AbstractDataContainer<Building> {
    private boolean isPrivate;

    public UsersGroup(@NonNull String name) {
        super(name);
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}
