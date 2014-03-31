package com.orm;

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
	
	@Override
	public boolean onCreate()
	{
		//The Android system calls onCreate() when it starts up the provider. 
		//You should perform only fast-running initialization tasks in this method, and defer database creation and data loading until the provider actually receives a request for the data
		return false;
	}
	
	static final String PROVIDER_NAME = "app.unifi.provider";
	static final String URL = "content://" + PROVIDER_NAME + "/router";
	static public final Uri CONTENT_URI = Uri.parse(URL);
	
	static final int ROUTER = 1;
	static final int ROUTER_ID = 2;
	
	static final UriMatcher uriMatcher;
	static
	{
	      uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	      uriMatcher.addURI(PROVIDER_NAME, "router", ROUTER);
	      uriMatcher.addURI(PROVIDER_NAME, "router/#", ROUTER_ID);
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
		switch (uriMatcher.match(uri))
		{
	      case ROUTER:
	    	 Router.deleteAll(Router.class);
	         break;
	      case ROUTER_ID:
	    	  String param = uri.getPathSegments().get(1);
	    	  Long id = Long.parseLong(param);
	    	  Router router = Router.findById(Router.class, id);
	  		  router.delete();
		}		
		return count;
	}

	@Override
	public String getType(Uri uri)
	{
		//Return the MIME type corresponding to a content URI
		switch (uriMatcher.match(uri))
		{
	      /**
	       * Get all router records 
	       */
	      case ROUTER:
	         return "vnd.android.cursor.dir/vnd.net.akbars.unifi.router";
	      /** 
	       * Get a particular router
	       */
	      case ROUTER_ID:
	         return "vnd.android.cursor.item/vnd.net.akbars.unifi.router";
	      default:
	         throw new IllegalArgumentException("Unsupported URI: " + uri);
	    }
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		//Return a content URI for the newly-inserted row
		//getAsString returns the value or null if the value is missing or cannot be converted
		String ssid = values.getAsString("ssid");
		String bssid = values.getAsString("bssid");
		Router new_router = new Router(getContext(), ssid, bssid);
		new_router.save();
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
		switch (uriMatcher.match(uri))
		{
	      case ROUTER:
	    	  List<Router> routers = Router.find(Router.class, selection, selectionArgs);
	    	  String[] whereArgs;
	    	  Iterator<Router> it = Router.findAsIterator(Router.class, selection, selectionArgs);
	    	  //cursor = Router.findAsCursor(Router.class, selection, selectionArgs);
	    	  return cursor;
		case ROUTER_ID:
	    	  String param = uri.getPathSegments().get(1);
	    	  String[] params = {param};
	    	  //cursor = Router.findAsCursor(Router.class, "WHERE id = ?", params);
	    	  return cursor;
		default:
	         throw new IllegalArgumentException("Unknown URI " + uri);
	    }
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		//UPDATE Router SET(key=value) WHERE column = ?, selectionArgs
		return 0;
	}

}