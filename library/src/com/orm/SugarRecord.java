package com.orm;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.orm.dsl.Ignore;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.*;

import static com.orm.SugarApp.getSugarContext;

public class SugarRecord<T>
{

    @Ignore
    String tableName = getSqlName();
	static ContentResolver db = getSugarContext().getContentResolver();
	static String PROVIDER_NAME = "com.orm.provider";

    protected Long id = null;

    public void delete()
    {
    	//create the URI based on the table name
    	String url = "content://" + PROVIDER_NAME + "/" + this.tableName;
    	Uri uri = Uri.parse(url);
    	db.delete(uri, "Id=?", new String[]{getId().toString()});
    }

    public static <T extends SugarRecord<?>> void deleteAll(Class<T> type)
    {
    	//create the URI based on the table name
    	String url = "content://" + PROVIDER_NAME + "/" + getTableName(type);
    	Uri uri = Uri.parse(url);
    	db.delete(uri, null, null);
    }

    public static <T extends SugarRecord<?>> void deleteAll(Class<T> type, String whereClause, String... whereArgs )
    {
    	//create the URI based on the table name
    	String url = "content://" + PROVIDER_NAME + "/" + getTableName(type);
    	Uri uri = Uri.parse(url);
        db.delete(uri, whereClause, whereArgs);
    }

    public void save()
    {
        save(db);
    }

    /*@SuppressWarnings("deprecation")
    public static <T extends SugarRecord<?>> void saveInTx(T... objects )
    {
        saveInTx(Arrays.asList(objects));
    }*/

    /*@SuppressWarnings("deprecation")
    public static <T extends SugarRecord<?>> void saveInTx(Collection<T> objects )
    {
        SQLiteDatabase sqLiteDatabase = getSugarContext().getDatabase().getDB();

        try{
            sqLiteDatabase.beginTransaction();
            sqLiteDatabase.setLockingEnabled(false);
            for(T object: objects){
                object.save(sqLiteDatabase);
            }
            sqLiteDatabase.setTransactionSuccessful();
        }catch (Exception e){
            Log.i("Sugar", "Error in saving in transaction " + e.getMessage());
        }finally {
            sqLiteDatabase.endTransaction();
            sqLiteDatabase.setLockingEnabled(true);
        }

    }*/

    void save(ContentResolver orm)
    {
        List<Field> columns = getTableFields();
        ContentValues values = new ContentValues(columns.size());
        for (Field column : columns)
        {
            column.setAccessible(true);
            Class<?> columnType = column.getType();
            try {
                String columnName = StringUtil.toSQLName(column.getName());
                Object columnValue = column.get(this);
                if (SugarRecord.class.isAssignableFrom(columnType))
                {
                    values.put(columnName,
                            (columnValue != null)
                                    ? String.valueOf(((SugarRecord) columnValue).id)
                                    : "0");
                } 
                else
                {
                    if (!"id".equalsIgnoreCase(column.getName()))
                    {
                        if (columnType.equals(Short.class) || columnType.equals(short.class))
                        {
                            values.put(columnName, (Short) columnValue);
                        }
                        else if (columnType.equals(Integer.class) || columnType.equals(int.class))
                        {
                            values.put(columnName, (Integer) columnValue);
                        }
                        else if (columnType.equals(Long.class) || columnType.equals(long.class))
                        {
                            values.put(columnName, (Long) columnValue);
                        }
                        else if (columnType.equals(Float.class) || columnType.equals(float.class))
                        {
                            values.put(columnName, (Float) columnValue);
                        }
                        else if (columnType.equals(Double.class) || columnType.equals(double.class))
                        {
                            values.put(columnName, (Double) columnValue);
                        }
                        else if (columnType.equals(Boolean.class) || columnType.equals(boolean.class))
                        {
                            values.put(columnName, (Boolean) columnValue);
                        }
                        else if (Date.class.equals(columnType))
                        {
                            values.put(columnName, ((Date) column.get(this)).getTime());
                        }
                        else if (Calendar.class.equals(columnType))
                        {
                            values.put(columnName, ((Calendar) column.get(this)).getTimeInMillis());
                        }
                        else
                        {
                            values.put(columnName, String.valueOf(columnValue));
                        }
                    }
                }
            }
            catch (IllegalAccessException e)
            {
                Log.e("Sugar", e.getMessage());
            }
        }

        //create the URI based on the table name
    	String url = "content://" + PROVIDER_NAME + "/" + getSqlName();
    	Uri uri = Uri.parse(url);
        if (id == null)
        {
            Uri new_row = orm.insert(uri, values);
            id = Long.parseLong(new_row.getPathSegments().get(1));
        }
        else
        	orm.update(uri, values, "ID = ?", new String[]{String.valueOf(id)});

        Log.i("Sugar", getClass().getSimpleName() + " saved : " + id);
    }

