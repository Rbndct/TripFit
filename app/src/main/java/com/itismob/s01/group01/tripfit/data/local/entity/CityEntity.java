package com.itismob.s01.group01.tripfit.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Mirrors the pipeline-owned `cities` table in Supabase (see API-Integration-Plan.md §3). */
@Entity(tableName = "cities")
public class CityEntity {

    @PrimaryKey
    @NonNull
    private final String id;
    private final String name;
    private final String country;
    private final double lat;
    private final double lng;
    private final String bboxJson;
    private final String sourcesJson;
    private final long updatedAt;

    public CityEntity(@NonNull String id, String name, String country, double lat, double lng,
                       String bboxJson, String sourcesJson, long updatedAt) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.lat = lat;
        this.lng = lng;
        this.bboxJson = bboxJson;
        this.sourcesJson = sourcesJson;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public String getBboxJson() {
        return bboxJson;
    }

    public String getSourcesJson() {
        return sourcesJson;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
