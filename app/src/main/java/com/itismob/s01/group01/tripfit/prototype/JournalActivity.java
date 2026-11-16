package com.itismob.s01.group01.tripfit.prototype;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.io.InputStream;

public class JournalActivity extends PrototypeActivity {
    private ActivityResultLauncher<String[]> photoPicker;
    private String selectedPhoto="", editingId="", draftNote="", draftStopId="";
    private int draftDay=1;
    private TextInputEditText note;
    private TextView photoStatus;
    private Spinner day, stop;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); if(trip==null) { finish(); return; }
        if(state!=null) { selectedPhoto=state.getString("photo",""); draftNote=state.getString("note",""); editingId=state.getString("editing",""); draftDay=state.getInt("day",1); draftStopId=state.getString("stop",""); }
        photoPicker=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{
            if(uri==null) return;
            try { getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); selectedPhoto=uri.toString(); photoStatus.setText("Photo attached · visible only in this local journal"); }
            catch(SecurityException e) { toast("Could not keep access to this photo. Choose another image."); }
        });
        render();
    }
    private void render() {
        page("Bring the memories home",trip.name+" · Photos & journal",true);
        notice("Your local trip journal · Attach a photo or a short note to a day or destination. Photos stay on this device and are not uploaded.");
        title(editingId.isEmpty()?"A moment worth keeping":"Edit your memory");
        String[] days=new String[trip.days]; for(int i=0;i<days.length;i++) days[i]="Day "+(i+1);
        day=choices(content,"Trip day",days,"Day "+draftDay);
        String[] stops=new String[trip.stops.size()+1]; stops[0]="Whole day"; for(int i=0;i<trip.stops.size();i++) stops[i+1]="Day "+trip.stops.get(i).day+" · "+trip.stops.get(i).name;
        stop=choices(content,"Attach to a destination (optional)",stops,stops[0]);
        for(int i=0;i<trip.stops.size();i++) if(trip.stops.get(i).id.equals(draftStopId)) stop.setSelection(i+1);
        note=field(content,"What would you like to remember?",draftNote,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE); note.setMinLines(3);
        photoStatus=text(selectedPhoto.isEmpty()?"No photo selected":"Photo attached",14,MUTED); content.addView(photoStatus);
        button(content,"Choose a photo",()->photoPicker.launch(new String[]{"image/*"}));
        if(!selectedPhoto.isEmpty()) button(content,"Remove photo from this draft",()->{draftNote=value(note);draftDay=day.getSelectedItemPosition()+1;draftStopId=selectedStopId();selectedPhoto="";render();});
        button(content,"Save memory",()->{
            if(value(note).isEmpty() && selectedPhoto.isEmpty()) { note.setError("Add a note or choose a photo"); return; }
            DraftTrip.Entry entry=new DraftTrip.Entry(); if(!editingId.isEmpty()) entry.id=editingId;
            entry.day=day.getSelectedItemPosition()+1; entry.note=value(note); entry.photo=selectedPhoto;
            if(stop.getSelectedItemPosition()>0) { DraftTrip.Stop linked=trip.stops.get(stop.getSelectedItemPosition()-1); entry.stopId=linked.id; entry.day=linked.day; }
            trip.journal.removeIf(e->e.id.equals(entry.id)); trip.journal.add(entry); save();
            selectedPhoto=""; draftNote=""; editingId=""; draftStopId=""; draftDay=1; render();
        });
        title("Your trip, remembered");
        if(trip.journal.isEmpty()) label(content,"No entries yet. Save your first travel memory above.");
        for(DraftTrip.Entry entry:trip.journal) {
            LinearLayout card=card(content); String target="Day "+DraftRules.journalDay(trip,entry);
            for(DraftTrip.Stop item:trip.stops) if(item.id.equals(entry.stopId)) target+=" · "+item.name;
            card.addView(text(target,18,INK));
            if(!entry.note.isEmpty()) label(card,entry.note);
            if(!entry.photo.isEmpty()) addPhoto(card,entry.photo);
            button(card,"Edit memory",()->{ editingId=entry.id; draftNote=entry.note; selectedPhoto=entry.photo; draftStopId=entry.stopId; draftDay=DraftRules.journalDay(trip,entry); render(); });
            button(card,"Remove memory",()->new MaterialAlertDialogBuilder(this).setTitle("Remove this journal entry?").setMessage("The original photo will stay in your gallery.").setNegativeButton("Keep",null).setPositiveButton("Remove",(d,w)->{trip.journal.remove(entry);save();render();}).show());
        }
    }
    private void addPhoto(LinearLayout card,String path) {
        try {
            Uri uri=Uri.parse(path); BitmapFactory.Options options=new BitmapFactory.Options(); options.inJustDecodeBounds=true;
            try(InputStream input=getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(input,null,options); }
            options.inSampleSize=1; while(options.outWidth/options.inSampleSize>1200 || options.outHeight/options.inSampleSize>1200) options.inSampleSize*=2;
            options.inJustDecodeBounds=false;
            Bitmap bitmap; try(InputStream input=getContentResolver().openInputStream(uri)) { bitmap=BitmapFactory.decodeStream(input,null,options); }
            if(bitmap==null) { label(card,"This image cannot be displayed. Choose another photo."); return; }
            ImageView image=new ImageView(this); image.setImageBitmap(bitmap); image.setScaleType(ImageView.ScaleType.CENTER_CROP); image.setContentDescription("Attached travel photo"); card.addView(image,new LinearLayout.LayoutParams(-1,dp(190)));
        } catch(java.io.IOException|SecurityException e) { label(card,"This photo is no longer accessible. You can choose it again by editing this memory."); }
    }
    private String selectedStopId() { return stop.getSelectedItemPosition()>0?trip.stops.get(stop.getSelectedItemPosition()-1).id:""; }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString("photo",selectedPhoto); state.putString("note",value(note)); state.putString("editing",editingId); state.putString("stop",selectedStopId()); state.putInt("day",day.getSelectedItemPosition()+1); super.onSaveInstanceState(state); }
}
