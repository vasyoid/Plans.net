package ru.spbau.mit.plansnet.data.objects;

import android.support.annotation.NonNull;

import java.io.Serializable;

import ru.spbau.mit.plansnet.constructor.MapObjectLinear;

public class Window extends LinearObject implements Serializable {
    public Window(@NonNull final MapObjectLinear obj) {
        super(obj);
    }
}