    public static <T extends SugarRecord<?>> List<T> listAll(Class<T> type)
    {
        return find(type, null, null, null, null, null);
    }

    public static <T extends SugarRecord<?>> T findById(Class<T> type, Long id)
    {
        List<T> list = find(type, "id=?", new String[]{String.valueOf(id)}, null, null, "1");
        if (list.isEmpty()) return null;
        return list.get(0);
    }

    public static <T extends SugarRecord<?>> Iterator<T> findAll(Class<T> type)
    {
        return findAsIterator(type, null, null, null, null, null);
    }

    public static <T extends SugarRecord<?>> Iterator<T> findAsIterator(Class<T> type,
                                                                        String whereClause, String... whereArgs)
    {
        return findAsIterator(type, whereClause, whereArgs, null, null, null);
    }

    public static <T extends SugarRecord<?>> Iterator<T> findWithQueryAsIterator(Class<T> type, String query, String... arguments)
    {
    	//create the URI based on the table name
    	String url = "content://" + PROVIDER_NAME + "/RAW_QUERY";
    	Uri uri = Uri.parse(url);
        Cursor c = db.query(uri, null, query, arguments, null);
        return new CursorIterator<T>(type, c);
    }

    public static <T extends SugarRecord<?>> Iterator<T> findAsIterator(Class<T> type,
                                                                    String whereClause, String[] whereArgs,
                                                                    String groupBy, String orderBy, String limit)
    {
    	String url = "content://" + PROVIDER_NAME + "/EXTENDED_QUERY";
    	Uri uri = Uri.parse(url);
    	String[] args = {getTableName(type), groupBy, orderBy, limit};
        Cursor c = db.query(uri, args, whereClause, whereArgs, null);
        return new CursorIterator<T>(type, c);
    }

    public static <T extends SugarRecord<?>> List<T> find(Class<T> type,
                                                       String whereClause, String... whereArgs)
    {
    	Log.d("in sugar", "find called");
        return find(type, whereClause, whereArgs, null, null, null);
    }

