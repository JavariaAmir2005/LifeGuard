package com.example.lifeguard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "lifeguard.db";
    private static final int DATABASE_VERSION = 1;

    // Tables
    public static final String TABLE_CONTACTS = "contacts";
    public static final String TABLE_HISTORY = "history";

    // Contacts columns
    public static final String COL_CONTACT_ID = "id";
    public static final String COL_CONTACT_NAME = "name";
    public static final String COL_CONTACT_PHONE = "phone";

    // History columns
    public static final String COL_HISTORY_ID = "id";
    public static final String COL_HISTORY_TYPE = "type";
    public static final String COL_HISTORY_TIME = "time";
    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createContactsTable = "CREATE TABLE " + TABLE_CONTACTS + "(" +
                COL_CONTACT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_CONTACT_NAME + " TEXT," +
                COL_CONTACT_PHONE + " TEXT)";

        String createHistoryTable = "CREATE TABLE " + TABLE_HISTORY + "(" +
                COL_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_HISTORY_TYPE + " TEXT," +
                COL_HISTORY_TIME + " TEXT)";

        db.execSQL(createContactsTable);
        db.execSQL(createHistoryTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    public long addContact(String name, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_CONTACT_NAME, name);
        cv.put(COL_CONTACT_PHONE, phone);
        return db.insert(TABLE_CONTACTS, null, cv);
    }

    public Cursor getAllContacts() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_CONTACTS, null, null, null, null, null, COL_CONTACT_NAME + " ASC");
    }

    public int updateContact(int id, String name, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_CONTACT_NAME, name);
        cv.put(COL_CONTACT_PHONE, phone);
        return db.update(TABLE_CONTACTS, cv, COL_CONTACT_ID + "=?", new String[]{String.valueOf(id)});
    }

    public int deleteContact(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_CONTACTS, COL_CONTACT_ID + "=?", new String[]{String.valueOf(id)});
    }

    // --- CRUD for History ---
    public long addHistory(String type, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_HISTORY_TYPE, type);
        cv.put(COL_HISTORY_TIME, time);
        return db.insert(TABLE_HISTORY, null, cv);
    }

    public Cursor getAllHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_HISTORY, null, null, null, null, null, COL_HISTORY_ID + " DESC");
    }

}
