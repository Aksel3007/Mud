package com.Aksel_Stark.mud;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TrailDAO {

    @Query("SELECT * FROM trail")
    List<Trail> getAll();

    @Query("SELECT * FROM trail WHERE id =:id")
    Trail loadSingle(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Trail trail);

    @Delete
    void delete(Trail trail);

    @Update
    void update(Trail trail);

}