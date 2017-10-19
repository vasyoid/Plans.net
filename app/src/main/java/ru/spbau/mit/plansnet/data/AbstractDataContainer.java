package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic abstract class which contains ArrayList of type T and works with this data
 * @param <T> type of data which will be contains
 */
public abstract class AbstractDataContainer<T extends AbstractNamedData> extends AbstractNamedData {
    @NonNull
    private final ArrayList<T> data;

    public AbstractDataContainer(@NonNull String name) {
        super(name);
        data = new ArrayList<>();
    }

    @NonNull

    public List<T> getArrayOfData() {
        return data;
    }

    /**
     * Adds an element to the data
     *
     * @param element an element to adding
     * @return an added element
     */
    @NonNull
    public T addData(@NonNull T element) {
        data.add(element);
        return data.get(data.size() - 1);
    }

    /**
     * Searches an element by name
     *
     * @param elementName name of an element for searching
     * @return an element if it have found or null otherwise
     */
    @Nullable
    public T findByName(String elementName) {
        for (T object : data) {
            if (object.getName().equals(elementName)) {
                return object;
            }
        }

        return null;
    }
}
