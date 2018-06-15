package com.curiosity.jidnyasa.geocalc;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.parceler.Parcels;

import java.util.Calendar;
import java.util.Locale;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LocationSearchActivity extends AppCompatActivity {

    int PLACE_AUTOCOMPLETE_REQUEST_CODE_SOURCE = 1;
    int PLACE_AUTOCOMPLETE_REQUEST_CODE_DEST = 2;

    private static final String TAG = "NewLocationSearch";

    @BindView(R.id.sourceLocation) TextView sLoc;
    @BindView(R.id.destLocation) TextView dLoc;
    @BindView(R.id.calcSearchDate) TextView csDate;

    private LocationLookup locLookup;

    private DateTime startDate;
    Calendar myCalender;
    int day, month, year;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_search);
        ButterKnife.bind(this);

        locLookup = new LocationLookup();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Set Today's date
        DateTime today = DateTime.now();
        csDate.setText(formatted(today));
        startDate = today;

        //Calender instantiation
        myCalender = Calendar.getInstance();
        day = myCalender.get(Calendar.DAY_OF_MONTH);
        month = myCalender.get(Calendar.MONTH);
        year = myCalender.get(Calendar.YEAR);
        month = month + 1;

        //Calculation Date Function
        csDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DatePickerDialog dp = new DatePickerDialog(LocationSearchActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                    public void onDateSet(DatePicker view, int year, int month, int day) {
                        startDate = new DateTime(year, month + 1, day, 0, 0);
                        csDate.setText(formatted(startDate));
                    }
                }, year, month, day);

                dp.show();
            }
        });

        //Source location
        sLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN).build(LocationSearchActivity.this);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE_SOURCE);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });

        //Destination location
        dLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN).build(LocationSearchActivity.this);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE_DEST);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });

        //FAB button
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent result = new Intent();

                //Passes the date
                DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
                locLookup.dateOutput = String.valueOf(startDate);

                // add more code to initialize the rest of the fields
                Parcelable parcel = Parcels.wrap(locLookup);
                result.putExtra("Location", parcel);
                setResult(MainActivity.LOCATION_SEARCH, result);
                finish();
            }
        });
    }

    private String formatted(DateTime d) {
        return d.monthOfYear().getAsShortText(Locale.getDefault()) + " " +d.getDayOfMonth() + ", " + d.getYear();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE_SOURCE) {
            if (resultCode == RESULT_OK) {
                Place pl = PlaceAutocomplete.getPlace(this, data);
                sLoc.setText(pl.getName());
                locLookup.origLat = pl.getLatLng().latitude;
                locLookup.origLng = pl.getLatLng().longitude;
                Log.i(TAG, "onActivityResult: " + pl.getName() + "/" + pl.getAddress());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status stat = PlaceAutocomplete.getStatus(this, data);
                Log.d(TAG, "onActivityResult: ");
            } else if (requestCode == RESULT_CANCELED) {
                System.out.println("Cancelled by the user");
            }
        }

        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE_DEST) {
            if (resultCode == RESULT_OK) {
                Place p2 = PlaceAutocomplete.getPlace(this, data);
                dLoc.setText(p2.getName());
                locLookup.endLat = p2.getLatLng().latitude;
                locLookup.endLng = p2.getLatLng().longitude;
                Log.i(TAG, "onActivityResult: " + p2.getName() + "/" + p2.getAddress());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status stat = PlaceAutocomplete.getStatus(this, data);
                Log.d(TAG, "onActivityResult: ");
            } else if (requestCode == RESULT_CANCELED) {
                System.out.println("Cancelled by the user");
            } else super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
