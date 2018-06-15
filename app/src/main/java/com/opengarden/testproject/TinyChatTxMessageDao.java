package com.opengarden.testproject;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface TinyChatTxMessageDao {
    @Query("SELECT * FROM txmessage")
    List<TinyChatTxMessage> getAll();

    @Insert
    void insert(TinyChatTxMessage msg);

    @Query("DELETE FROM txmessage")
    void deleteAll();
}
