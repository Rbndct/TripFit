package com.itismob.s01.group01.tripfit.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Mirrors the pipeline-owned `places` table in Supabase (see API-Integration-Plan.md §3). */
@Entity(
        tableName = "places",
        foreignKeys = @ForeignKey(
                entity = CityEntity.class,
                parentColumns = "id",
                childColumns = "cityId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("cityId")}
)
public class PlaceEntity {

    @PrimaryKey
    @NonNull
    private final String id;
    private final String cityId;
    private final String name;
    private final String category;
    private final double lat;
    private final double lng;
    private final String address;
    private final Integer priceTier;
    private final Double rating;
    private final boolean isHiddenGem;
    private final String description;
    private final String imageUrl;
    private final String wikidataId;
    private final String osmId;
    private final String sourcesJson;
    private final Long cleanedAt;

    public PlaceEntity(@NonNull String id, String cityId, String name, String category,
                        double lat, double lng, String address, Integer priceTier, Double rating,
                        boolean isHiddenGem, String description, String imageUrl,
                        String wikidataId, String osmId, String sourcesJson, Long cleanedAt) {
        this.id = id;
        this.cityId = cityId;
        this.name = name;
        this.category = category;
        this.lat = lat;
        this.lng = lng;
        this.address = address;
        this.priceTier = priceTier;
        this.rating = rating;
        this.isHiddenGem = isHiddenGem;
        this.description = description;
        this.imageUrl = imageUrl;
        this.wikidataId = wikidataId;
        this.osmId = osmId;
        this.sourcesJson = sourcesJson;
        this.cleanedAt = cleanedAt;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public String getCityId() {
        return cityId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public String getAddress() {
        return address;
    }

    public Integer getPriceTier() {
        return priceTier;
    }

    public Double getRating() {
        return rating;
    }

    public boolean isHiddenGem() {
        return isHiddenGem;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getWikidataId() {
        return wikidataId;
    }

    public String getOsmId() {
        return osmId;
    }

    public String getSourcesJson() {
        return sourcesJson;
    }

    public Long getCleanedAt() {
        return cleanedAt;
    }
}
