package unimelb.comp90018.saferdriver;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import java.util.Locale;


public class StartActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start);
    }

    public void start(View view)
    {
        Intent intent = new Intent(this, GooglyEyesActivity.class);
        startActivity(intent);

    }
}
