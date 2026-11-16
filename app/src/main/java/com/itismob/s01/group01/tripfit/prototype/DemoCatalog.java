package com.itismob.s01.group01.tripfit.prototype;

import com.itismob.s01.group01.tripfit.data.frontend.PlaceSuggestion;

import java.util.ArrayList;
import java.util.List;

/** Illustrative discovery and price data, clearly labeled as samples in the UI. */
public final class DemoCatalog {
    private static final List<PlaceSuggestion> ALL = java.util.Arrays.asList(
        place("Tokyo","Senso-ji Temple","Activities",35.7148,139.7967,0,4.7,82000,60,false,true,"A classic Asakusa starting point."),
        place("Tokyo","Tokyo Skytree","Activities",35.7101,139.8107,2100,4.5,92000,90,false,false,"Indoor observation decks over the city."),
        place("Tokyo","Meiji Shrine","Activities",35.6764,139.6993,0,4.6,45000,60,false,true,"A peaceful forest walk and shrine visit."),
        place("Tokyo","Shibuya Crossing","Activities",35.6595,139.7004,0,4.5,31000,30,false,true,"Watch the city move at the iconic crossing."),
        place("Tokyo","teamLab Planets","Activities",35.6491,139.7898,3800,4.6,28000,90,false,false,"An immersive indoor art experience."),
        place("Tokyo","Yanaka Ginza","Activities",35.7278,139.7666,0,4.6,430,60,true,true,"A sample community pick for an old-town stroll."),
        place("Tokyo","Nezu Museum","Activities",35.6622,139.7170,1500,4.7,620,75,true,false,"An indoor alternative with art and quiet galleries."),
        place("Tokyo","Asakusa ramen kitchen","Food",35.7125,139.7942,1100,4.5,320,60,true,false,"Illustrative restaurant close to the Asakusa stops."),
        place("Tokyo","Shibuya lunch cafe","Food",35.6603,139.6991,1800,4.6,870,60,false,false,"Illustrative cafe with a mid-range meal estimate."),
        place("Tokyo","Yanaka noodle counter","Food",35.7285,139.7662,850,4.7,95,45,true,false,"Illustrative affordable neighborhood lunch."),
        place("Manila","Fort Santiago","Activities",14.5942,120.9703,75,4.6,14000,90,false,true,"Explore the historic walled city."),
        place("Manila","National Museum of Fine Arts","Activities",14.5869,120.9812,0,4.7,9000,90,false,false,"A free indoor art stop and rainy-day alternative."),
        place("Manila","Rizal Park","Activities",14.5826,120.9787,0,4.6,22000,60,false,true,"A landmark park close to the museum district."),
        place("Manila","Escolta heritage walk","Activities",14.5995,120.9801,0,4.6,240,60,true,true,"A sample community pick for heritage buildings."),
        place("Manila","Bahay Tsinoy","Activities",14.5914,120.9745,100,4.7,180,60,true,false,"An indoor cultural museum in Intramuros."),
        place("Manila","Intramuros lunch spot","Food",14.5895,120.9754,250,4.5,450,60,false,false,"Illustrative local lunch near the heritage stops."),
        place("Manila","Escolta neighborhood cafe","Food",14.5998,120.9805,180,4.7,120,45,true,false,"Illustrative budget-friendly coffee and snack stop.")
    );
    public static List<PlaceSuggestion> places(String city) {
        List<PlaceSuggestion> places=new ArrayList<>();
        for(PlaceSuggestion place:ALL) if(place.city.equals(city)) places.add(place);
        return places;
    }
    public static double lat(String city) { return city.equals("Tokyo")?35.6762:14.5995; }
    public static double lng(String city) { return city.equals("Tokyo")?139.6503:120.9842; }
    private static PlaceSuggestion place(String city,String name,String category,double lat,double lng,
            double price,double rating,int reviews,int minutes,boolean hidden,boolean outdoor,String description) {
        return new PlaceSuggestion("sample:"+city+":"+name,city,name,category,city.equals("Tokyo")?"JPY":"PHP",
                lat,lng,price,rating,reviews,minutes,hidden,outdoor,description,price==0?0:price<(city.equals("Tokyo")?1200:200)?1:2);
    }
    private DemoCatalog() {}
}
