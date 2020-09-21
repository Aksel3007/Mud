package com.Aksel_Stark.mud;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.Aksel_Stark.mud.Trail;
import com.Aksel_Stark.mud.TrailDAO;

@Database(entities = {Trail.class}, version = 2,exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TrailDAO trailDao();
}