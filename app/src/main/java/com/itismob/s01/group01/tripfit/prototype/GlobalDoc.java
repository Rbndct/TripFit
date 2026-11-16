package com.itismob.s01.group01.tripfit.prototype;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/** Persisted DTO for settings and per-item maps that are keyed by globally unique item ids. */
final class GlobalDoc {

    @SerializedName("homeCurrency")
    String homeCurrency;
    @SerializedName("bufferMin")
    Integer bufferMin;
    @SerializedName("legModes")
    Map<String, String> legModes = new HashMap<>();
    @SerializedName("stopBuffers")
    Map<String, Integer> stopBuffers = new HashMap<>();
    @SerializedName("itemDetails")
    Map<String, ItemDetails> itemDetails = new HashMap<>();

    GlobalDoc() { }
}
