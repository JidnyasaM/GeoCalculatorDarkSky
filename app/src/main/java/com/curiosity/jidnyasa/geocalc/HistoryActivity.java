package com.curiosity.jidnyasa.geocalc;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.parceler.Parcels;

import java.util.List;

import static com.curiosity.jidnyasa.geocalc.MainActivity.allHistory;


public class HistoryActivity extends AppCompatActivity
        implements HistoryFragment.OnListFragmentInteractionListener {

    //List<LocationLookup> allHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //allHistory = MainActivity.allHistory;

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Click on one of the options.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }


    @Override
    public void onListFragmentInteraction(LocationLookup item) {
        System.out.println("Interact!");
        Intent intent = new Intent();
         String[] vals = {Double.toString(item.origLat), Double.toString(item.origLng),
                Double.toString(item.endLat), Double.toString(item.endLng)};
       // Parcelable parcel = Parcels.wrap(allHistory);
        intent.putExtra("item", vals);
        setResult(MainActivity.HISTORY_RESULT,intent);
        finish();
    }

}
