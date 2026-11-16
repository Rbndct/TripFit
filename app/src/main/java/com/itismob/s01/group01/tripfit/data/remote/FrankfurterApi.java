package com.itismob.s01.group01.tripfit.data.remote;

import com.itismob.s01.group01.tripfit.data.remote.dto.RatesDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** Frankfurter (ECB reference rates): free and keyless, per docs/api-terms.md. */
public interface FrankfurterApi {

    /** Supported currencies as {@code code -> name}, e.g. {@code "JPY" -> "Japanese Yen"}. */
    @GET("currencies")
    Call<Map<String, String>> getCurrencies();

    /** Latest rates quoted against {@code base}; an unknown base answers HTTP 404. */
    @GET("latest")
    Call<RatesDto> getLatest(@Query("base") String base);
}
