package com.itismob.s01.group01.tripfit.prototype;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.itismob.s01.group01.tripfit.MapPickerActivity;
import java.util.ArrayList;
import java.util.List;

public class StopEditorActivity extends PrototypeActivity {
    private DraftTrip.Stop stop;
    private boolean editing;
    private TextView location;
    private TextInputEditText name, duration, cost;
    private ActivityResultLauncher<Intent> mapPicker;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); if(trip==null) { finish(); return; }
        String id=getIntent().getStringExtra("stop_id");
        for(DraftTrip.Stop item:trip.stops) if(item.id.equals(id)) { stop=item; editing=true; }
        if(stop==null) { stop=new DraftTrip.Stop(); stop.day=getIntent().getIntExtra("day",1); }
        if(state!=null) { stop=new com.google.gson.Gson().fromJson(state.getString("draft"),DraftTrip.Stop.class); }
        mapPicker=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),result->{
            if(result.getResultCode()!=Activity.RESULT_OK || result.getData()==null) return;
            stop.lat=result.getData().getDoubleExtra(MapPickerActivity.EXTRA_RESULT_LAT,0);
            stop.lng=result.getData().getDoubleExtra(MapPickerActivity.EXTRA_RESULT_LNG,0); stop.hasLocation=true;
            if(value(name).isEmpty()) name.setText("Pinned destination"); updateLocation();
        });
        render();
    }
    private void render() {
        page(editing?"Edit destination":"Add a destination","Search sample places, type a name or drop a pin.",true);
        TextInputEditText search=field(content,"Search sample places","",InputType.TYPE_CLASS_TEXT);
        button(content,"Search in "+trip.city,()->{
            List<DemoCatalog.Place> matches=new ArrayList<>(); List<String> names=new ArrayList<>();
            for(DemoCatalog.Place place:DemoCatalog.places(trip.city)) if(place.name.toLowerCase(java.util.Locale.ROOT).contains(value(search).toLowerCase(java.util.Locale.ROOT))) { matches.add(place); names.add(place.name); }
            if(matches.isEmpty()) { toast("No sample matches. Add a name or choose a map pin."); return; }
            new MaterialAlertDialogBuilder(this).setTitle("Sample places in "+trip.city).setItems(names.toArray(new String[0]),(d,index)->{
                DemoCatalog.Place p=matches.get(index); stop.lat=p.lat; stop.lng=p.lng; stop.hasLocation=true;
                stop.address=p.name+", "+p.city; stop.outdoor=p.outdoor; stop.category=p.category;
                name.setText(p.name); duration.setText(String.valueOf(p.minutes)); cost.setText(String.valueOf(p.price));
                category.setSelection(p.category.equals("Food")?1:0); outdoor.setChecked(p.outdoor); updateLocation();
            }).show();
        });
        name=field(content,"Destination name",stop.name,InputType.TYPE_CLASS_TEXT);
        location=text("",14,TEAL); content.addView(location); updateLocation();
        button(content,"Pick or adjust location on map",()->{
            Intent intent=new Intent(this,MapPickerActivity.class);
            intent.putExtra(MapPickerActivity.EXTRA_CITY_LAT,stop.hasLocation?stop.lat:DemoCatalog.lat(trip.city));
            intent.putExtra(MapPickerActivity.EXTRA_CITY_LNG,stop.hasLocation?stop.lng:DemoCatalog.lng(trip.city)); mapPicker.launch(intent);
        });
        TextInputEditText day=field(content,"Day (1–"+trip.days+")",String.valueOf(stop.day),InputType.TYPE_CLASS_NUMBER);
        duration=field(content,"Stay in minutes (blank = 60)",String.valueOf(stop.duration),InputType.TYPE_CLASS_NUMBER);
        TextInputEditText buffer=field(content,"Buffer before this stop (minutes)",String.valueOf(stop.buffer),InputType.TYPE_CLASS_NUMBER);
        category=choices(content,"Budget category",new String[]{"Activities","Food","Transport","Other"},stop.category);
        cost=field(content,"Estimated cost ("+trip.localCurrency()+")",String.valueOf(stop.cost),InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        label(content,"Prices are sample estimates. Blank stay duration defaults to 60 minutes; food recommendations use their own suggested duration.");
        Spinner transport=choices(content,"Transport from previous stop",new String[]{"Suggested","Walking","Cycling","Driving","Transit"},stop.mode);
        CheckBox booked=check(content,"Booked activity · lock the time",stop.booked);
        TextView fixed=text("Fixed time: "+RoutePreview.time(stop.fixedMinute),16,INK); content.addView(fixed);
        button(content,"Choose fixed time",()->new TimePickerDialog(this,(picker,hour,minute)->{
            stop.fixedMinute=hour*60+minute; fixed.setText("Fixed time: "+RoutePreview.time(stop.fixedMinute));
        },stop.fixedMinute/60,stop.fixedMinute%60,true).show());
        outdoor=check(content,"Outdoor activity (check weather)",stop.outdoor);
        button(content,editing?"Save changes":"Add to itinerary",()->{
            if(value(name).isEmpty()) { name.setError("Enter a destination name"); return; }
            try {
                stop.day=(int)number(day,1,trip.days); stop.duration=value(duration).isEmpty()?60:(int)number(duration,5,720);
                stop.buffer=(int)number(buffer,0,180); stop.cost=number(cost,0,100000000); stop.name=value(name);
                stop.category=category.getSelectedItem().toString(); stop.mode=transport.getSelectedItem().toString(); stop.booked=booked.isChecked(); stop.outdoor=outdoor.isChecked();
                if(editing) { for(int i=0;i<trip.stops.size();i++) if(trip.stops.get(i).id.equals(stop.id)) trip.stops.set(i,stop); }
                else trip.stops.add(stop); save(); finish();
            } catch(IllegalArgumentException ignored) {}
        });
        if(editing) button(content,"Remove stop",()->new MaterialAlertDialogBuilder(this).setTitle("Remove this stop?")
            .setMessage("Only this local draft is changed.").setNegativeButton("Keep",null).setPositiveButton("Remove",(d,w)->{
                trip.stops.removeIf(s->s.id.equals(stop.id)); save(); finish();
            }).show());
    }
    private Spinner category;
    private CheckBox outdoor;
    private void updateLocation() { location.setText(stop.hasLocation?String.format(java.util.Locale.US,"Selected pin: %.5f, %.5f",stop.lat,stop.lng):"No pin selected · you can still add a stop by name"); }
    @Override protected void onSaveInstanceState(Bundle state) {
        stop.name=value(name); state.putString("draft",new com.google.gson.Gson().toJson(stop)); super.onSaveInstanceState(state);
    }
}
