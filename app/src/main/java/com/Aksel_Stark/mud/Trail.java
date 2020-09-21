package com.Aksel_Stark.mud;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

//Room
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;




import java.io.Serializable;
import java.util.jar.JarEntry;


@Entity
public class Trail implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "name")
    String name;

    @ColumnInfo(name = "longitude")
    double longitude;

    @ColumnInfo(name = "latitude")
    double latitude;

    @ColumnInfo(name = "rawWeatherData")
    String rawWeatherData;

    @ColumnInfo(name = "precipLastDay")
    double precipLastDay;


    Trail(String name_,double long_,double lat_){
        setName(name_);
        setLatitude(lat_);
        setLongitude(long_);
    }

    Trail(){
        setName("Unnamed Trail");
        setLatitude(0.0);
        setLongitude(0.0);
    }


    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setRawWeatherData(String rawWeatherData) {
        this.rawWeatherData = rawWeatherData;
        //Log.d("TAG","Raw data written to trail"+rawWeatherData); //To test if recieved json

        getFullPrecip(); //Parses weather data and gets precipitation data
    }

    public String getRawWeatherData(){return rawWeatherData;}


    //Get total rainfall. Sets precipLastDay. Negative value if error.
    public void getFullPrecip(){
        double fullPrecip = 0;
        JSONObject reader;
        try {
            reader = new JSONObject(rawWeatherData);

            JSONObject daily = reader.getJSONObject("daily");

            JSONArray data = daily.getJSONArray("data");

            JSONObject zero = data.getJSONObject(0);

            fullPrecip = zero.getDouble("precipIntensity");

            Log.d("TAG","Precipitation read from json: " + fullPrecip + this.getName());
        }
        catch(Exception e){
            Log.d("TAG","Error reading from json weather data:"+e);
            precipLastDay = -1;
        }
        precipLastDay = fullPrecip*24;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
