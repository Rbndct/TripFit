package com.itismob.s01.group01.tripfit.prototype;

import android.app.Activity;
import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.itismob.s01.group01.tripfit.HomeActivity;
import com.itismob.s01.group01.tripfit.TripFitApp;
import com.itismob.s01.group01.tripfit.data.Result;
import com.itismob.s01.group01.tripfit.data.frontend.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;

/** Exercises the actual prototype UI against isolated local storage; no live accounts or APIs. */
@RunWith(AndroidJUnit4.class)
public class ProposalWorkflowTest {
    private TripFitApp app;
    private DraftStore store;
    private DraftTrip trip;
    private FrontendServices previous;
    @Before public void setUp() {
        app=ApplicationProvider.getApplicationContext();
        final String prefix="proposal_qa_"+UUID.randomUUID()+"_";
        Context isolated=new ContextWrapper(app) {
            @Override public SharedPreferences getSharedPreferences(String name,int mode) { return super.getSharedPreferences(prefix+name,mode); }
        };
        store=new DraftStore(isolated); trip=new DraftTrip(); trip.name="Workflow check";
        trip.members.add("You"); trip.budget=1000; trip.foodBudget=500; trip.activityBudget=400; trip.transportBudget=100; trip.otherBudget=0;
        store.save(trip);
        previous=app.replaceFrontendServicesForTesting(new FrontendServices(store,new SampleTravelDataSource()));
    }
    @After public void tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            for(Stage stage:new Stage[]{Stage.RESUMED,Stage.PAUSED,Stage.STOPPED}) {
                for(Activity activity:new ArrayList<>(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage))) activity.finish();
            }
        });
        app.replaceFrontendServicesForTesting(previous);
    }
    private ActivityScenario<Activity> launch(Class<?> screen) {
        Intent intent=new Intent(app,screen); intent.putExtra(HomeActivity.EXTRA_TRIP_ID,trip.id); return ActivityScenario.launch(intent);
    }
    private void input(String hint,String value) {
        onView(allOf(isAssignableFrom(EditText.class),withHint(hint))).perform(scrollTo(),replaceText(value),closeSoftKeyboard());
    }
    private void tap(String label) { onView(withText(label)).perform(scrollTo(),click()); }
    private void select(String label,String value) {
        onView(withContentDescription(label)).perform(scrollTo(),click()); onData(equalTo(value)).perform(click());
    }
    private DraftTrip saved() { return store.get(trip.id); }
    private DraftTrip.Stop addStop(String name,int day,boolean booked,boolean outdoor) {
        DraftTrip.Stop stop=new DraftTrip.Stop(); stop.name=name; stop.day=day; stop.booked=booked; stop.outdoor=outdoor; stop.currency="PHP"; trip.stops.add(stop); return stop;
    }
    @Test public void tripSettingsPersistCityDatesBudgetAndCurrencyMetadata() {
        try(ActivityScenario<Activity> scenario=launch(TripSetupActivity.class)) {
            input("Trip name","Manila weekend"); select("Destination city","Manila");
            input("Start date (YYYY-MM-DD)","2026-12-05"); input("Number of days","2"); input("Budget in home currency","2000");
            tap("Save trip settings");
            DraftTrip result=saved(); assertEquals("Manila weekend",result.name); assertEquals(2,result.days);
            assertEquals("PHP",result.localCurrency()); assertEquals("Asia/Manila",result.timeZone()); assertEquals(2000,result.budget,.001);
        }
    }
    @Test public void manualStopPersistsCategoryDefaultDurationBookingAndPerLegTransport() {
        try(ActivityScenario<Activity> scenario=launch(StopEditorActivity.class)) {
            input("Destination name","Booked transfer"); input("Day (1–3)","2"); input("Stay in minutes (optional)","");
            select("Budget category","Transport"); input("Buffer before this stop (minutes)","20");
            input("Estimated cost","250"); select("Price currency","PHP"); select("Transport from previous stop","Driving");
            tap("Booked activity · lock the time"); tap("Add to itinerary");
            DraftTrip.Stop stop=saved().stops.get(0); assertEquals(2,stop.day); assertEquals(30,stop.duration);
            assertEquals(20,stop.buffer); assertTrue(stop.booked); assertEquals(540,stop.fixedMinute); assertEquals("Driving",stop.mode);
            assertEquals(250,saved().convertedCost(stop),.001);
        }
    }
    @Test public void discoveryAcceptsAMixOfPopularAndHiddenPlacesAcrossDays() {
        try(ActivityScenario<Activity> scenario=launch(DiscoverActivity.class)) {
            tap("Build a suggested itinerary"); onView(withText("Add selected stops")).perform(click());
            DraftTrip result=saved(); assertEquals(5,result.stops.size()); boolean hidden=false,popular=false;
            for(DraftTrip.Stop stop:result.stops) {
                assertTrue(stop.hasLocation); assertFalse(stop.placeId.isEmpty()); assertTrue(stop.day>=1 && stop.day<=trip.days);
                for(PlaceSuggestion place:new SampleTravelDataSource().cached(TravelQuery.from(result)).places(result.city)) if(place.matches(stop)) { hidden|=place.hidden; popular|=!place.hidden; }
            }
            assertTrue(hidden); assertTrue(popular);
        }
    }
    @Test public void alternativeProviderFeedsDiscoveryAndRefreshFailureKeepsTheDraft() {
        trip.city="Paris"; trip.destinationCurrency="EUR"; trip.destinationTimeZone="Europe/Paris"; trip.homeCurrency="USD"; store.save(trip);
        Map<String,Double> rates=new HashMap<>(); rates.put("USD",1d); rates.put("EUR",2d);
        PlaceSuggestion place=new PlaceSuggestion("opaque-vendor-id","Paris","Fixture gallery","Activities","EUR",48.86,2.34,12d,4.8,40,60,true,false,"Fixture place");
        TravelSnapshot snapshot=new TravelSnapshot(Collections.singletonList(new Destination("paris-id","Paris","EUR","Europe/Paris",48.86,2.34)),Collections.singletonList(place),
            new ExchangeRates(rates,"Fixture rates",false,1L),new SampleTravelTimes(),Collections.emptyMap(),"Fixture places","Fixture travel cache");
        TravelDataSource source=new TravelDataSource() {
            public TravelSnapshot cached(TravelQuery ignored) { return snapshot; }
            public void refresh(TravelQuery ignored,Result.Callback<TravelSnapshot> callback) { callback.onResult(Result.error("Offline")); }
        };
        app.replaceFrontendServicesForTesting(new FrontendServices(store,source));
        try(ActivityScenario<Activity> scenario=launch(DiscoverActivity.class)) {
            tap("Refresh travel information"); onView(withText("Fixture gallery")).perform(scrollTo()).check(matches(isDisplayed()));
            tap("Add to Day 1"); DraftTrip.Stop stop=saved().stops.get(0);
            assertEquals("opaque-vendor-id",stop.placeId); assertEquals(24,saved().convertedCost(stop,snapshot.rates),.001);
        }
    }
    @Test public void expenseFormPersistsItsDayAndConvertsTheOriginalCurrency() {
        try(ActivityScenario<Activity> scenario=launch(BudgetActivity.class)) {
            input("What did you spend on?","Lunch"); input("Amount","150"); select("Expense day","Day 2"); select("Paid in","JPY");
            tap("Add expense"); DraftTrip result=saved(); assertEquals(1,result.expenses.size());
            assertEquals(2,result.expenses.get(0).day); assertEquals(57,DraftBudget.spent(result,ExchangeRates.samples(),null,null),.001);
            onView(withText(containsString("Day 2 · Food"))).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }
    @Test public void foodSuggestionsUseTheSelectedDaysRemainingBudget() {
        trip.days=2; trip.dailyBudget=true; trip.budget=1000; trip.foodBudget=500;
        DraftTrip.Expense expense=new DraftTrip.Expense(); expense.amount=490; expense.day=1; trip.expenses.add(expense); store.save(trip);
        try(ActivityScenario<Activity> scenario=launch(DiscoverActivity.class)) {
            select("What would you like to find?","Food"); tap("Only show places within remaining category budget"); tap("Update suggestions");
            onView(withText(startsWith("No sample places match"))).perform(scrollTo()).check(matches(isDisplayed()));
            select("Add suggestions to","Day 2"); tap("Update suggestions");
            onView(withText("Yanaka noodle counter")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(allOf(withText("Add to Day 2"),isDescendantOfA(allOf(isAssignableFrom(com.google.android.material.card.MaterialCardView.class),hasDescendant(withText("Yanaka noodle counter")))))).perform(scrollTo(),click());
            assertEquals(2,saved().stops.get(0).day);
            assertEquals(10,DraftBudget.suggestionAllowance(saved(),ExchangeRates.samples(),"Food",1),.001);
        }
    }
    @Test public void activityBudgetFilterExcludesUnaffordableAttractions() {
        trip.activityBudget=500; store.save(trip);
        try(ActivityScenario<Activity> scenario=launch(DiscoverActivity.class)) {
            select("What would you like to find?","Activities"); tap("Only show places within remaining category budget"); tap("Update suggestions");
            onView(withText("Tokyo Skytree")).check(doesNotExist());
            onView(withText("Senso-ji Temple")).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }
    @Test public void routePreviewAndDayBalancingKeepBookingsFixed() {
        trip.days=2; DraftTrip.Stop booking=addStop("Reservation",2,true,false); booking.fixedMinute=975;
        addStop("Flexible morning",2,false,true); addStop("Flexible afternoon",2,false,true); store.save(trip);
        try(ActivityScenario<Activity> scenario=launch(PlannerActivity.class)) {
            tap("Day 2"); tap("Preview a shorter route"); onView(withText("Apply order")).perform(click());
            tap("Spread flexible stops across days"); onView(withText("Balance days")).perform(click());
            boolean moved=false;
            for(DraftTrip.Stop stop:saved().stops) if(stop.id.equals(booking.id)) { assertEquals(2,stop.day); assertEquals(975,stop.fixedMinute); }
                else if(stop.day==1) moved=true;
            assertTrue(moved);
        }
    }
    @Test public void weatherSwapMovesFlexibleStopsAndKeepsTheBookedBlock() {
        trip.days=2; trip.weather=new ArrayList<>(Arrays.asList("Clear","Clear"));
        DraftTrip.Stop park=addStop("Park",1,false,true), museum=addStop("Indoor museum",2,false,false), booking=addStop("Booked tour",1,true,true);
        booking.fixedMinute=600; store.save(trip);
        try(ActivityScenario<Activity> scenario=launch(WeatherActivity.class)) {
            select("Sample condition for Day 1","Rain"); tap("Apply sample forecast for Day 1"); tap("Indoor alternatives for Park");
            onView(withText("Swap days with Indoor museum (Day 2)")).perform(click());
            DraftTrip result=saved(); for(DraftTrip.Stop stop:result.stops) {
                if(stop.id.equals(park.id)) assertEquals(2,stop.day);
                if(stop.id.equals(museum.id)) assertEquals(1,stop.day);
                if(stop.id.equals(booking.id)) { assertEquals(1,stop.day); assertEquals(600,stop.fixedMinute); assertTrue(stop.booked); }
            }
        }
    }
    @Test public void groupJoiningVotingAndEqualCostPreviewAreConnected() {
        addStop("Shared stop",1,false,true).cost=300; store.save(trip);
        try(ActivityScenario<Activity> scenario=launch(GroupActivity.class)) {
            input("Companion name","Alex"); tap("Add companion to preview"); input("Sample join code","TRIP-DEMO"); tap("Preview joining");
            tap("Vote for Senso-ji Temple"); assertEquals(3,saved().members.size()); assertEquals(1,saved().votes.size());
            assertTrue(saved().votes.get(0).startsWith("sample:"));
            onView(withText("PHP 100 each")).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }
    @Test public void journalDraftKeepsItsLinkedStopAcrossRecreation() {
        DraftTrip.Stop temple=addStop("Temple",2,false,true); store.save(trip);
        try(ActivityScenario<Activity> scenario=launch(JournalActivity.class)) {
            select("Attach to a destination (optional)","Day 2 · Temple"); input("What would you like to remember?","A quiet morning");
            scenario.recreate(); tap("Save memory");
            DraftTrip.Entry entry=saved().journal.get(0); assertEquals(temple.id,entry.stopId); assertEquals(2,entry.day); assertEquals("A quiet morning",entry.note);
        }
    }
    @Test public void journalDisplaysALocalPhotoWithoutUploadingIt() throws Exception {
        File image=new File(app.getCacheDir(),"proposal-photo-"+trip.id+".png"); Bitmap bitmap=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888); bitmap.eraseColor(0xff196c62);
        try(FileOutputStream output=new FileOutputStream(image)) { bitmap.compress(Bitmap.CompressFormat.PNG,100,output); } bitmap.recycle();
        DraftTrip.Entry entry=new DraftTrip.Entry(); entry.note="Photo memory"; entry.photo=Uri.fromFile(image).toString(); trip.journal.add(entry); store.save(trip);
        try(ActivityScenario<Activity> scenario=launch(JournalActivity.class)) {
            onView(withContentDescription("Attached travel photo")).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }
    @Test public void calendarPreviewPersistsReviewWithoutPretendingToSync() {
        addStop("Museum",1,true,false).address="Test street"; store.save(trip);
        try(ActivityScenario<Activity> scenario=launch(SummaryActivity.class)) {
            tap("Preview Google Calendar sync"); onView(withText("Complete preview")).perform(click());
            assertTrue(saved().calendarPreview); assertTrue(TripExport.calendar(saved()).contains("LOCATION:Test street"));
            onView(withText("✓ Preview reviewed · No events have been synced to Google Calendar.")).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }
}