    public static <T extends SugarRecord<?>> List<T> findWithQuery(Class<T> type, String query, String... arguments)
    {
        T entity;
        List<T> toRet = new ArrayList<T>();
        //create the URI based on the table name
    	String url = "content://" + PROVIDER_NAME + "/RAW_QUERY";
    	Uri uri = Uri.parse(url);
        Cursor c = db.query(uri, null, query, arguments, null);

        try {
            while (c.moveToNext()) {
                //entity = type.getDeclaredConstructor().newInstance();
            	entity = type.getDeclaredConstructor(new Class[] {android.content.Context.class}).newInstance(getSugarContext());
            	entity.inflate(c);
                toRet.add(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return toRet;
    }

    public static void executeQuery(String query, String... arguments)
    {
    	//create the URI based on the table name
    	String url = "content://" + PROVIDER_NAME + "/EXEC_SQL";
    	Uri uri = Uri.parse(url);
        db.query(uri, null, query, arguments, null);
    }

    public static <T extends SugarRecord<?>> List<T> find(Class<T> type,
                                                       String whereClause, String[] whereArgs,
                                                       String groupBy, String orderBy, String limit)
    {
    	String url = "content://" + PROVIDER_NAME + "/EXTENDED_QUERY";
    	Uri uri = Uri.parse(url);
    	//is deprecated: buildQuery (String[] projectionIn, String selection, String[] selectionArgs, String groupBy, String having, String sortOrder, String limit)
        /*Object args[] = whereArgs;
    	String where_query = String.format(whereClause, args);
    	SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
    	builder.setTables(getTableName(type));
    	String query = builder.buildQuery(null, whereClause, whereArgs, groupBy, null, orderBy, limit);
    	String query = builder.buildQuery(null, where_query, groupBy, null, orderBy, limit);
    	Cursor c = db.query(uri, null, query, null, null);//null pointer exception here*/
    	/*Bundle b = new Bundle();
    	b.putString("whereClause", whereClause);
    	b.putStringArray("whereArgs", whereArgs);
        b.putString("groupBy", groupBy);
        b.putString("orderBy", orderBy);
        b.putString("limit", limit);
        Bundle ret = db.call("EXTENDED_QUERY", getTableName(type), b);
        //how do i put a cursor in the return bundle?  don't want to do this logic inside the call method itself*/
    	String[] args = {getTableName(type), groupBy, orderBy, limit};
    	/*Log.d("null testing", uri.toString());
    	Log.d("null testing", args.toString());
    	Log.d("null testing", whereClause.toString());
    	Log.d("null testing", whereArgs.toString());*/
    	
        T entity;
        List<T> toRet = new ArrayList<T>();
        Log.d("in find", "db.query()");
        Cursor c = db.query(uri, args, whereClause, whereArgs, null);
        try {
            while (c.moveToNext())
            {
            	Log.d("in find", "got result row from query");
                //entity = type.getDeclaredConstructor().newInstance();//error here
            	//The parameterTypes parameter is an array of Class objects that identify the constructor's formal parameter types, in declared order.
                entity = type.getDeclaredConstructor(new Class[] {android.content.Context.class}).newInstance(getSugarContext());
                Log.d("constructor","got constructor & new instance");
                entity.inflate(c);
                toRet.add(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
        return toRet;
    }
    
    public static <T extends SugarRecord<?>> long count(Class<?> type,
            String whereClause, String[] whereArgs)
    {
    	return count(type, whereClause, whereArgs, null, null, null);
    }
    
    public static <T extends SugarRecord<?>> long count(Class<?> type,
            String whereClause, String[] whereArgs,
            String groupBy, String orderBy, String limit)
    {
    	/*Database db = getSugarContext().getDatabase();
        SQLiteDatabase sqLiteDatabase = db.getDB();
        long toRet = -1;

        String filter = (!TextUtils.isEmpty(whereClause)) ? " where "  + whereClause : "";
        SQLiteStatement sqLiteStatament = sqLiteDatabase.compileStatement("SELECT count(*) FROM " + getTableName(type) + filter);

        if (whereArgs != null) {
            for (int i = whereArgs.length; i != 0; i--) {
                sqLiteStatament.bindString(i, whereArgs[i - 1]);
            }
        }

        try {
            toRet = sqLiteStatament.simpleQueryForLong();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sqLiteStatament.close();
        }
        return toRet;
        */
        String url = "content://" + PROVIDER_NAME + "/" + getTableName(type);
    	Uri uri = Uri.parse(url);
        Cursor c = db.query(uri, null, whereClause, whereArgs, null);
        return c.getCount();
    }

    @SuppressWarnings("unchecked")
    void inflate(Cursor cursor) {
        Map<Field, Long> entities = new HashMap<Field, Long>();
        List<Field> columns = getTableFields();
        for (Field field : columns) {
            field.setAccessible(true);
            try {
                Class fieldType = field.getType();
                String colName = StringUtil.toSQLName(field.getName());

                int columnIndex = cursor.getColumnIndex(colName);

                if (cursor.isNull(columnIndex)) {
                    continue;
                }

                if(colName.equalsIgnoreCase("id")){
                    long cid = cursor.getLong(columnIndex);
                    field.set(this, Long.valueOf(cid));
                }else if (fieldType.equals(long.class) || fieldType.equals(Long.class)) {
                    field.set(this,
                            cursor.getLong(columnIndex));
                } else if (fieldType.equals(String.class)) {
                    String val = cursor.getString(columnIndex);
                    field.set(this, val != null && val.equals("null") ? null : val);
                } else if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
                    field.set(this,
                            cursor.getDouble(columnIndex));
                } else if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
                    field.set(this,
                            cursor.getString(columnIndex).equals("1"));
                } else if (field.getType().getName().equals("[B")) {
                    field.set(this,
                            cursor.getBlob(columnIndex));
                } else if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
                    field.set(this,
                            cursor.getInt(columnIndex));
                } else if (fieldType.equals(float.class) || fieldType.equals(Float.class)) {
                    field.set(this,
                            cursor.getFloat(columnIndex));
                } else if (fieldType.equals(short.class) || fieldType.equals(Short.class)) {
                    field.set(this,
                            cursor.getShort(columnIndex));
                } else if (fieldType.equals(Timestamp.class)) {
                    long l = cursor.getLong(columnIndex);
                    field.set(this, new Timestamp(l));
                } else if (fieldType.equals(Date.class)) {
                    long l = cursor.getLong(columnIndex);
                    field.set(this, new Date(l));
                } else if (fieldType.equals(Calendar.class)) {
                    long l = cursor.getLong(columnIndex);
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(l);
                    field.set(this, c);
                } else if (Enum.class.isAssignableFrom(fieldType)) {
                    try {
                        Method valueOf = field.getType().getMethod("valueOf", String.class);
                        String strVal = cursor.getString(columnIndex);
                        Object enumVal = valueOf.invoke(field.getType(), strVal);
                        field.set(this, enumVal);
                    } catch (Exception e) {
                        Log.e("Sugar", "Enum cannot be read from Sqlite3 database. Please check the type of field " + field.getName());
                    }
                } else if (SugarRecord.class.isAssignableFrom(fieldType)) {
                    long id = cursor.getLong(columnIndex);
                    if (id > 0)
                        entities.put(field, id);
                    else
                        field.set(this, null);
                } else
                    Log.e("Sugar", "Class cannot be read from Sqlite3 database. Please check the type of field " + field.getName() + "(" + field.getType().getName() + ")");
            } catch (IllegalArgumentException e) {
                Log.e("field set error", e.getMessage());
            } catch (IllegalAccessException e) {
                Log.e("field set error", e.getMessage());
            }

        }

        for (Field f : entities.keySet()) {
            try {
                f.set(this, findById((Class<? extends SugarRecord<?>>) f.getType(), 
                        entities.get(f)));
            } catch (SQLiteException e) {
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            }
        }
    }

    public List<Field> getTableFields()
    {
        List<Field> fieldList = SugarConfig.getFields(getClass());
        if(fieldList != null) return fieldList;

        Log.d("Sugar", "Fetching properties");
        List<Field> typeFields = new ArrayList<Field>();

        getAllFields(typeFields, getClass());

        List<Field> toStore = new ArrayList<Field>();
        for (Field field : typeFields) {
            if (!field.isAnnotationPresent(Ignore.class) && !Modifier.isStatic(field.getModifiers())&& !Modifier.isTransient(field.getModifiers())) {
                toStore.add(field);
            }
        }

        SugarConfig.setFields(getClass(), toStore);
        return toStore;
    }

    private static List<Field> getAllFields(List<Field> fields, Class<?> type)
    {
        Collections.addAll(fields, type.getDeclaredFields());

        if (type.getSuperclass() != null) {
            fields = getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    public String getSqlName()
    {
        return getTableName(getClass());
    }

    public static String getTableName(Class<?> type)
    {
        return StringUtil.toSQLName(type.getSimpleName());
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    static class CursorIterator<E extends SugarRecord<?>> implements Iterator<E>
    {
        Class<E> type;
        Cursor cursor;

        public CursorIterator(Class<E> type, Cursor cursor)
        {
            this.type = type;
            this.cursor = cursor;
        }

        @Override
        public boolean hasNext()
        {
            return cursor != null && !cursor.isClosed() && !cursor.isAfterLast();
        }

        @Override
        public E next()
        {
            E entity = null;
            if (cursor == null || cursor.isAfterLast()) {
                throw new NoSuchElementException();
            }

            if (cursor.isBeforeFirst()) {
                cursor.moveToFirst();
            }

            try {
                //entity = type.getDeclaredConstructor().newInstance();
            	entity = type.getDeclaredConstructor(new Class[] {android.content.Context.class}).newInstance(getSugarContext());
                entity.inflate(cursor);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.moveToNext();
                if (cursor.isAfterLast()) {
                    cursor.close();
                }
            }
            return entity;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
