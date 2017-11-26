package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;

import java.io.Serializable;

/**
 * Created by kostya55516 on 19.10.17.
 */

public abstract class AbstractNamedData implements Serializable {
    @NonNull
    private String name;

    AbstractNamedData() {
        name = "default";
    }

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
