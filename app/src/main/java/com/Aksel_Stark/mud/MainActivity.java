package com.Aksel_Stark.mud;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.facebook.stetho.Stetho;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;



public class MainActivity extends AppCompatActivity implements TrailRecyclerAdapter.ItemClickListener {
    final int milisecsPrDay = 86400000;

    TrailRecyclerAdapter adapter;

    Intent addLocation;
    Intent seeTrailInfo;
    EditText trailName;

    RequestQueue queue; //For Volley
    boolean WeatherRecievedFlag = false;
    ArrayList<Trail> TrailList = new ArrayList<>();


    Trail returnTrail = new Trail();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Stetho.initializeWithDefaults(this);

        setContentView(R.layout.activity_main);
        TrailList.clear();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        trailName = findViewById(R.id.trailName);
        addLocation = new Intent(this,MapsActivity.class);
        seeTrailInfo = new Intent(this,TrailActivity.class);

        Log.d("TAG","onCreate for main activity"); //Debugging

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String trailNameString = trailName.getText().toString();
                if (trailNameString.isEmpty()){ //If no name is chosen, a toast reminds the user
                    Context context = getApplicationContext();
                    CharSequence text = "You have to name your trail";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }
                else{ //Opens map if a name has been chosen
                    addLocation.putExtra("name",trailNameString);
                    startActivityForResult(addLocation,100);
                }

            }
        });





        ArrayList TL = new ArrayList<Trail>(getTrailsFromDB());
        TrailList = TL;


        //Adding testtrail to populate list (for debugging)
        //Trail TestTrail = new Trail("DKs tag",12.424,56.4634);
        //TrailList.add(TestTrail);

        updateWeatherData();

        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.TrailRecyclerViewID);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        adapter = new TrailRecyclerAdapter(this, TrailList);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

    }

    @Override
    public void onItemClick(View view, int position) {
        //Toast.makeText(this, "You clicked " + adapter.getItem(position) + " on row number " + position, Toast.LENGTH_SHORT).show();

        seeTrailInfo.putExtra("id",TrailList.get(position).getId());
        startActivityForResult(seeTrailInfo,200);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //Resultat
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 100){
            if (resultCode == RESULT_OK){
                //Retrieve location from intent
                double longitude = data.getDoubleExtra("longitude",0);
                double latitude = data.getDoubleExtra("latitude",0);
                String TrailName = data.getStringExtra("name");

                Trail newTrail = new Trail(TrailName,longitude,latitude);
                Log.d("TAG","New trail: " + TrailName + " long: "+ longitude + " lat: "+ latitude);
                getJsonFromWeatherAPI(latitude,longitude,1584746726, newTrail);

                //Add the new trail to the list, so it can be shown in the recyclerView
                int newTrailIndex = adapter.getItemCount();
                TrailList.add(newTrailIndex,newTrail);
                saveNewTrailToDB(newTrail);
                adapter.notifyItemInserted(newTrailIndex);

                updateWeatherData(); //ToDo: Change to update only new trail data (for efficiency)




                long currentTime = CurrentUnixTime();
                Log.d("TAG","Unix time:"+currentTime);

                //Toast prints longitude (for debugging/testing)
                Context context = getApplicationContext();
                CharSequence text = "Longitude: "+ longitude;
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();


            }

        }
        else if (requestCode == 200){
            Log.d("TAG","returned to main with requestcode 200");

            TrailList = new ArrayList<Trail>(getTrailsFromDB());
            //updateWeatherData();
            adapter.notifyDataSetChanged();

        }


        super.onActivityResult(requestCode, resultCode, data);
    }


    //API access____________________________________________________________________________________

    public void getJsonFromWeatherAPI(double lat, double lon, int start, final Trail trailObj){ //
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


                        trailObj.setRawWeatherData(response);

                        saveNewTrailToDB(trailObj);



                        returnTrail = trailObj;

                        Log.d("TAG","Response from api: " + returnTrail.getRawWeatherData());

                        //Log.d("TAG","Response"+response); //To test if recieved json (debugging)
                        adapter.notifyDataSetChanged();
                        //Log.d("TAG","Raw weather from trailObj: "+trailObj.getRawWeatherData());

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("TAG", "Problem loading from API", error);
            }
        });
        queue.add(stringRequest);

        /* Sleep until data is recieved (data updated in db instead)
        int sleepCounter = 0;
        do{
            sleepCounter += 1;
            try{
            Thread.sleep(1000);
            Log.d("TAG","Waiting for api response" + returnTrail.getRawWeatherData());
            }catch(Exception e){
                Log.d("TAG","Sleep: "+e);
            }
        }while((returnTrail.getRawWeatherData().equals("0")) && (sleepCounter < 10));*/


        //if the data has not been updated, just return trail as it was (trail updated in db instead)
        /*if(returnTrail == null){

            Log.d("TAG","Weather data not updated");

            return trailObj;
        }
        else {
            Log.d("TAG","Weather data updated. Data: "+ returnTrail.getRawWeatherData());
            return returnTrail;
        }*/
    }


    //Update weather data for all trails in TrailList
    public void updateWeatherData(){
        Log.d("TAG","updateWeatherData() length of list: " + TrailList.size());
        for (int i = 0;i<TrailList.size();i++){
            Trail T = TrailList.get(i);

            getJsonFromWeatherAPI(T.latitude,T.longitude,(int)CurrentUnixTime()-milisecsPrDay,T); //Gets the weather data from api. Data is added to room db, so no return.

            //Log.d("TAG","Raw weather from trailObj: "+T.getRawWeatherData());

            TrailList.set(i,T);
        }
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


    //____________________________________________________________________________________Room tasks

    //AsynchTask to add a trail to room database
    private void saveNewTrailToDB(Trail trail) {

        class SaveNewTrailToDB extends AsyncTask<Trail, Void, Void> { //Takes a trail as argument

            @Override
            protected Void doInBackground(Trail... trails) {

                //adding to room database
                DatabaseClient.getInstance(getApplicationContext()).getAppDatabase()
                        .trailDao()
                        .insert(trails[0]);
                return null; //?
            }

            @Override
            protected void onPostExecute(Void aVoid) { //Makes a toast when trail has been added
                super.onPostExecute(aVoid);

                Toast.makeText(getApplicationContext(), "Saved to room DB", Toast.LENGTH_LONG).show();
            }
        }

        //Instantiate the task and execute it
        SaveNewTrailToDB snt = new SaveNewTrailToDB();
        snt.execute(trail);
    }


    //AsynchTask to get the list of trails from room database
    private List<Trail> getTrailsFromDB() {


        class GetTrailsFromDB extends AsyncTask<Void, Void, List<Trail>> { //Takes a trail as argument

            @Override
            protected List<Trail> doInBackground(Void... voids) {

                //Getting list from room database
                List<Trail> trailList = DatabaseClient.getInstance(getApplicationContext())
                        .getAppDatabase()
                        .trailDao()
                        .getAll();
                return trailList;
            }

            @Override
            protected void onPostExecute(List<Trail> trails) { //Makes a toast when trail has been added
                super.onPostExecute(trails);
                Toast.makeText(getApplicationContext(), "Retrieved list from DB", Toast.LENGTH_LONG).show();
            }
        }

        List<Trail> TL;

        //Instantiate the task and execute it
        GetTrailsFromDB getTrails = new GetTrailsFromDB();
        try {
            TL = getTrails.execute().get();
        }
        catch(Exception e){ //If an exception is thrown, function returns null. For now
            Log.d("TAG","Error while getting trail list"+e);
            return null;
        }
        return TL;
    }

}
