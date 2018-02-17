package ru.spbau.mit.plansnet.data.objects;

import android.support.annotation.NonNull;

import java.io.Serializable;

import ru.spbau.mit.plansnet.constructor.objects.MapObjectLinear;

/**
 * Created by kostya55516 on 26.11.17.
 */

public class Wall extends LinearObject implements Serializable {
    public Wall(@NonNull final MapObjectLinear obj) {
        super(obj);
    }
}
