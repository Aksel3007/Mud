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
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import org.json.JSONArray;
import org.json.JSONObject;

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
    GraphView dailyGraph;
    GraphView hourlyGraph;

    RequestQueue queue; //For Volley

    ArrayList<String> rawWeatherData;
    double[] dailyPrecip = new double[7];
    double[] hourlyPrecip = new double[168];
    BarGraphSeries<DataPoint> dailyPrecipSeries;
    DataPoint[] dailyPrecipDP = new DataPoint[7];
    DataPoint[] hourlyPrecipDP = new DataPoint[168];

    final int secsPrDay = 86400;
    int responseCount = 0;


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

        //Setup graphs
        dailyGraph = (GraphView) findViewById(R.id.dailyGraph);
        dailyGraph.getViewport().setXAxisBoundsManual(true);
        dailyGraph.getViewport().setMaxX(6.5);
        dailyGraph.getViewport().setMinX(-0.5);
        hourlyGraph = (GraphView) findViewById(R.id.hourlyGraph);
        hourlyGraph.getViewport().setXAxisBoundsManual(true);
        hourlyGraph.getViewport().setMinX(-0.5);
        hourlyGraph.getViewport().setMaxX(47.5);


        trailName.setText(trail.getName());


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

        for (int i = 0;i < 7; i++){ //Gets weather data for current day, then previous day and so on
            getJsonFromWeatherAPI(trail.getLatitude(),trail.getLongitude(),start,i);

            start -= secsPrDay;
        }

    }


    public void getJsonFromWeatherAPI(double lat, double lon, long start, final int index){ //Get weather data from api.
        if(queue==null){
            queue = Volley.newRequestQueue(this);
        }

        String apiKey = getResources().getString(R.string.apiKey);

        String url = "https://api.darksky.net/forecast/"+apiKey+"/"+lat+","+lon+","+start;

        Log.d("TAG","API URL " + url);



        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) { //Runs when a response has been recieved from api
                        Log.d("TAG","Response recieved from api");

                        responseCount++;

                        rawWeatherData.add(response);
                        dailyPrecip[index] = getPrecipDaily(response);
                        dailyPrecipDP[index] = new DataPoint(index,getPrecipDaily(response));


                        for(int i = 0;i < 24;i++){
                            hourlyPrecip[index*24+i] = getPrecipHourly(response,i);
                            hourlyPrecipDP[index*24+i] = new DataPoint (index*24+i,getPrecipHourly(response,i));
                        }


                        if(responseCount == 7){
                            dailyGraph.addSeries(new BarGraphSeries<>(dailyPrecipDP));
                            hourlyGraph.addSeries(new BarGraphSeries<>(hourlyPrecipDP));
                        }

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



    public double getPrecipDaily(String weatherData){//
        double fullPrecip = 0;
        JSONObject reader;
        try {
            reader = new JSONObject(weatherData);

            JSONObject daily = reader.getJSONObject("daily");

            JSONArray data = daily.getJSONArray("data");

            JSONObject zero = data.getJSONObject(0);

            fullPrecip = zero.getDouble("precipIntensity");
        }
        catch(Exception e){
            Log.d("TAG","Error reading from json weather data:"+e);
            return -1;
        }
        return fullPrecip*24;
    }

    public double getPrecipHourly(String weatherData, int hourIndex){//
        double Precip = 0;
        JSONObject reader;
        try {
            reader = new JSONObject(weatherData);

            JSONObject hourly = reader.getJSONObject("hourly");

            JSONArray data = hourly.getJSONArray("data");

            JSONObject hour = data.getJSONObject(hourIndex);

            Precip = hour.getDouble("precipIntensity");
        }
        catch(Exception e){
            Log.d("TAG","Error reading from json weather data:"+e);
            return -1;
        }
        return Precip;
    }

}
