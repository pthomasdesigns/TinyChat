package com.opengarden.testproject;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "txmessage")
public class TinyChatTxMessage {
    public String msg;
    @NonNull @PrimaryKey
    public String client_time;
}
