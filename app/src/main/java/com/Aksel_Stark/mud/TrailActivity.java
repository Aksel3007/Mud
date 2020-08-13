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

import java.util.List;

public class TrailActivity extends AppCompatActivity {

    TextView trailName;
    Button removeButton;
    Intent intentFromMain;
    Trail trail;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trail);



        intentFromMain = getIntent();
        trail = getTrailFromDB();

        trailName = findViewById(R.id.TrailName);
        int t = intentFromMain.getIntExtra("id",-1);





        removeButton = findViewById(R.id.removeButton);

        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteTrail(trail);
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

}
