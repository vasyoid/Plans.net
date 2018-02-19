package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;

import java.io.Serializable;

public abstract class AbstractNamedData implements Serializable {
    @NonNull
    private String name;

    public AbstractNamedData() {
        name = "default";
    }

    public AbstractNamedData(@NonNull final String name) {
        this.name = name;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return getName();
    }
}
