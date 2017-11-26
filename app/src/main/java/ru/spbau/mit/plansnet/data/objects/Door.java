package ru.spbau.mit.plansnet.data.objects;

import android.support.annotation.NonNull;

import java.io.Serializable;

import ru.spbau.mit.plansnet.constructor.MapObjectLinear;

/**
 * Created by kostya55516 on 26.11.17.
 */

public class Door extends LinearObject implements Serializable {
    public Door(@NonNull final MapObjectLinear obj) {
        super(obj);
    }
}
