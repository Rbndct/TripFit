package com.itismob.s01.group01.tripfit.prototype;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.itismob.s01.group01.tripfit.HomeActivity;
import com.itismob.s01.group01.tripfit.R;
import java.util.Locale;

/** Shared native Material building blocks for the frontend prototype. */
public abstract class PrototypeActivity extends AppCompatActivity {
    protected LinearLayout root, content, navigation;
    protected DraftStore store;
    protected DraftTrip trip;
    protected String tripId;
    protected static final int NAVY=0xff13213b, INK=0xff15181d, MUTED=0xff586374, PAPER=0xfffbfbf9, TEAL=0xff196c62;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); store=new DraftStore(this);
        tripId=getIntent().getStringExtra(HomeActivity.EXTRA_TRIP_ID);
        if(tripId!=null) trip=store.get(tripId);
    }
    protected void page(String title,String subtitle,boolean back) {
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(PAPER);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{
            androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
        });
        WindowInsetsCompat existing=ViewCompat.getRootWindowInsets(getWindow().getDecorView());
        if(existing!=null) {
            androidx.core.graphics.Insets bars=existing.getInsets(WindowInsetsCompat.Type.systemBars());
            root.setPadding(bars.left,bars.top,bars.right,bars.bottom);
        }
        setContentView(root);
        LinearLayout screenRoot=root;
        screenRoot.post(()->ViewCompat.requestApplyInsets(screenRoot));
        androidx.core.view.WindowInsetsControllerCompat controller=new androidx.core.view.WindowInsetsControllerCompat(getWindow(),root);
        controller.setAppearanceLightStatusBars(true); controller.setAppearanceLightNavigationBars(true);
        LinearLayout header=column(); header.setPadding(dp(20),dp(12),dp(20),dp(18)); header.setBackgroundColor(NAVY); root.addView(header);
        LinearLayout heading=row(); header.addView(heading);
        if(back) {
            TextView arrow=text("‹",30,Color.WHITE); arrow.setPadding(0,0,dp(18),0); arrow.setContentDescription("Go back");
            arrow.setMinWidth(dp(48)); arrow.setMinHeight(dp(48)); arrow.setOnClickListener(v->finish()); heading.addView(arrow);
        }
        TextView brand=text(title,24,Color.WHITE); brand.setTypeface(null,Typeface.BOLD); heading.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView sub=text(subtitle,13,0xffd7deea); header.addView(sub);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=column(); content.setPadding(dp(20),dp(16),dp(20),dp(24)); scroll.addView(content);
        navigation=row(); root.addView(navigation);
    }
    protected LinearLayout column() { LinearLayout layout=new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); return layout; }
    protected LinearLayout row() { LinearLayout layout=new LinearLayout(this); layout.setOrientation(LinearLayout.HORIZONTAL); layout.setGravity(android.view.Gravity.CENTER_VERTICAL); return layout; }
    protected int dp(int value) { return (int)(getResources().getDisplayMetrics().density*value+0.5f); }
    protected TextView text(String value,int size,int color) { TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setLineSpacing(dp(3),1); return t; }
    protected void label(LinearLayout target,String value) { TextView label=text(value,13,MUTED); label.setPadding(0,dp(5),0,dp(10)); target.addView(label); }
    protected void title(String value) { TextView t=text(value,20,INK); t.setTypeface(null,Typeface.BOLD); t.setPadding(0,dp(14),0,dp(10)); content.addView(t); }
    protected void notice(String value) { LinearLayout card=card(content); label(card,value); }
    protected LinearLayout card(LinearLayout target) {
        MaterialCardView card=new MaterialCardView(this); card.setRadius(dp(18)); card.setCardElevation(0); card.setCardBackgroundColor(Color.WHITE);
        card.setStrokeWidth(dp(1)); card.setStrokeColor(0xffe0e4e9);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(12); target.addView(card,lp);
        LinearLayout inside=column(); inside.setPadding(dp(16),dp(14),dp(16),dp(14)); card.addView(inside); return inside;
    }
    protected MaterialButton button(LinearLayout target,String title,Runnable action) {
        MaterialButton button=new MaterialButton(this); button.setText(title); button.setAllCaps(false); button.setMinHeight(dp(48));
        button.setOnClickListener(v->{hideKeyboard(); action.run();}); target.addView(button,new LinearLayout.LayoutParams(-1,-2)); return button;
    }
    protected TextInputEditText field(LinearLayout target,String hint,String value,int inputType) {
        TextInputLayout frame=new TextInputLayout(this); frame.setHint(hint); frame.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(8); lp.bottomMargin=dp(10); target.addView(frame,lp);
        TextInputEditText input=new TextInputEditText(this); input.setText(value); input.setInputType(inputType); input.setTextColor(INK); input.setTextSize(15);
        frame.addView(input); return input;
    }
    protected Spinner choices(LinearLayout target,String title,String[] values,String selected) {
        label(target,title); Spinner spinner=new Spinner(this); spinner.setContentDescription(title);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter);
        for(int i=0;i<values.length;i++) if(values[i].equals(selected)) spinner.setSelection(i);
        target.addView(spinner,new LinearLayout.LayoutParams(-1,dp(48))); return spinner;
    }
    protected CheckBox check(LinearLayout target,String title,boolean selected) { CheckBox box=new CheckBox(this); box.setText(title); box.setTextColor(INK); box.setChecked(selected); target.addView(box); return box; }
    protected void open(Class<?> activity) { Intent intent=new Intent(this,activity); if(tripId!=null) intent.putExtra(HomeActivity.EXTRA_TRIP_ID,tripId); startActivity(intent); }
    protected void openFeature(String name) {
        try { open(Class.forName(getPackageName()+".prototype."+name)); }
        catch(ClassNotFoundException error) { toast("This screen is not included in this build."); }
    }
    protected void toast(String value) { Toast.makeText(this,value,Toast.LENGTH_SHORT).show(); }
    protected String value(EditText field) { return field.getText().toString().trim(); }
    protected double number(EditText field,double min,double max) {
        try { double value=Double.parseDouble(value(field)); if(Double.isFinite(value)&&value>=min&&value<=max) return value; } catch(NumberFormatException ignored) {}
        field.setError("Enter a number from "+min+" to "+max); throw new IllegalArgumentException();
    }
    protected String money(double amount,String currency) { return String.format(Locale.US,"%s %,.0f",currency,amount); }
    protected void hideKeyboard() { View focus=getCurrentFocus(); if(focus!=null) ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0); }
    protected void save() { store.save(trip); }
    protected void reload() { if(tripId!=null) trip=store.get(tripId); }
    protected String date(int day) {
        try {
            java.text.SimpleDateFormat format=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US); format.setLenient(false);
            java.util.Calendar calendar=java.util.Calendar.getInstance(); calendar.setTime(format.parse(trip.startDate)); calendar.add(java.util.Calendar.DATE,day-1);
            return new java.text.SimpleDateFormat("EEE, MMM d",Locale.US).format(calendar.getTime());
        } catch(java.text.ParseException e) { return "Day "+day; }
    }
}
