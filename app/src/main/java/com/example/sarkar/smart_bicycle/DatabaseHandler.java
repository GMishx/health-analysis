package com.example.sarkar.smart_bicycle;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sarkar on 04-12-2016.
 */

class DatabaseHandler extends SQLiteOpenHelper {

    private static final String DB_Name = "Health.db";
    private static final String TB_Name = "rides";
    private static final String KEY_ID = "_id";
    private static final String KEY_BPM = "BPM";
    private static final String KEY_speed = "speed";
    private static final String KEY_dateTime = "dateTime";

    private static final String USER_TB_Name = "user";
    private static final String USER_KEY_ID = "_id";
    private static final String USER_KEY_NAME = "namee";
    private static final String USER_KEY_DOB = "dob";
    private static final String USER_KEY_DIST = "distance";

    DatabaseHandler(Context context) {
        super(context, DB_Name, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_RIDE_TABLE = "CREATE TABLE " + TB_Name + "(" + KEY_ID +" INTEGER PRIMARY KEY,"
                + KEY_BPM +" INTEGER," + KEY_speed +" FLOAT,"
                + KEY_dateTime +" TEXT)";
        db.execSQL(CREATE_RIDE_TABLE);
        String CREATE_USER_TABLE = "CREATE TABLE " + USER_TB_Name + "(" + USER_KEY_ID +" INTEGER PRIMARY KEY,"
                + USER_KEY_NAME +" TEXT," + USER_KEY_DOB +" INTEGER,"
                + USER_KEY_DIST +" REAL)";
        db.execSQL(CREATE_USER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TB_Name);
        db.execSQL("DROP TABLE IF EXISTS " + USER_TB_Name);

        // Create tables again
        onCreate(db);
    }

    // code to add the new contact
    void addRide(rideData ride) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_BPM, ride.getBPM());
        values.put(KEY_speed, ride.getSpeed());
        values.put(KEY_dateTime, ride.getDateTime());

        // Inserting Row
        db.insert(TB_Name, null, values);
        //2nd argument is String containing nullColumnHack
        db.close(); // Closing database connection
    }

    rideData getRideData(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TB_Name, new String[] { KEY_ID,
                        KEY_BPM, KEY_speed, KEY_dateTime }, KEY_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        assert cursor != null;
        cursor.moveToFirst();
        rideData ride = new rideData(cursor.getInt(0),
                cursor.getInt(1), cursor.getFloat(2), cursor.getString(3));
        // return contact
        cursor.close();
        return ride;
    }

    // code to get all contacts in a list view
    List<rideData> getAllRides() {
        List<rideData> rideList = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TB_Name;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                rideData ride = new rideData();
                ride.setID(Integer.parseInt(cursor.getString(0)));
                ride.setBPM(Integer.parseInt(cursor.getString(1)));
                ride.setSpeed(Float.valueOf(cursor.getString((2))));
                ride.setDateTime(cursor.getString(3));
                // Adding contact to list
                rideList.add(ride);
            } while (cursor.moveToNext());
        }

        cursor.close();
        // return contact list
        return rideList;
    }

    // Deleting single contact
    public void deleteRide(rideData ride) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TB_Name, KEY_ID + " = ?",
                new String[] { String.valueOf(ride.getID()) });
        db.close();
    }

    void clearRides(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from "+ TB_Name);
        db.close();
    }

    // Getting contacts Count
    public int getRidesCount() {
        String countQuery = "SELECT  * FROM " + TB_Name;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        // return count
        return cursor.getCount();
    }

    int updateUser(userData d){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(USER_KEY_NAME, d.getName());
        values.put(USER_KEY_DOB, d.getDOB());
        values.put(USER_KEY_DIST, d.getDist());

        // updating row
        return db.update(USER_TB_Name, values, USER_KEY_ID + " = ?",
                new String[] { String.valueOf(d.getID()) });
    }

    void addUser(userData d) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(USER_KEY_NAME, d.getName());
        values.put(USER_KEY_DIST, d.getDist());
        values.put(USER_KEY_DOB, d.getDOB());

        // Inserting Row
        db.insert(USER_TB_Name, null, values);
        //2nd argument is String containing nullColumnHack
        db.close(); // Closing database connection
    }

    int userExist(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(USER_TB_Name,new String[] {USER_KEY_ID},null,null,null,null,null);
        int retval = -1;
        assert cursor != null;
        if (cursor.getCount()>0) {
            cursor.moveToFirst();
            retval = Integer.parseInt(cursor.getString(0));
        }
        cursor.close();
        return retval;
    }

    float getDistance(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(USER_TB_Name, new String[] { USER_KEY_DIST}, USER_KEY_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        assert cursor != null;
        cursor.moveToFirst();

        float distance = cursor.getFloat(0);
        cursor.close();
        // return contact
        return distance;
    }

    float updateDistance(int id,float dist) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(USER_KEY_DIST, dist);

        // updating row
        return db.update(USER_TB_Name, values, USER_KEY_ID + " = ?",
                new String[] { String.valueOf(id) });
    }

    userData getUserData(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(USER_TB_Name, new String[] { USER_KEY_ID,
                        USER_KEY_NAME, USER_KEY_DOB, USER_KEY_DIST}, USER_KEY_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        assert cursor != null;
        cursor.moveToFirst();

        userData d = new userData(cursor.getInt(0),
                cursor.getString(1), cursor.getLong(2), cursor.getFloat(3));
        cursor.close();
        // return contact
        return d;
    }
}
