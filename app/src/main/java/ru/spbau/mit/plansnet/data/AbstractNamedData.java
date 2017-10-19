package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;

/**
 * Created by kostya55516 on 19.10.17.
 */

public abstract class AbstractNamedData {
    @NonNull
    private String name;

    AbstractNamedData(@NonNull final String name) {
        this.name = name;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }
}
