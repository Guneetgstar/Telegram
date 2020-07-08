package org.telegram.export;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insert(Message message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insert(List<Message> messages);

    @Query("delete from user_messages")
    public void deleteAll();

    @Query("select * from user_messages")
    public List<Message> getAll();
}
