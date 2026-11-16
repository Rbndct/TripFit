package com.itismob.s01.group01.tripfit.data.frontend;

import com.itismob.s01.group01.tripfit.prototype.DraftTrip;

/** Presentation-ready place data. Map vendor DTOs into this type in an adapter. */
public final class PlaceSuggestion {
    public final String id, city, name, category, currency, description;
    public final double lat, lng, rating;
    public final Double price;
    public final Integer priceTier;
    public final int reviews, minutes;
    public final boolean hidden, outdoor;
    public PlaceSuggestion(String id,String city,String name,String category,String currency,
            double lat,double lng,Double price,double rating,int reviews,int minutes,
            boolean hidden,boolean outdoor,String description) {
        this(id,city,name,category,currency,lat,lng,price,rating,reviews,minutes,hidden,outdoor,description,null);
    }
    public PlaceSuggestion(String id,String city,String name,String category,String currency,
            double lat,double lng,Double price,double rating,int reviews,int minutes,
            boolean hidden,boolean outdoor,String description,Integer priceTier) {
        if(id==null || id.isEmpty() || name==null || name.isEmpty() || city==null || category==null || description==null
                || currency==null || !currency.matches("[A-Z]{3}") || !Double.isFinite(lat) || !Double.isFinite(lng)
                || lat< -90 || lat>90 || lng< -180 || lng>180 || (price!=null && (!Double.isFinite(price) || price<0))
                || !Double.isFinite(rating) || rating<0 || rating>5 || reviews<0 || minutes<5 || minutes>720
                || (priceTier!=null && (priceTier<0 || priceTier>4))) throw new IllegalArgumentException("Invalid place data");
        this.id=id; this.city=city; this.name=name; this.category=category; this.currency=currency;
        this.lat=lat; this.lng=lng; this.price=price; this.rating=rating; this.reviews=reviews;
        this.minutes=minutes; this.hidden=hidden; this.outdoor=outdoor; this.description=description; this.priceTier=priceTier;
    }
    public DraftTrip.Stop stop(int day) {
        DraftTrip.Stop stop=new DraftTrip.Stop(); stop.placeId=id;
        stop.name=name; stop.address=name+", "+city; stop.category=category; stop.currency=currency;
        stop.lat=lat; stop.lng=lng; stop.hasLocation=true; stop.cost=price==null?0:price; stop.costKnown=price!=null;
        stop.duration=minutes; stop.day=day; stop.outdoor=outdoor;
        return stop;
    }
    public boolean matches(DraftTrip.Stop stop) {
        return stop.placeId!=null && !stop.placeId.isEmpty()?id.equals(stop.placeId):name.equals(stop.name);
    }
    public String tierLabel() {
        if(price!=null && price==0) return "Free";
        if(priceTier==null) return "Price level unavailable";
        StringBuilder label=new StringBuilder(); for(int i=0;i<Math.min(4,Math.max(1,priceTier));i++) label.append('$'); return label.toString();
    }
}
