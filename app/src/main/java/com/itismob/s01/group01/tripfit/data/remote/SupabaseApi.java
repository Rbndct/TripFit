package com.itismob.s01.group01.tripfit.data.remote;

import com.itismob.s01.group01.tripfit.data.remote.dto.CityDto;
import com.itismob.s01.group01.tripfit.data.remote.dto.PlaceDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** PostgREST endpoints for the pipeline-owned, public-read `cities`/`places` tables. */
public interface SupabaseApi {

    @GET("cities")
    Call<List<CityDto>> getCities(@Query("select") String select);

    /** {@code cityIdFilter}/{@code nameFilter} are full PostgREST filter expressions, e.g. "eq.&lt;uuid&gt;" / "ilike.*temple*". */
    @GET("places")
    Call<List<PlaceDto>> searchPlaces(
            @Query("city_id") String cityIdFilter,
            @Query("name") String nameFilter,
            @Query("select") String select,
            @Query("limit") int limit
    );
}
