package com.opengarden.testproject;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.support.constraint.ConstraintLayout;

@Database(entities = {TinyChatTxMessage.class}, version = 1, exportSchema = false)
public abstract class TinyChatTxMessageDatabase extends RoomDatabase {

    private static volatile TinyChatTxMessageDatabase INSTANCE;

    public abstract TinyChatTxMessageDao tinyChatTxMessageDao();

    public static TinyChatTxMessageDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (TinyChatTxMessageDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            TinyChatTxMessageDatabase.class, "txmessage.db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }



}