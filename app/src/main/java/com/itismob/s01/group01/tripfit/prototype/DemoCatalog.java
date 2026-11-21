package com.itismob.s01.group01.tripfit.prototype;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Illustrative discovery and price data, clearly labeled as samples in the UI. */
public final class DemoCatalog {
    public enum City {
        TOKYO("offline-tokyo", "Tokyo", "Japan", "JPY", 35.6762, 139.6503, 1200),
        MANILA("offline-manila", "Manila", "Philippines", "PHP", 14.5995, 120.9842, 200);

        public final String id, name, country, currency;
        public final double lat, lng;
        final int lowPriceThreshold;

        City(String id, String name, String country, String currency, double lat, double lng,
             int lowPriceThreshold) {
            this.id = id;
            this.name = name;
            this.country = country;
            this.currency = currency;
            this.lat = lat;
            this.lng = lng;
            this.lowPriceThreshold = lowPriceThreshold;
        }
    }

    public static final class Place {
        public final City catalogCity;
        public final String id, city, name, category, description, currency;
        public final double lat, lng, price, rating;
        public final int reviews, minutes;
        public final boolean hidden, outdoor;

        Place(String id, String city, String name, String category, double lat, double lng,
              double price, double rating, int reviews, int minutes, boolean hidden,
              boolean outdoor, String description) {
            this.id = "offline-" + id;
            this.catalogCity = city(city);
            this.city = city;
            this.name = name;
            this.category = category;
            this.lat = lat;
            this.lng = lng;
            this.price = price;
            this.rating = rating;
            this.reviews = reviews;
            this.minutes = minutes;
            this.hidden = hidden;
            this.outdoor = outdoor;
            this.description = description;
            this.currency = catalogCity.currency;
        }

        public String address() {
            return name + ", " + city;
        }

        public int priceTier() {
            if (price == 0) {
                return 0;
            }
            return price < catalogCity.lowPriceThreshold ? 1 : 2;
        }

        public ItemDetails itemDetails() {
            return new ItemDetails(price, currency, category, outdoor);
        }

        public TripSnapshot.Stop stop(int day) {
            TripSnapshot.Stop stop = new TripSnapshot.Stop();
            stop.name = name;
            stop.address = address();
            stop.category = category;
            stop.lat = lat;
            stop.lng = lng;
            stop.hasLocation = true;
            stop.cost = price;
            stop.duration = minutes;
            stop.day = day;
            stop.outdoor = outdoor;
            stop.currency = currency;
            return stop;
        }
    }

