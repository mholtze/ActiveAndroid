package com.activeandroid;

import android.text.TextUtils;
import android.util.Log;

import com.activeandroid.annotation.FullTextColumn;
import com.activeandroid.annotation.FullTextTable;
import com.activeandroid.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FullTextTableInfo {
    public static final String ID_NAME = "docid";

    //////////////////////////////////////////////////////////////////////////////////////
    // PRIVATE MEMBERS
    //////////////////////////////////////////////////////////////////////////////////////

    private Class<? extends FullTextModel> mType;
    private String mTableName;
    private String mIdName = ID_NAME;

    private Map<Field, String> mColumnNames = new LinkedHashMap<Field, String>();

    //////////////////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////////////////////////////////////////

    public FullTextTableInfo(Class<? extends FullTextModel> type) {
        mType = type;

        final FullTextTable tableAnnotation = type.getAnnotation(FullTextTable.class);

        if (tableAnnotation != null) {
            mTableName = tableAnnotation.name();
        }
        else {
            mTableName = type.getSimpleName();
        }

        // Manually add the id column since it is not declared like the other columns.
        Field idField = getIdField(type);
        mColumnNames.put(idField, mIdName);

        List<Field> fields = new LinkedList<Field>(ReflectionUtils.getDeclaredColumnFields(type));
        Collections.reverse(fields);

        for (Field field : fields) {
            if (field.isAnnotationPresent(FullTextColumn.class)) {
                final FullTextColumn columnAnnotation = field.getAnnotation(FullTextColumn.class);
                String columnName = columnAnnotation.name();
                if (TextUtils.isEmpty(columnName)) {
                    columnName = field.getName();
                }

                mColumnNames.put(field, columnName);
            }
        }

    }

    //////////////////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    //////////////////////////////////////////////////////////////////////////////////////

    public Class<? extends FullTextModel> getType() {
        return mType;
    }

    public String getTableName() {
        return mTableName;
    }

    public String getIdName() {
        return mIdName;
    }

    public Collection<Field> getFields() {
        return mColumnNames.keySet();
    }

    public String getColumnName(Field field) {
        return mColumnNames.get(field);
    }

    private Field getIdField(Class<?> type) {
        if (type.equals(FullTextModel.class)) {
            try {
                return type.getDeclaredField("docId");
            }
            catch (NoSuchFieldException e) {
                Log.e("Impossible!", e.toString());
            }
        }
        else if (type.getSuperclass() != null) {
            return getIdField(type.getSuperclass());
        }

        return null;
    }
}
