package ca.ualberta.cmput301w17t09.mood9.mood9;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Button goButton = (Button) findViewById(R.id.go);
        EditText usernameField = (EditText) findViewById(R.id.username_field);


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String name = sharedPreferences.getString("username", null);

        //If the name is not null, then the user has already choosen a name, the app will go straight to the FeedActivity
        if(name != null){
            Intent feedIntent = new Intent(MainActivity.this, FeedActivity.class);
            startActivity(feedIntent);
            finish();
        }


        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("username", usernameField.getText().toString());

                String user_id = UserModel.getUser(usernameField.getText().toString());

                if (user_id == null) {
                    Snackbar snackbar = Snackbar
                            .make(v, "Username already exists, please enter a different unique username", Snackbar.LENGTH_LONG);

                    snackbar.show();
                    return;
                }
                editor.putString("user_id", user_id);

                editor.commit();
                String test = sharedPreferences.getString("user_id", "TESTER IN MAIN");

                Intent feedIntent = new Intent(MainActivity.this, FeedActivity.class);
                startActivity(feedIntent);
                finish();
            }
        });

    }
}

