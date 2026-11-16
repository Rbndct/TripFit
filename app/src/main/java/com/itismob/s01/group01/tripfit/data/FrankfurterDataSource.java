package com.itismob.s01.group01.tripfit.data;

import com.itismob.s01.group01.tripfit.data.remote.FrankfurterApi;
import com.itismob.s01.group01.tripfit.data.remote.dto.RatesDto;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Reads currencies and exchange rates from Frankfurter. Methods are synchronous, so callers run
 * them off the main thread (see {@link CurrencyRepository}). No key is needed, and no
 * identifying header is sent: the request carries nothing about the user.
 */
public class FrankfurterDataSource {

    private static final String BASE_URL = "https://api.frankfurter.dev/v1/";
    /** All rates are quoted against USD so conversion between any two codes crosses through it. */
    static final String BASE_CURRENCY = "USD";

    private final FrankfurterApi api;

    public FrankfurterDataSource() {
        this(defaultHttpClient());
    }

    FrankfurterDataSource(OkHttpClient httpClient) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.api = retrofit.create(FrankfurterApi.class);
    }

    /** Supported currencies as {@code code -> name}. */
    public Map<String, String> fetchCurrenciesSync() throws IOException {
        return unwrap(api.getCurrencies().execute());
    }

    /** Latest rates per 1 USD. Throws when the answer carries no rates, so callers never cache an empty table. */
    public RatesDto fetchUsdRatesSync() throws IOException {
        RatesDto rates = unwrap(api.getLatest(BASE_CURRENCY).execute());
        if (rates.getRates() == null || rates.getRates().isEmpty()) {
            throw new IOException("Frankfurter response had no rates");
        }
        return rates;
    }

    private <T> T unwrap(Response<T> response) throws IOException {
        if (!response.isSuccessful()) {
            throw new IOException("Frankfurter request failed: HTTP " + response.code());
        }
        T body = response.body();
        if (body == null) {
            throw new IOException("Frankfurter returned an empty body");
        }
        return body;
    }

    private static OkHttpClient defaultHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }
}
