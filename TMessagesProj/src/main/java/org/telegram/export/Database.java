package org.telegram.export;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;
@androidx.room.Database(entities = Message.class,version = 2)
public abstract class Database extends RoomDatabase {
    private static Database database;

    public static synchronized Database getDatabase(Context context) {
        if(database==null)
            database = Room.databaseBuilder(context.getApplicationContext() , Database.class , "Db.db")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        return database;
    }

    public abstract MessageDao getMessageDao();

}
