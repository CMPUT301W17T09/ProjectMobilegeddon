package ca.ualberta.cmput301w17t09.mood9.mood9;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.Image;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

public class MoodViewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_view);

        int position = getIntent().getExtras().getInt("moodIndex");
        Mood9Application mApplication = (Mood9Application) getApplication();
        Mood mood = null;

        if(position == -1){
            String moodId = getIntent().getStringExtra("moodID");
            if (moodId != null) {
                for (Mood eachmood : mApplication.getMoodLinkedList()) {
                    if (eachmood.getId().equals(moodId)) {
                        mood = eachmood;
                    }
                }
            }
        }
        else{
            mood = mApplication.getMoodLinkedList().get(position);
        }

        SocialSituationModel socialSituationModel = mApplication.getSocialSituationModel();
        EmotionModel emotionModel = mApplication.getEmotionModel();
        SocialSituation ss = socialSituationModel.getSocialSituation(mood.getSocialSituationId());
        Emotion e = emotionModel.getEmotion(mood.getEmotionId());

        ImageView emoticon = (ImageView) findViewById(R.id.emotion_image);
        TextView emotion = (TextView) findViewById(R.id.emotion_name);
        TextView date = (TextView) findViewById(R.id.date);
        TextView username = (TextView) findViewById(R.id.username);
        TextView trigger = (TextView) findViewById(R.id.trigger);
        TextView social_situation = (TextView) findViewById(R.id.social_situation);
        ImageView viewMoodImage = (ImageView) findViewById(R.id.viewMoodImage);

        int resID = getResources().getIdentifier(e.getName().toLowerCase().trim() , "drawable", getPackageName());
        viewMoodImage.setImageBitmap(StringToBitMap(mood.getImage()));
        emoticon.setImageResource(resID);
        emotion.setText(e.getName());
        social_situation.setText(ss.getName());
        date.setText(mood.getDate().toString());
        trigger.setText(mood.getTrigger());
        username.setText(UserModel.getUserProfile(mood.getUser_id()).getName());

        Button edit = (Button) findViewById(R.id.edit);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String user_id = sharedPreferences.getString("user_id", "TESTER IN MAIN");
        if (mood.getUser_id().equals(user_id)) {
            edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //for list item clicked
                    Intent editMoodIntent = new Intent(MoodViewActivity.this, AddMoodActivity.class);
                    editMoodIntent.putExtra("editCheck", 1);
                    editMoodIntent.putExtra("moodIndex", position);
                    startActivityForResult(editMoodIntent, 1);
                }
            });
        } else {
            edit.setVisibility(View.INVISIBLE);
        }
    }


    //http://androidtrainningcenter.blogspot.in/2012/03/how-to-convert-string-to-bitmap-and.html
    //Found on March 23, 2017
    public Bitmap StringToBitMap(String encodedString){
        try{
            byte [] encodeByte= Base64.decode(encodedString,Base64.DEFAULT);
            Bitmap bitmap= BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        }catch(Exception e){
            e.getMessage();
            return null;
        }
    }

    // Code Documentation found here: https://developer.android.com/reference/android/app/Activity.html
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        finish();
    }
}
