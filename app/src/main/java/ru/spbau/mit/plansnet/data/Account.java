package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;

/**
 * Created by kostya55516 on 14.10.17.
 */

public class Account extends AbstractDataContainer<UsersGroup> {
    private Object ID;

    public Object getID() {
        return ID;
    }

    public Account(@NonNull String name) {
        super(name);
    }
}
