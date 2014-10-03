package com.activeandroid;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.activeandroid.content.ContentProvider;
import com.activeandroid.serializer.TypeSerializer;
import com.activeandroid.util.Log;
import com.activeandroid.util.ReflectionUtils;

import java.lang.reflect.Field;

public abstract class FullTextModel<M extends Model> {
    /** Prime number used for hashcode() implementation. */
    private static final int HASH_PRIME = 739;

    private final FullTextTableInfo mTableInfo;
    private M model;

    protected FullTextModel(M model) {
        mTableInfo = Cache.getFullTextInfo(getClass());
        this.model = model;
    }

    public final M getModel() {
        return model;
    }

    public final Long getId() {
        return model != null ? model.getId() : null;
    }

    /**
     * Template method called at beginning of save mainly used to update text search
     * members based on reference model.
     */
    protected void beforeSave() {

    }

    public Long save() {
        beforeSave();

        final SQLiteDatabase db = Cache.openDatabase();
        final ContentValues values = new ContentValues();
        final Long docId = getId();

        for (Field field : mTableInfo.getFields()) {
            final String fieldName = mTableInfo.getColumnName(field);
            Class<?> fieldType = field.getType();

            field.setAccessible(true);

            try {
                Object value = field.get(this);

                if (value != null) {
                    final TypeSerializer typeSerializer = Cache.getParserForType(fieldType);
                    if (typeSerializer != null) {
                        // serialize data
                        value = typeSerializer.serialize(value);
                        // set new object type
                        if (value != null) {
                            fieldType = value.getClass();
                            // check that the serializer returned what it promised
                            if (!fieldType.equals(typeSerializer.getSerializedType())) {
                                Log.w(String.format("TypeSerializer returned wrong type: expected a %s but got a %s",
                                        typeSerializer.getSerializedType(), fieldType));
                            }
                        }
                    }
                }

                // TODO: Find a smarter way to do this? This if block is necessary because we
                // can't know the type until runtime.
                if (value == null) {
                    values.putNull(fieldName);
                }
                else if (fieldType.equals(String.class)) {
                    values.put(fieldName, value.toString());
                }
                else if (ReflectionUtils.isSubclassOf(fieldType, Enum.class)) {
                    values.put(fieldName, ((Enum<?>) value).name());
                }
                else {
                    values.put(fieldName, value.toString());
                }
            }
            catch (IllegalArgumentException e) {
                Log.e(e.getClass().getName(), e);
            }
            catch (IllegalAccessException e) {
                Log.e(e.getClass().getName(), e);
            }
        }

        values.put("docid", model.getId());
        db.replace(mTableInfo.getTableName(), null, values);

        Cache.getContext().getContentResolver()
                .notifyChange(ContentProvider.createFullTextUri(mTableInfo.getType(), docId), null);

        return docId;
    }

    @Override
    public String toString() {
        return mTableInfo.getTableName() + "@" + getId();
    }

    @Override
    public boolean equals(Object obj) {
        final Long docId = getId();
        if (obj instanceof FullTextModel && docId != null) {
            final FullTextModel other = (FullTextModel) obj;

            return docId.equals(other.getId())
                    && (this.mTableInfo.getTableName().equals(other.mTableInfo.getTableName()));
        } else {
            return this == obj;
        }
    }

    @Override
    public int hashCode() {
        final Long docId = getId();
        int hash = HASH_PRIME;
        hash += HASH_PRIME * (docId == null ? super.hashCode() : docId.hashCode()); //if id is null, use Object.hashCode()
        hash += HASH_PRIME * mTableInfo.getTableName().hashCode();
        return hash; //To change body of generated methods, choose Tools | Templates.
    }

    // TODO: static search methods
}

