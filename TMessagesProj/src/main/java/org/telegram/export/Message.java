package org.telegram.export;


import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "user_messages")
public class Message {
    @PrimaryKey(autoGenerate = false)
    public int msg_id;
    public int from_id;
    public int to_id;
    @SerializedName("utc_time")
    public long date;
    public int reply_to_msg_id;
    @Nullable
    public String from_name;
    @Nullable
    public String to_name;
    @Nullable
    public String message;
    @Nullable
    public String action;
    @Nullable
    public String file;

    public Message(){

    }
    public Message(int id,int from_id,int to_id,long date,int reply_to_msg_id,String from_name,String to_name, String message,String action,String file){
        this.date=date;
        this.from_id=from_id;
        this.msg_id=id;
        this.from_name=from_name;
        this.to_name=to_name;
        this.file=file;
        this.message=message;
        this.reply_to_msg_id=reply_to_msg_id;
        this.to_id=to_id;
    }
}
