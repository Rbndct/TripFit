package com.itismob.s01.group01.tripfit.prototype;

import java.util.ArrayList;
import java.util.List;

/** Illustrative discovery and price data, clearly labeled as samples in the UI. */
public final class DemoCatalog {
    public static class Place {
        public final String city, name, category, description;
        public final double lat, lng, price, rating;
        public final int reviews, minutes;
        public final boolean hidden, outdoor;
        Place(String city, String name, String category, double lat, double lng, double price,
              double rating, int reviews, int minutes, boolean hidden, boolean outdoor, String description) {
            this.city=city; this.name=name; this.category=category; this.lat=lat; this.lng=lng;
            this.price=price; this.rating=rating; this.reviews=reviews; this.minutes=minutes;
            this.hidden=hidden; this.outdoor=outdoor; this.description=description;
        }
        public DraftTrip.Stop stop(int day) {
            DraftTrip.Stop stop = new DraftTrip.Stop();
            stop.name=name; stop.address=name+", "+city; stop.category=category;
            stop.lat=lat; stop.lng=lng; stop.hasLocation=true; stop.cost=price;
            stop.duration=minutes; stop.day=day; stop.outdoor=outdoor;
            return stop;
        }
    }
    private static final List<Place> ALL = java.util.Arrays.asList(
        new Place("Tokyo","Senso-ji Temple","Activities",35.7148,139.7967,0,4.7,82000,60,false,true,"A classic Asakusa starting point."),
        new Place("Tokyo","Tokyo Skytree","Activities",35.7101,139.8107,2100,4.5,92000,90,false,false,"Indoor observation decks over the city."),
        new Place("Tokyo","Meiji Shrine","Activities",35.6764,139.6993,0,4.6,45000,60,false,true,"A peaceful forest walk and shrine visit."),
        new Place("Tokyo","Shibuya Crossing","Activities",35.6595,139.7004,0,4.5,31000,30,false,true,"Watch the city move at the iconic crossing."),
        new Place("Tokyo","teamLab Planets","Activities",35.6491,139.7898,3800,4.6,28000,90,false,false,"An immersive indoor art experience."),
        new Place("Tokyo","Yanaka Ginza","Activities",35.7278,139.7666,0,4.6,430,60,true,true,"A sample community pick for an old-town stroll."),
        new Place("Tokyo","Nezu Museum","Activities",35.6622,139.7170,1500,4.7,620,75,true,false,"An indoor alternative with art and quiet galleries."),
        new Place("Tokyo","Asakusa ramen kitchen","Food",35.7125,139.7942,1100,4.5,320,60,true,false,"Illustrative restaurant close to the Asakusa stops."),
        new Place("Tokyo","Shibuya lunch cafe","Food",35.6603,139.6991,1800,4.6,870,60,false,false,"Illustrative cafe with a mid-range meal estimate."),
        new Place("Tokyo","Yanaka noodle counter","Food",35.7285,139.7662,850,4.7,95,45,true,false,"Illustrative affordable neighborhood lunch."),
        new Place("Manila","Fort Santiago","Activities",14.5942,120.9703,75,4.6,14000,90,false,true,"Explore the historic walled city."),
        new Place("Manila","National Museum of Fine Arts","Activities",14.5869,120.9812,0,4.7,9000,90,false,false,"A free indoor art stop and rainy-day alternative."),
        new Place("Manila","Rizal Park","Activities",14.5826,120.9787,0,4.6,22000,60,false,true,"A landmark park close to the museum district."),
        new Place("Manila","Escolta heritage walk","Activities",14.5995,120.9801,0,4.6,240,60,true,true,"A sample community pick for heritage buildings."),
        new Place("Manila","Bahay Tsinoy","Activities",14.5914,120.9745,100,4.7,180,60,true,false,"An indoor cultural museum in Intramuros."),
        new Place("Manila","Intramuros lunch spot","Food",14.5895,120.9754,250,4.5,450,60,false,false,"Illustrative local lunch near the heritage stops."),
        new Place("Manila","Escolta neighborhood cafe","Food",14.5998,120.9805,180,4.7,120,45,true,false,"Illustrative budget-friendly coffee and snack stop.")
    );
    public static List<Place> places(String city) {
        List<Place> places=new ArrayList<>();
        for(Place place:ALL) if(place.city.equals(city)) places.add(place);
        return places;
    }
    public static double lat(String city) { return city.equals("Tokyo")?35.6762:14.5995; }
    public static double lng(String city) { return city.equals("Tokyo")?139.6503:120.9842; }
    private DemoCatalog() {}
}