    private static final List<Place> ALL = java.util.Arrays.asList(
        new Place("senso-ji", "Tokyo", "Senso-ji Temple", "Activities",35.7148,139.7967,0,4.7,82000,60,false,true,
                "A classic Asakusa starting point."),
        new Place("skytree", "Tokyo", "Tokyo Skytree", "Activities",35.7101,139.8107,2100,4.5,92000,90,false,false,
                "Indoor observation decks over the city."),
        new Place("meiji", "Tokyo", "Meiji Shrine", "Activities",35.6764,139.6993,0,4.6,45000,60,false,true,
                "A peaceful forest walk and shrine visit."),
        new Place("shibuya", "Tokyo", "Shibuya Crossing", "Activities",35.6595,139.7004,0,4.5,31000,30,false,true,
                "Watch the city move at the iconic crossing."),
        new Place("teamlab", "Tokyo", "teamLab Planets", "Activities",35.6491,139.7898,3800,4.6,28000,90,false,false,
                "An immersive indoor art experience."),
        new Place("yanaka-ginza", "Tokyo", "Yanaka Ginza", "Activities",35.7278,139.7666,0,4.6,430,60,true,true,
                "A sample community pick for an old-town stroll."),
        new Place("nezu-museum", "Tokyo", "Nezu Museum", "Activities",35.6622,139.7170,1500,4.7,620,75,true,false,
                "An indoor alternative with art and quiet galleries."),
        new Place("asakusa-ramen-kitchen", "Tokyo", "Asakusa ramen kitchen", "Food",35.7125,139.7942,1100,4.5,320,60,true,false,
                "Illustrative restaurant close to the Asakusa stops."),
        new Place("shibuya-lunch-cafe", "Tokyo", "Shibuya lunch cafe", "Food",35.6603,139.6991,1800,4.6,870,60,false,false,
                "Illustrative cafe with a mid-range meal estimate."),
        new Place("yanaka-noodle-counter", "Tokyo", "Yanaka noodle counter", "Food",35.7285,139.7662,850,4.7,95,45,true,false,
                "Illustrative affordable neighborhood lunch."),
        new Place("fort", "Manila", "Fort Santiago", "Activities",14.5942,120.9703,75,4.6,14000,90,false,true,
                "Explore the historic walled city."),
        new Place("national-museum-of-fine-arts", "Manila", "National Museum of Fine Arts", "Activities",14.5869,120.9812,0,4.7,9000,90,false,false,
                "A free indoor art stop and rainy-day alternative."),
        new Place("rizal", "Manila", "Rizal Park", "Activities",14.5826,120.9787,0,4.6,22000,60,false,true,
                "A landmark park close to the museum district."),
        new Place("escolta-heritage-walk", "Manila", "Escolta heritage walk", "Activities",14.5995,120.9801,0,4.6,240,60,true,true,
                "A sample community pick for heritage buildings."),
        new Place("bahay-tsinoy", "Manila", "Bahay Tsinoy", "Activities",14.5914,120.9745,100,4.7,180,60,true,false,
                "An indoor cultural museum in Intramuros."),
        new Place("intramuros-lunch-spot", "Manila", "Intramuros lunch spot", "Food",14.5895,120.9754,250,4.5,450,60,false,false,
                "Illustrative local lunch near the heritage stops."),
        new Place("escolta-neighborhood-cafe", "Manila", "Escolta neighborhood cafe", "Food",14.5998,120.9805,180,4.7,120,45,true,false,
                "Illustrative budget-friendly coffee and snack stop."),
        new Place("nakamise", "Tokyo", "Nakamise Shopping Street", "Activities",35.7124,139.7967,0,4.5,12000,30,false,true,
                "Browse the stalls on the approach to Senso-ji."),
        new Place("nezu", "Tokyo", "Nezu Shrine", "Activities",35.7203,139.7609,0,4.5,2400,45,true,true,
                "Walk through torii gates in a quiet shrine garden."),
        new Place("todoroki-valley", "Tokyo", "Todoroki Valley", "Activities",35.6078,139.6467,0,4.5,900,60,true,true,
                "A wooded ravine with a stream and a small temple."),
        new Place("shimokitazawa", "Tokyo", "Shimokitazawa", "Activities",35.6615,139.6682,0,4.3,1500,60,true,true,
                "Browse vintage shops and small music venues."),
        new Place("afuri-ramen", "Tokyo", "Afuri Ramen", "Food",35.6470,139.7094,1200,4.3,2100,45,false,false,
                "A sample stop for light, citrus-flavoured ramen."),
        new Place("ichiran-shibuya", "Tokyo", "Ichiran Shibuya", "Food",35.6602,139.7004,1500,4.4,5400,45,false,false,
                "Tonkotsu ramen served in individual booths."),
        new Place("gonpachi", "Tokyo", "Gonpachi Nishiazabu", "Food",35.6603,139.7236,3800,4.2,3800,75,false,false,
                "A lively izakaya with grilled dishes and soba."),
        new Place("sushi-zanmai", "Tokyo", "Tsukiji Sushi Zanmai", "Food",35.6656,139.7707,4500,4.5,6200,60,false,false,
                "A sit-down sushi stop near the market."),
        new Place("kaikaya", "Tokyo", "Kaikaya", "Food",35.6575,139.6959,6000,4.6,1400,75,false,false,
                "A seafood izakaya with a seasonal menu."),
        new Place("daiwa-sushi", "Tokyo", "Daiwa Sushi", "Food",35.6455,139.7827,9000,4.7,2900,60,false,false,
                "A sushi counter at Toyosu Market."),
        new Place("meiji-jingu-garden", "Tokyo", "Meiji Jingu Garden", "Activities",35.6737,139.6999,500,4.5,8800,45,false,true,
                "A garden walk with a pond and seasonal flowers."),
        new Place("tokyo-national-museum", "Tokyo", "Tokyo National Museum", "Activities",35.7188,139.7764,1000,4.4,9600,90,false,false,
                "Explore Japanese art and artefacts in Ueno."),
        new Place("sumida-river-cruise", "Tokyo", "Sumida River Cruise", "Activities",35.7106,139.7975,1720,4.2,3100,60,false,true,
                "See Tokyo's bridges from the river."),
        new Place("shibuya-sky", "Tokyo", "Shibuya Sky", "Activities",35.6585,139.7021,2200,4.6,18500,60,false,true,
                "An observation deck overlooking Shibuya."),
        new Place("intramuros", "Manila", "Intramuros", "Activities",14.5896,120.9747,0,4.5,31000,90,false,true,
                "Walk between plazas, walls and historic buildings."),
        new Place("binondo", "Manila", "Binondo Food Crawl", "Food",14.6001,120.9742,350,4.6,1300,90,true,true,
                "Try a few snacks while walking through Chinatown."),
        new Place("manila-cathedral", "Manila", "Manila Cathedral", "Activities",14.5915,120.9736,0,4.6,9400,45,false,false,
                "Visit the cathedral facing Plaza de Roma."),
        new Place("san-agustin-museum", "Manila", "San Agustin Church Museum", "Activities",14.5889,120.9738,200,4.4,1100,60,true,false,
                "Tour the galleries and historic cloisters."),
        new Place("manila-bay-walk", "Manila", "Manila Bay Sunset Walk", "Activities",14.5689,120.9829,0,4.2,800,45,true,true,
                "Stroll along the bayfront near sunset.")
    );
    public static City city(String nameOrId) {
        for (City city : City.values()) {
            if (city.name.equalsIgnoreCase(nameOrId) || city.id.equals(nameOrId)) {
                return city;
            }
        }
        return null;
    }

    public static List<Place> places(String city) {
        return search(city, "");
    }

    public static List<Place> search(String city, String query) {
        City match = city(city);
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<Place> places = new ArrayList<>();
        for (Place place : ALL) {
            if (place.catalogCity == match && place.name.toLowerCase(Locale.ROOT).contains(needle)) {
                places.add(place);
            }
        }
        return places;
    }

    public static Place findById(String id) {
        for (Place place : ALL) {
            if (place.id.equals(id)) {
                return place;
            }
        }
        return null;
    }

    static Place findByName(String name) {
        String key = nameKey(name);
        for (Place place : ALL) {
            if (nameKey(place.name).equals(key)) {
                return place;
            }
        }
        return null;
    }

    public static String nameKey(String name) {
        String key = name.trim().toLowerCase(Locale.ROOT);
        switch (key) {
            case "tokyo skytree tembo deck": return "tokyo skytree";
            case "escolta street": return "escolta heritage walk";
            default: return key;
        }
    }

    public static double lat(String city) {
        City match = city(city);
        return (match == null ? City.MANILA : match).lat;
    }

    public static double lng(String city) {
        City match = city(city);
        return (match == null ? City.MANILA : match).lng;
    }

    private DemoCatalog() { }
}
