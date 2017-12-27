package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Generic abstract class which contains HashMap of type T and works with this data
 * @param <T> type of data which will be contains
 */
public abstract class AbstractDataContainer<T extends AbstractNamedData>
        extends AbstractNamedData implements Serializable {
    @NonNull
    private final HashMap<String, T> data;

    public AbstractDataContainer(@NonNull String name) {
        super(name);
        data = new HashMap<>();
    }

    @NotNull
    public ArrayList<String> getListOfNames() {
        ArrayList<String> list = new ArrayList<>();
        for (Object name : data.keySet().toArray())  {
            list.add((String) name);
        }
        return list;
    }

    @NonNull
    public HashMap<String, T> getAllData() {
        return data;
    }

    /**
     * Adds an element to the data
     *
     * @param element an element to adding
     * @return previous value or null
     */
    @NonNull
    public T addData(@NonNull T element) {
        return data.put(element.getName(), element);
    }

    /**
     * Find an element with equals name and replace this.
     * Add the element to container if it doesn't exist
     *
     * @return an added element
     */
    @NonNull
    public T setElementToContainer(T toSet) {
        data.put(toSet.getName(), toSet);
        return toSet;
    }

    /**
     * Searches an element by name
     *
     * @param elementName name of an element for searching
     * @return an element if it have found or null otherwise
     */
    @Nullable
    public T findByName(String elementName) {
        return data.get(elementName);
    }
}
