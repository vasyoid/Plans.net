package ru.spbau.mit.plansnet.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic abstract class which contains HashMap of type T and works with this data
 * @param <T> type of data which will be contains
 */
public abstract class AbstractDataContainer<T extends AbstractNamedData>
        extends AbstractNamedData implements Serializable {
    @NonNull
    private final Map<String, T> data = new HashMap<>();

    public AbstractDataContainer(@NonNull String name) {
        super(name);
    }

    @NotNull
    public List<T> getListOfData() {
        ArrayList<T> list = new ArrayList<>();
        list.addAll(data.values());
        return list;
    }

    @NonNull
    public Collection<T> getValues() {
        return data.values();
    }

    @NonNull
    public Map<String, T> getAllData() {
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
