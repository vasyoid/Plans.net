package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;

/**
 * Created by kostya55516 on 14.10.17.
 */

public class Account extends AbstractDataContainer<UsersGroup> {
    private String ID;
    public String getID() {
        return ID;
    }

    public Account(@NonNull final String name, @NonNull final String ID) {
        super(name);
        this.ID = ID;
    }
}
