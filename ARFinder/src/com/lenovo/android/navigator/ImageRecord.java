package com.lenovo.android.navigator;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.lenovo.android.navigator.AnchorViewClone.Data;
import com.lenovo.android.navigator.AnchorViewClone.UriTimestamp;

/*
 * 记录信息点详细内容的数据库
 */
public class ImageRecord extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "data.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_RECORD = "record";
    private static final String TABLE_URI = "uri";

    private static final String NAME = "name";
    private static final String DISTANCE = "distance";
    private static final String PHONE = "phone";
    private static final String ADDR = "addr";
    private static final String WINX = "winx";
    private static final String WINY = "winy";

    private static final String LINK = "link";
    private static final String TIMESTAMP = "timestamp";

    public ImageRecord(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE record (" +
                    "timestamp INTEGER NOT NULL," +
                    "distance TEXT ," +
                    "winx TEXT ," +
                    "winy TEXT ," +
                    "name TEXT ," +
                    "addr TEXT ," +
                    "phone TEXT " +
                    ");");
            db.execSQL("CREATE TABLE uri (" +
                    "timestamp INTEGER NOT NULL," +
                    "link TEXT NOT NULL" +
                    ");");
        } catch (SQLException e) {
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { 
        if (oldVersion != DATABASE_VERSION) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_URI);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORD);
            onCreate(db);
        }
    }

    /*
     * 查找最早的记录
     */
    public long queryOldestTimestamp() {
        SQLiteDatabase db = getWritableDatabase();
        String cols[] = { "min(" + TIMESTAMP + ")" };

        int result = -1;
        Cursor cursor = db.query(TABLE_URI, cols, null, null, null, null, null);
        int rowNums = cursor.getCount();
        if (rowNums <= 0) {
        	cursor.close();
        	db.close();
            return result;
        }
        
        try {
            cursor.moveToFirst();
            result = cursor.getInt(0);
        } finally {
            cursor.close();
        }
        db.close();
        return result;
    }

    public boolean deleteLinkAndRecord(long timestamp) {
        SQLiteDatabase db = getWritableDatabase(); 
        String where = TIMESTAMP + "='" + timestamp + "'";
        boolean a = db.delete(TABLE_RECORD, where, null) > 0;
        boolean b= db.delete(TABLE_URI, where, null) > 0;
        db.close();
        return a && b;
    }


	public boolean insertUri(String link, long timestamp) {
        SQLiteDatabase db = getWritableDatabase(); 
        ContentValues values = new ContentValues();

        values.put(LINK, link);
        values.put(TIMESTAMP, timestamp);
        boolean ret = db.insert(TABLE_URI, null, values) > 0;
        db.close();
        return ret;
    }
	
	public boolean insertRecord(Data data, long timestamp) {
        SQLiteDatabase db = getWritableDatabase(); 
        ContentValues values = new ContentValues();

        values.put(TIMESTAMP, timestamp);
        values.put(DISTANCE, data.distance+"");
        values.put(NAME, data.name);
        values.put(ADDR, data.addr);
        values.put(PHONE, data.phone);
        values.put(WINX, data.winX+"");
        values.put(WINY, data.winY+"");

        boolean ret = db.insert(TABLE_RECORD, null, values) > 0;
        db.close();
        return ret;
    }

    public List<UriTimestamp> queryUriAndTimestamp() {
        SQLiteDatabase db = getWritableDatabase();
        String cols[] = { LINK , TIMESTAMP };
        
        String orderBy = TIMESTAMP +  " asc";
        List<UriTimestamp> result = null;
        
        Cursor cursor = db.query(TABLE_URI, cols, null, null, null, null, orderBy);
        int rowNums = cursor.getCount();
        if (rowNums <= 0) {
        	cursor.close();
        	db.close();
            return result;
        }
        
        try {
            result = new ArrayList<UriTimestamp>();

            while (cursor.moveToNext()) {
            	String uri = cursor.getString(0);
            	long timestamp = cursor.getLong(1);            	
            	UriTimestamp tmp = new UriTimestamp(uri, timestamp);
            	result.add(tmp);
            }
        } finally {
        	cursor.close();
        }
        db.close();
        return result;
    }

    public String queryUri(long timestamp) {
        SQLiteDatabase db = getWritableDatabase();
        String cols[] = { LINK };
        String where = TIMESTAMP + "=" + timestamp;

        String result = null;
        Cursor cursor = db.query(TABLE_URI, cols, where, null, null, null, null);
        int rowNums = cursor.getCount();
        if (rowNums <= 0) {
        	cursor.close();
        	db.close();
        	return result;
        }            
        
        try {
        	cursor.moveToFirst();
        	result = cursor.getString(0);
        } finally {
        	cursor.close();
        }
        db.close();
        return result;
    }

    public List<Data> queryRecord(long timestamp) {
        SQLiteDatabase db = getWritableDatabase();
        String cols[] = { DISTANCE, WINX, WINY, NAME, ADDR, PHONE };
        String where = TIMESTAMP + " = " + timestamp;

        List<Data> result = null;
        Cursor cursor = db.query(TABLE_RECORD, cols, where, null, null, null, null);
        try {
            int rowNum = cursor.getCount();
            if (rowNum <= 0) {
            	cursor.close();
            	db.close();
                return null;
            }
            result = new ArrayList<Data>();

            while (cursor.moveToNext()) {
                String distance = cursor.getString(0);
                float winX = cursor.getFloat(1);
                float winY = cursor.getFloat(2);
                String name = cursor.getString(3);
                String addr = cursor.getString(4);
                String phone = cursor.getString(5);

                Data data = new Data(distance, winX, winY, name, addr, phone);
                result.add(data);
            }
        } finally {
            cursor.close();
            db.close();
        }
        return result;
    }
}

