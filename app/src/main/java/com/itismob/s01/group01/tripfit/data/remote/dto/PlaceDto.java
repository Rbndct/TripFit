package com.itismob.s01.group01.tripfit.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Maps a row from Supabase's `places` REST response (API-Integration-Plan.md §3 canonical
 * schema). Gson populates these fields by reflection, so they aren't final like the rest of
 * the codebase's domain types.
 */
public class PlaceDto {

    private String id;
    @SerializedName("city_id")
    private String cityId;
    private String name;
    private String category;
    private double lat;
    private double lng;
    private String address;
    @SerializedName("price_tier")
    private Integer priceTier;
    private Double rating;
    @SerializedName("is_hidden_gem")
    private boolean isHiddenGem;
    private String description;
    @SerializedName("image_url")
    private String imageUrl;

    /** Gson bypasses this via reflection when deserializing; it exists for app/test code that
     * needs to build a PlaceDto directly (e.g. caching a selected search result). */
    public PlaceDto(String id, String cityId, String name, String category, double lat,
                     double lng, String address, Integer priceTier, Double rating,
                     boolean isHiddenGem, String description, String imageUrl) {
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
    }

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
}
