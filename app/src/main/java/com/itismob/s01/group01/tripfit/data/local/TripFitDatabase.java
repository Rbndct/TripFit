package com.itismob.s01.group01.tripfit.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.itismob.s01.group01.tripfit.data.local.dao.CityDao;
import com.itismob.s01.group01.tripfit.data.local.dao.ExpenseDao;
import com.itismob.s01.group01.tripfit.data.local.dao.ItineraryItemDao;
import com.itismob.s01.group01.tripfit.data.local.dao.PlaceDao;
import com.itismob.s01.group01.tripfit.data.local.dao.TripDao;
import com.itismob.s01.group01.tripfit.data.local.entity.CityEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.ExpenseEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.ItineraryItemEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.PlaceEntity;
import com.itismob.s01.group01.tripfit.data.local.entity.TripEntity;

@Database(
        entities = {
                CityEntity.class,
                PlaceEntity.class,
                TripEntity.class,
                ItineraryItemEntity.class,
                ExpenseEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class TripFitDatabase extends RoomDatabase {

    private static final String DB_NAME = "tripfit.db";
    private static volatile TripFitDatabase instance;

    public abstract CityDao cityDao();

    public abstract PlaceDao placeDao();

    public abstract TripDao tripDao();

    public abstract ItineraryItemDao itineraryItemDao();

    public abstract ExpenseDao expenseDao();

    public static TripFitDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (TripFitDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            TripFitDatabase.class,
                            DB_NAME
                    ).build();
                }
            }
        }
        return instance;
    }
}
