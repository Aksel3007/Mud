package com.Aksel_Stark.mud;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class TrailActivity extends AppCompatActivity {

    TextView trailName;
    TextView RainLastDay;
    Button removeButton;
    Button backButton;
    Intent intentFromMain;
    Trail trail;

    RequestQueue queue; //For Volley

    ArrayList<String> rawWeatherData;

    final int secsPrDay = 86400;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trail);

        intentFromMain = getIntent();
        trail = getTrailFromDB();

        rawWeatherData = new ArrayList<String>();
        getWeatherData();

        trailName = findViewById(R.id.TrailName);
        RainLastDay = findViewById(R.id.RainLastDay);
        removeButton = findViewById(R.id.removeButton);
        backButton = findViewById(R.id.BackButton);




        trailName.setText(trail.getName());




        int t = intentFromMain.getIntExtra("id",-1);




        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteTrail(trail);
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }




    //AsynchTask to get trail from room database
    private Trail getTrailFromDB() {


        class GetTrailFromDB extends AsyncTask<Void, Void, Trail> {

            @Override
            protected Trail doInBackground(Void... voids) {

                int id = intentFromMain.getIntExtra("id",-1);

                //Getting list from room database
                Trail trail = DatabaseClient.getInstance(getApplicationContext()).getAppDatabase().trailDao().loadSingle(id);
                return trail;
            }
            @Override
            protected void onPostExecute(Trail trail) {
                super.onPostExecute(trail);
                double rain = trail.precipLastDay;
                RainLastDay.setText("Rain: "+ rain);

            }


        }

        Trail T;
        //Instantiate the task and execute it
        GetTrailFromDB getTrail = new GetTrailFromDB();
        try {
            T = getTrail.execute().get();
        }
        catch(Exception e){ //If an exception is thrown, function returns null. For now
            Log.d("TAG","Error while getting trail list"+e);
            return null;
        }

        return T;
    }


    //AsyncTast to remove the trail from the database and return to main activity
    private void deleteTrail(final Trail trail) {
        class DeleteTrail extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                DatabaseClient.getInstance(getApplicationContext()).getAppDatabase()
                        .trailDao()
                        .delete(trail);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Toast.makeText(getApplicationContext(), "Deleted", Toast.LENGTH_LONG).show();
                finish();
            }
        }

        DeleteTrail dt = new DeleteTrail();
        dt.execute();
    }


    //API Access and parsing________________________________________________________________________

    private void getWeatherData(){ //get a weeks worth of weather data and parse it
        long start = CurrentUnixTime();

        for (int i = 0;i < 7; i++){
            getJsonFromWeatherAPI(trail.getLatitude(),trail.getLongitude(),start,i);
            start -= secsPrDay;
        }

    }


    public void getJsonFromWeatherAPI(double lat, double lon, long start, final int index){ //Get weather data from api.
        if(queue==null){
            queue = Volley.newRequestQueue(this);
        }

        //API: https://openweathermap.org/history


        //String url = "https://jobs.github.com/positions.json?description=" + search;

        String apiKey = getResources().getString(R.string.apiKey);

        //String url = "http://history.openweathermap.org/data/2.5/history/city?lat=+"+lat+"&lon="+lon+"&type=hour&start="+start+"&cnt="+cnt+"&appid="+apiKey;

        String url = "https://api.darksky.net/forecast/"+apiKey+"/"+lat+","+lon+","+start;

        Log.d("TAG","API URL " + url);


        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("TAG","Response recieved from api");


                        rawWeatherData.add(response);


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("TAG", "Problem loading from API", error);
            }
        });
        queue.add(stringRequest);
    }


    //Get current time in unix time
    public long CurrentUnixTime(){
        Calendar cal = Calendar.getInstance();

        TimeZone timeZone = cal.getTimeZone();

        Date cals = Calendar.getInstance(timeZone.getDefault()).getTime();

        long milliseconds = cals.getTime();

        milliseconds = milliseconds + timeZone.getOffset(milliseconds);

        Log.d("TAG","Unix time: "+milliseconds / 1000L);

        return milliseconds / 1000L;
    }



}
