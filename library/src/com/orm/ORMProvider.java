package com.orm;

import static com.orm.SugarApp.getSugarContext;

import java.util.Iterator;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.ContentUris;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.database.sqlite.SQLiteDatabase;

import com.orm.Database;
import com.orm.SugarApp;
import com.orm.SugarDb;

public class ORMProvider extends ContentProvider
{
	/*
	 * All of these methods except onCreate() can be called by multiple threads at once, so they must be thread-safe
	 */
	
	private SQLiteDatabase db;
	static final String PROVIDER_NAME = "app.unifi.provider";
	static final String URL = "content://" + PROVIDER_NAME + "/router";
	static public final Uri CONTENT_URI = Uri.parse(URL);
	
	static final int TABLE = 1;
	static final int ROW = 2;
	static final int RAW_QUERY = 3;
	static final int EXEC_SQL = 4;

	static final UriMatcher uriMatcher;
	static
	{
	      uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	      uriMatcher.addURI(PROVIDER_NAME, "RAW_QUERY", RAW_QUERY);
	      uriMatcher.addURI(PROVIDER_NAME, "EXEC_SQL", EXEC_SQL);
	      uriMatcher.addURI(PROVIDER_NAME, "*", TABLE);//only matches one segment
	      uriMatcher.addURI(PROVIDER_NAME, "*/#", ROW);
	}
	
	@Override
	public boolean onCreate()
	{
		//The Android system calls onCreate() when it starts up the provider. 
		//You should perform only fast-running initialization tasks in this method, and defer database creation and data loading until the provider actually receives a request for the data
		db = getSugarContext().getDatabase().getDB();
		return (db == null)? false:true;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		//Use the arguments to select the table and the rows to delete. Return the number of rows deleted.
		//find the row(s) to delete first
		//then delete them
		//content://contacts/people/22 and the implementation is responsible for parsing the record number (22) when creating a SQL statement
		//Uri contains the table and potentially the row id to delete
		//selection contains any filters, like: WHERE col = value
		//selectionArgs replace ? placeholders in the selection clause
		//for now, just use the Uri
		//List<Router> search_results = Router.find(Router.class, "ssid = ?", "\"InfoScout\"");
		// UnsupportedOperationException ()
		int count = 0;
		String table;
		switch (uriMatcher.match(uri))
		{
	      case TABLE:
	    	 table = uri.getPathSegments().get(0);
	    	 db.delete(table, selection, selectionArgs);
	         break;
	      case ROW:
	    	  /*table = uri.getPathSegments().get(0);
	    	  String row = uri.getPathSegments().get(1);
		      db.delete(table, selection, selectionArgs);*/
	    	  throw new UnsupportedOperationException( "Not Implemented");
		}
		return count;
	}

	@Override
	public String getType(Uri uri)
	{
		String table;
		//Return the MIME type corresponding to a content URI
		switch (uriMatcher.match(uri))
		{
	      /**
	       * Get all table records 
	       */
	      case TABLE:
	    	 table = uri.getPathSegments().get(0);
	         return "vnd.android.cursor.dir/vnd.net.akbars.unifi." + table;
	      /** 
	       * Get a particular router
	       */
	      case ROW:
	    	 table = uri.getPathSegments().get(0);
	         return "vnd.android.cursor.item/vnd.net.akbars.unifi." + table;
	      default:
	         throw new IllegalArgumentException("Unsupported URI: " + uri);
	    }
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		//Return a content URI for the newly-inserted row
		//getAsString returns the value or null if the value is missing or cannot be converted
		String table = uri.getPathSegments().get(0);
		db.insert(table, null, values);
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		/* Uri maps to the table in the provider named table_name
		 * projection is an array of columns that should be included for each row retrieved.
		 * selection specifies the criteria for selecting rows
		 * Selection arguments replace ? placeholders in the selection clause
		 * sortOrder specifies the order in which rows appear in the returned Cursor
		 */
		Cursor cursor = null;
		String table = null;
		switch (uriMatcher.match(uri))
		{
	      case TABLE:
	    	  table = uri.getPathSegments().get(0);
	    	  cursor = db.query(table,projection,selection,selectionArgs,null,null,sortOrder);
	    	  return cursor;
		case ROW:
			  table = uri.getPathSegments().get(0);
	    	  cursor = db.query(table,projection,selection,selectionArgs,null,null,sortOrder);
	    	  return cursor;
		case RAW_QUERY:
			cursor = db.rawQuery(selection, selectionArgs);//query, arguments
			return cursor;
		case EXEC_SQL:
			db.execSQL(selection, selectionArgs);//query, arguments
			return null;
		default:
	         throw new IllegalArgumentException("Unknown URI " + uri);
	    }
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		db.update(uri.getPathSegments().get(0), values, selection, selectionArgs);
		return 0;
	}

}