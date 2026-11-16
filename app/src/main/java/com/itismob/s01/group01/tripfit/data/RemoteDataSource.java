package com.itismob.s01.group01.tripfit.data;

import com.itismob.s01.group01.tripfit.BuildConfig;
import com.itismob.s01.group01.tripfit.data.remote.SupabaseApi;
import com.itismob.s01.group01.tripfit.data.remote.dto.CityDto;
import com.itismob.s01.group01.tripfit.data.remote.dto.PlaceDto;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.HttpUrl;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Talks to Supabase's auto-generated REST for the shared, pipeline-owned `cities`/`places`
 * tables. The anon key is safe here (RLS grants it read-only); write access needs the
 * service_role key, which never ships on-device (see the ingestion pipeline instead).
 *
 * <p>Methods are synchronous ({@code Call.execute()}) because every caller already runs on
 * {@link AppExecutors#diskIO()}, matching the rest of the repository's background-work pattern.
 */
public class RemoteDataSource {

    private static final String PLACE_FIELDS =
            "id,city_id,name,category,lat,lng,address,price_tier,rating,is_hidden_gem,description,image_url";

    private final SupabaseApi api;

    public RemoteDataSource() {
        this(defaultHttpClient());
    }

    RemoteDataSource(OkHttpClient httpClient) {
        this(httpClient, BuildConfig.SUPABASE_URL);
    }

    RemoteDataSource(OkHttpClient httpClient, String supabaseUrl) {
        // Local trip planning remains available before remote settings are supplied.
        if (supabaseUrl == null || HttpUrl.parse(supabaseUrl.trim()) == null) {
            this.api = null;
            return;
        }
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(supabaseUrl.trim().replaceAll("/+$", "") + "/rest/v1/")
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.api = retrofit.create(SupabaseApi.class);
    }

    public List<CityDto> fetchCitiesSync() throws IOException {
        requireConfiguredApi();
        return unwrap(api.getCities("id,name,country,lat,lng").execute());
    }

    public List<PlaceDto> searchPlacesSync(String cityId, String query, int limit) throws IOException {
        requireConfiguredApi();
        Response<List<PlaceDto>> response = api.searchPlaces(
                "eq." + cityId, "ilike.*" + query + "*", PLACE_FIELDS, limit
        ).execute();
        return unwrap(response);
    }

    private void requireConfiguredApi() throws IOException {
        if (api == null) {
            throw new IOException("Online place search is unavailable: configure SUPABASE_URL in local.properties.");
        }
    }

    private <T> T unwrap(Response<T> response) throws IOException {
        if (!response.isSuccessful()) {
            throw new IOException("Supabase request failed: HTTP " + response.code());
        }
        return response.body();
    }

    private static OkHttpClient defaultHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BASIC : HttpLoggingInterceptor.Level.NONE);
        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                        .build()))
                .addInterceptor(logging)
                .build();
    }
}
