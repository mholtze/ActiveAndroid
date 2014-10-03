package com.activeandroid;

import com.activeandroid.query.Select;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Basic lazy list that can be used for one-to-many collections. This only supports
 * "inverse" (in hibernate/jpa terminology) persistence management; this means the
 * client is always responsible for managing child objects.
 */
public class HasMany<E extends Model> extends AbstractList<E> {
    private List<E> internalList;
    private Class<E> type;
    private Model owner;
    private String foreignKey;

    public HasMany(Class<E> type, Model owner, String foreignKey) {
        this.type = type;
        this.owner = owner;
        this.foreignKey = foreignKey;
    }

    private boolean isLoaded() {
        return internalList != null;
    }

    private List<E> getInternalList() {
        if (internalList == null) {
            Long id = owner.getId();
            if (id != null) {
                internalList = new Select()
                        .from(type)
                        .where(Cache.getTableName(type) + "." + foreignKey + "=?", id)
                        .execute();
            }
            else {
                internalList = new ArrayList<E>();
            }
        }
        return internalList;
    }

    @Override
    public int size() {
        return getInternalList().size();
    }

    @Override
    public E get(int i) {
        return getInternalList().get(i);
    }

    @Override
    public void clear() {
        internalList = null;
    }

    @Override
    public void add(int location, E object) {
        getInternalList().add(location, object);
    }

    @Override
    public E remove(int location) {
        if (internalList != null)
            return internalList.remove(location);
        return null;
    }

    @Override
    public boolean remove(Object object) {
        if (internalList != null)
            return super.remove(object);
        return true;
    }

    @Override
    protected void removeRange(int start, int end) {
        if (internalList != null)
            super.removeRange(start, end);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        if (internalList != null)
            return super.removeAll(collection);
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        if (internalList != null)
            return super.retainAll(collection);
        return true;
    }
}
