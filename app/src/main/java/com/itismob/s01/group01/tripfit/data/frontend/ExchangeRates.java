package com.itismob.s01.group01.tripfit.data.frontend;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable rate snapshot. Every value expresses one currency unit in the same base currency. */
public final class ExchangeRates {
    private final Map<String,Double> values;
    public final String description;
    public final boolean sample;
    public final long fetchedAtMillis;
    public ExchangeRates(Map<String,Double> values,String description,boolean sample,long fetchedAtMillis) {
        if(values==null || values.isEmpty()) throw new IllegalArgumentException("No exchange rates");
        for(Map.Entry<String,Double> entry:values.entrySet()) {
            if(entry.getKey()==null || !entry.getKey().matches("[A-Z]{3}") || entry.getValue()==null
                    || !Double.isFinite(entry.getValue()) || entry.getValue()<=0) throw new IllegalArgumentException("Invalid exchange rate");
        }
        this.values=Collections.unmodifiableMap(new LinkedHashMap<>(values));
        this.description=description; this.sample=sample; this.fetchedAtMillis=fetchedAtMillis;
    }
    public boolean supports(String currency) { return values.containsKey(currency); }
    public String[] currencies() { return values.keySet().toArray(new String[0]); }
    public double convert(double amount,String from,String to) {
        if(!Double.isFinite(amount) || amount<0) throw new IllegalArgumentException("Invalid amount");
        if(!supports(from) || !supports(to)) throw new IllegalArgumentException("Missing exchange rate");
        double converted=amount*values.get(from)/values.get(to);
        if(!Double.isFinite(converted)) throw new IllegalArgumentException("Amount is too large");
        return converted;
    }
    public static ExchangeRates samples() {
        Map<String,Double> rates=new LinkedHashMap<>(); rates.put("PHP",1d); rates.put("JPY",.38); rates.put("USD",58d); rates.put("EUR",63d);
        return new ExchangeRates(rates,"Sample rates · 1 JPY = 0.38 PHP; 1 USD = 58 PHP; 1 EUR = 63 PHP. Not live quotes.",true,0);
    }
}
