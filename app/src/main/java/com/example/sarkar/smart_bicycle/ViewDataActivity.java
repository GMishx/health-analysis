package com.example.sarkar.smart_bicycle;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class ViewDataActivity extends AppCompatActivity {

    DatabaseHandler db;
    List<rideData> ls;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        db = new DatabaseHandler(getBaseContext());
        ls = db.getAllRides();
        listView = (ListView) findViewById(R.id.lv);
        listView.setAdapter(new myAdapter());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view,"Are you sure you wanna clear DB?", Snackbar.LENGTH_LONG)
                        .setAction("Clear", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                db.clearRides();
                                ls.clear();
                                ((ArrayAdapter<rideData>)listView.getAdapter()).notifyDataSetChanged();
                            }
                        }).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    class myAdapter extends ArrayAdapter<rideData> {
        myAdapter() {
            super(ViewDataActivity.this, R.layout.activity_listview, ls);
        }

        @NonNull
        public View getView(int position, View convertView,
                            @NonNull ViewGroup parent) {
            LayoutInflater inflater=getLayoutInflater();
            View row=inflater.inflate(R.layout.activity_listview, parent, false);
            TextView dateTime=(TextView)row.findViewById(R.id.dateTime);
            TextView BPM=(TextView)row.findViewById(R.id.BPM);
            TextView speed=(TextView)row.findViewById(R.id.speed);
            rideData ride = ls.get(position);
            dateTime.setText(ride.getDateTime());
            BPM.setText(String.format(Locale.ENGLISH,"%d",ride.getBPM()));
            speed.setText(String.format(Locale.ENGLISH,"%f",ride.getSpeed()));
            return(row);
        }
    }
}
