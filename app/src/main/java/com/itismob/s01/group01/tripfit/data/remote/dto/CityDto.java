package com.itismob.s01.group01.tripfit.data.remote.dto;

/**
 * Maps a row from Supabase's `cities` REST response. Gson populates these fields by
 * reflection, so unlike the rest of the codebase's domain types they aren't final.
 */
public class CityDto {

    private String id;
    private String name;
    private String country;
    private double lat;
    private double lng;

    public CityDto() { }

    public CityDto(String id, String name, String country, double lat, double lng) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.lat = lat;
        this.lng = lng;
    }

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
}
