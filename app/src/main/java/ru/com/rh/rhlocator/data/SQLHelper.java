package ru.com.rh.rhlocator.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "city.db";
    private static final int DATABASE_VERSION = 1;

    public SQLHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_TABLE = "CREATE TABLE " + Contract.TABLE_NAME + " ("
                + Contract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Contract.COLUMN_CITY_NAME + " TEXT NOT NULL, "
                + Contract.TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP);";

        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF IT EXISTS " + Contract.TABLE_NAME);
        onCreate(db);
    }
}
