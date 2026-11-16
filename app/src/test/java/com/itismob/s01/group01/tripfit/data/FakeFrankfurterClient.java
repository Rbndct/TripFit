package com.itismob.s01.group01.tripfit.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Test double for Frankfurter: answers by path and records requests; never touches the network. */
final class FakeFrankfurterClient {

    static final String CURRENCIES_JSON =
            "{\"JPY\":\"Japanese Yen\",\"PHP\":\"Philippine Peso\",\"USD\":\"United States Dollar\","
                    + "\"EUR\":\"Euro\"}";
    static final String USD_RATES_JSON =
            "{\"amount\":1.0,\"base\":\"USD\",\"date\":\"2026-11-13\","
                    + "\"rates\":{\"EUR\":0.9,\"JPY\":160.0,\"PHP\":64.0}}";

    private final List<Request> requests = new ArrayList<>();
    private final int status;
    private final String currenciesBody;
    private final String ratesBody;

    FakeFrankfurterClient() {
        this(200, CURRENCIES_JSON, USD_RATES_JSON);
    }

    FakeFrankfurterClient(int status, String currenciesBody, String ratesBody) {
        this.status = status;
        this.currenciesBody = currenciesBody;
        this.ratesBody = ratesBody;
    }

    static FakeFrankfurterClient failingWith(int status) {
        return new FakeFrankfurterClient(status, "{\"message\":\"error\"}", "{\"message\":\"error\"}");
    }

    List<Request> requests() {
        return Collections.unmodifiableList(requests);
    }

    OkHttpClient client() {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    requests.add(request);
                    String path = request.url().encodedPath();
                    String body = path.endsWith("/currencies") ? currenciesBody : ratesBody;
                    return new Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(status)
                            .message(status == 200 ? "OK" : "Error")
                            .body(ResponseBody.create(body, MediaType.parse("application/json")))
                            .build();
                })
                .build();
    }
}
