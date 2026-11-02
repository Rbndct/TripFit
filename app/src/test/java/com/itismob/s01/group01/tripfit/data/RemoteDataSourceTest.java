package com.itismob.s01.group01.tripfit.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.itismob.s01.group01.tripfit.data.remote.dto.CityDto;
import com.itismob.s01.group01.tripfit.data.remote.dto.PlaceDto;

import org.junit.Test;

import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Exercises {@link RemoteDataSource} against a mocked Retrofit response, per the repository
 * testing conventions (no live network calls in tests). */
public class RemoteDataSourceTest {

    private static final String CITIES_JSON =
            "[{\"id\":\"city-1\",\"name\":\"Tokyo\",\"country\":\"JP\",\"lat\":35.68,\"lng\":139.76}]";
    private static final String PLACES_JSON =
            "[{\"id\":\"place-1\",\"city_id\":\"city-1\",\"name\":\"Senso-ji Temple\","
                    + "\"category\":\"place_of_worship\",\"lat\":35.71,\"lng\":139.79,"
                    + "\"is_hidden_gem\":false,\"rating\":4.5}]";

    @Test
    public void fetchCitiesSync_parsesResponseIntoDtos() throws Exception {
        RemoteDataSource dataSource = new RemoteDataSource(fakeClient(CITIES_JSON));

        List<CityDto> cities = dataSource.fetchCitiesSync();

        assertEquals(1, cities.size());
        assertEquals("Tokyo", cities.get(0).getName());
        assertEquals("JP", cities.get(0).getCountry());
    }

    @Test
    public void searchPlacesSync_parsesResponseIntoDtos() throws Exception {
        RemoteDataSource dataSource = new RemoteDataSource(fakeClient(PLACES_JSON));

        List<PlaceDto> places = dataSource.searchPlacesSync("city-1", "temple", 25);

        assertEquals(1, places.size());
        assertEquals("Senso-ji Temple", places.get(0).getName());
        assertEquals("city-1", places.get(0).getCityId());
        assertFalse(places.get(0).isHiddenGem());
        assertTrue(places.get(0).getRating() > 4.0);
    }

    @Test(expected = java.io.IOException.class)
    public void fetchCitiesSync_throwsOnHttpError() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(500)
                        .message("Server Error")
                        .body(ResponseBody.create("", MediaType.parse("application/json")))
                        .build())
                .build();

        new RemoteDataSource(client).fetchCitiesSync();
    }

    private OkHttpClient fakeClient(String json) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(json, MediaType.parse("application/json")))
                        .build())
                .build();
    }
}
