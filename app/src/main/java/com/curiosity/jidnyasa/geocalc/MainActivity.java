package com.curiosity.jidnyasa.geocalc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.curiosity.jidnyasa.geocalc.webservice.WeatherService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.parceler.Parcels;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.curiosity.jidnyasa.geocalc.webservice.WeatherService.BROADCAST_WEATHER;

/* GeoCalculator by Jidnyasa Mantri and Geethanjali Sanikommu */

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    public static final int Settings_Activity = 1;
    public static final int LOCATION_SEARCH = 3;
    public static int HISTORY_RESULT = 2;
    public static String distanceUnit = "Kilometers";
    public static String bearingUnit = "Degrees";
    public Float distanceInKilometers;
    public Float bearingInDegrees;
    //DateTime datetime = new DateTime();

    ImageView p1Icon;
    ImageView p2Icon;
    TextView p1Temp;
    TextView p2Temp;
    TextView p1Summary;
    TextView p2Summary;

    DatabaseReference topRef;

    public static List<LocationLookup> allHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize the arrayList
        allHistory = new ArrayList<LocationLookup>();

        //EditText
        final EditText latitude_p1 = findViewById(R.id.latitude_p1);
        final EditText longitude_p1 = findViewById(R.id.longitude_p1);
        final EditText latitude_p2 = findViewById(R.id.latitude_p2);
        final EditText longitude_p2 = findViewById(R.id.longitude_p2);

        //TextView
        final TextView distanceAnswer = findViewById(R.id.distanceAnswer);
        final TextView bearingAnswer = findViewById(R.id.bearingAnswer);
        final TextView distanceUnits = findViewById(R.id.distanceUnits);
        final TextView bearingUnits = findViewById(R.id.bearingUnit);
        p1Temp = findViewById(R.id.sourceTemp);
        p1Summary = findViewById(R.id.sourceForecast);
        p2Temp = findViewById(R.id.destTemp);
        p2Summary = findViewById(R.id.destForecast);

        //Image
        p1Icon = findViewById(R.id.sourceImage);
        p2Icon = findViewById(R.id.destinationImage);

        //Button
        Button calculate = findViewById(R.id.calculate);
        Button clear = findViewById(R.id.clear);
        Button searchbutton = findViewById(R.id.search);

        //If calculate is clicked
        calculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (latitude_p1.length() == 0 || longitude_p1.length() == 0 ||
                        latitude_p2.length() == 0 || longitude_p2.length() == 0) {
                    //Disappear keypad on button click
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(latitude_p1.getWindowToken(), 0);
                    Toast.makeText(MainActivity.this,R.string.enterValues,Toast.LENGTH_SHORT).show();
                    return;
                }

                if (latitude_p1.length() != 0 || longitude_p1.length() != 0 ||
                        latitude_p2.length() != 0 || latitude_p2.length() != 0) {
                    //Disappear keypad on button click
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(latitude_p1.getWindowToken(), 0);
                }

                //Setting the answers to the TextViews
                unitSetter();

                // remember the calculation
                /* allHistory item = new allHistory(latitude_p1.getText().toString(),
                        longitude_p1.getText().toString(),latitude_p2.getText().toString(),
                        longitude_p2.getText().toString(),DateTime.now());
                HistoryContent.addItem(item); */
            }
        });

        //If clear is clicked
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Clear EditText
                latitude_p1.getText().clear();
                latitude_p2.getText().clear();
                longitude_p1.getText().clear();
                longitude_p2.getText().clear();

                //Clears the distance and bearing
                if (distanceAnswer.length() != 0 && bearingAnswer.length() != 0) {
                    distanceAnswer.setText("");
                    bearingAnswer.setText("");
                    distanceUnits.setText("");
                    bearingUnits.setText("");
                }

                //Disappear keypad on button click
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(latitude_p1.getWindowToken(), 0);

                //Weather View
                setWeatherViews(View.INVISIBLE);
            }
        });

        searchbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent2 = new Intent(MainActivity.this, LocationSearchActivity.class);
                startActivityForResult(intent2, LOCATION_SEARCH);
            }
        });

        //Google Play Services
        GoogleApiClient apiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

    }

    private void setWeatherViews(int visible) {
        p1Icon.setVisibility(visible);
        p2Icon.setVisibility(visible);
        p1Summary.setVisibility(visible);
        p2Summary.setVisibility(visible);
        p1Temp.setVisibility(visible);
        p2Temp.setVisibility(visible);
    }

    @Override
    public void onResume(){
        super.onResume();
        allHistory.clear();
        topRef = FirebaseDatabase.getInstance().getReference("history");
        topRef.addChildEventListener (chEvListener);

        IntentFilter weatherFilter = new IntentFilter(BROADCAST_WEATHER);
        LocalBroadcastManager.getInstance(this).registerReceiver(weatherReceiver, weatherFilter);
        this.setWeatherViews(View.INVISIBLE);
    }

    @Override
    public void onPause(){
        super.onPause();
        topRef.removeEventListener(chEvListener);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(weatherReceiver);
    }

    private ChildEventListener chEvListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            LocationLookup entry = (LocationLookup)
                    dataSnapshot.getValue(LocationLookup.class);
            entry._key = dataSnapshot.getKey();
            allHistory.add(entry);
        }
        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        }
        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            LocationLookup entry = (LocationLookup)
                    dataSnapshot.getValue(LocationLookup.class);
            List<LocationLookup> newHistory = new ArrayList<LocationLookup>();
            for (LocationLookup t : allHistory) {
                if (!t._key.equals(dataSnapshot.getKey())) {
                    newHistory.add(t);
                }
            }
            allHistory = newHistory;
        }
        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        }
        @Override
        public void onCancelled(DatabaseError databaseError) {
        }
    };

    private void unitSetter() {
        //Text views
        TextView distanceLabel = findViewById(R.id.distanceUnits);
        TextView distanceAns = findViewById(R.id.distanceAnswer);
        TextView bearingLabel = findViewById(R.id.bearingUnit);
        TextView bearingAns = findViewById(R.id.bearingAnswer);

        //EditText
        final EditText latitude_p1 = findViewById(R.id.latitude_p1);
        final EditText longitude_p1 = findViewById(R.id.longitude_p1);
        final EditText latitude_p2 = findViewById(R.id.latitude_p2);
        final EditText longitude_p2 = findViewById(R.id.longitude_p2);

        //Getting the p1 location
        Location loc1 = new Location("");
        loc1.setLatitude(Double.parseDouble(latitude_p1.getText().toString()));
        loc1.setLongitude(Double.parseDouble(longitude_p1.getText().toString()));

        //Getting the p2 location
        Location loc2 = new Location("");
        loc2.setLatitude(Double.parseDouble(latitude_p2.getText().toString()));
        loc2.setLongitude(Double.parseDouble(longitude_p2.getText().toString()));

        //Calculating the distance between p1 and p2
        Float distanceInMeters = loc1.distanceTo(loc2);
        distanceInKilometers = distanceInMeters / 1000;

        //Calculating the bearing between p1 and p2
        bearingInDegrees = loc1.bearingTo(loc2);


        if(distanceUnit.equals("Kilometers")) {
            DecimalFormat df = new DecimalFormat("###.##");
            String distInKilometers = String.valueOf(df.format(distanceInKilometers));
            distanceAns.setText(distInKilometers);
            distanceLabel.setText("Kilometers");
        }

        if(distanceUnit.equals("Miles")) {
            Double distanceInMiles = distanceInKilometers * 17.7777777778;
            DecimalFormat df = new DecimalFormat("###.##");
            String distInMiles = String.valueOf(df.format(distanceInMiles));
            distanceAns.setText(distInMiles);
            distanceLabel.setText("Miles");
        }

        if(bearingUnit.equals("Degrees")) {
            DecimalFormat df2 = new DecimalFormat("###.##");
            String bearInDegrees = String.valueOf(df2.format(bearingInDegrees));
            bearingAns.setText(bearInDegrees);
            bearingLabel.setText("Degrees");
        }

        if(bearingUnit.equals("Mils")){
            Double bearingInMils = bearingInDegrees * 0.621371;
            DecimalFormat df2 = new DecimalFormat("###.##");
            String bearInMils = String.valueOf(df2.format(bearingInMils));
            bearingAns.setText(bearInMils);
            bearingLabel.setText("Mils");
        }

        //Send Value to Firebase
        LocationLookup entry = new LocationLookup();
        entry.setOrigLat(Double.parseDouble(latitude_p1.getText().toString()));
        entry.setOrigLng(Double.parseDouble(longitude_p1.getText().toString()));
        entry.setEndLat(Double.parseDouble(latitude_p2.getText().toString()));
        entry.setEndLng(Double.parseDouble(longitude_p2.getText().toString()));
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        entry.setDateOutput(fmt.print(DateTime.now()));
        topRef.push().setValue(entry);

        //Weather
        WeatherService.startGetWeather(MainActivity.this, latitude_p1.getText().toString(), longitude_p1.getText().toString(),
                "p1");
        WeatherService.startGetWeather(MainActivity.this, latitude_p2.getText().toString(), longitude_p2.getText().toString(),
                "p2");
    }

    //View Settings om Task bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //When 'settings' are clicked
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent launchNewIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(launchNewIntent,Settings_Activity);
                return true;
            case R.id.action_history:
                Intent intent1 = new Intent(MainActivity.this, HistoryActivity.class);
                startActivityForResult(intent1, HISTORY_RESULT);
                return true;
        }
        return false;
    }

    //For Distance & Bearing Spinner
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //EditText
        EditText latitude_p1 = findViewById(R.id.latitude_p1);
        EditText longitude_p1 = findViewById(R.id.longitude_p1);
        EditText latitude_p2 = findViewById(R.id.latitude_p2);
        EditText longitude_p2 = findViewById(R.id.longitude_p2);

        if (requestCode == Settings_Activity) {
            if(resultCode == RESULT_OK) {
                //do unit switch
                distanceUnit = data.getStringExtra("Distance Unit");
                bearingUnit = data.getStringExtra("Bearing Unit");
                if(latitude_p1.length() != 0 || longitude_p1.length() != 0 ||
                        latitude_p2.length() != 0|| longitude_p2.length() != 0) {
                    unitSetter();
                } else {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(latitude_p1.getWindowToken(), 0);
                    Toast.makeText(MainActivity.this,R.string.cannotSave,Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        } else if (resultCode == HISTORY_RESULT) {

            String[] vals = data.getStringArrayExtra("item");
            latitude_p1.setText(vals[0]);
            longitude_p1.setText(vals[1]);
            latitude_p2.setText(vals[2]);
            longitude_p2.setText(vals[3]);
            unitSetter(); // code that updates the answers.
        }

        if(resultCode == LOCATION_SEARCH) {
            if (data != null && data.hasExtra("Location")) {
                Parcelable parcel = data.getParcelableExtra("Location");
                LocationLookup entry = Parcels.unwrap(parcel);
                Log.d("NewLocationSearch", "New Trip: " + entry.dateOutput);
                latitude_p1.setText(Double.toString(entry.origLat));
                longitude_p1.setText(Double.toString(entry.origLng));
                latitude_p2.setText(Double.toString(entry.endLat));
                longitude_p2.setText(Double.toString(entry.endLng));
                unitSetter();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private BroadcastReceiver weatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            double temp = bundle.getDouble("TEMPERATURE");
            String summary = bundle.getString("SUMMARY");
            String icon = bundle.getString("ICON").replaceAll("-", "_");
            String key = bundle.getString("KEY");
            int resID = getResources().getIdentifier(icon , "drawable",
                    getPackageName());
            setWeatherViews(View.VISIBLE);
            if (key.equals("p1")) {
                p1Summary.setText(summary);
                p1Temp.setText(Double.toString(temp));
                p1Icon.setImageResource(resID);
                p1Icon.setVisibility(View.INVISIBLE);
            } else {
                p2Summary.setText(summary);
                p2Temp.setText(Double.toString(temp));
                p2Icon.setImageResource(resID);
            }
        }

    };
}
