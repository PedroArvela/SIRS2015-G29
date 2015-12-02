package com.mobile.sirs.g29.lockerroom;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.security.PrivateKey;

public class ChallangeActivity extends AppCompatActivity {

    String _computerName;
    String _computerIP;
    int _computerPort;
    int tries = 0;

    String secret = "UnsecuredTemplate";

    private void exchangeSessionkey(){
        //TODO: send session key to computer in a safe manner
    }

    private boolean attemptChallange(String attempt){
        if(attempt.equals(secret)){
            Log.d("SECRET", "Atempt: " + attempt + " SECRET: " + secret);
            this.exchangeSessionkey();
            return true;
        }
        return false;
    }

    public void attemptButton(){
        String attempt;
        if(tries < 3){
            attempt = ((EditText)findViewById(R.id.challangePassPhrase)).getText().toString();
            if(this.attemptChallange(attempt)){
                //return to main activity with OK signal
                setResult(Activity.RESULT_OK);
                finish();
            } else {
                tries++;
                ProgressBar pb = (ProgressBar)findViewById(R.id.challangeTriesbar);
                pb.setProgress(tries*5);
                if(tries == 3){
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                }
            }
        } else{
            //return to main activity with FAIL signal
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challange);

        _computerName = getIntent().getStringExtra("NAME");
        _computerIP = getIntent().getStringExtra("IP");
        _computerPort = Integer.parseInt(getIntent().getStringExtra("PORT"));

        //grab passphrase from storage
        try{
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(this.getFilesDir().toString() + "/passphrase/passfile.pass"));
            secret = (String) inputStream.readObject();
            inputStream.close();

        } catch (Exception e){
            e.printStackTrace();
        }

        ProgressBar pb = (ProgressBar)findViewById(R.id.challangeTriesbar);
        pb.setMax(15);


        final Button b = (Button)findViewById(R.id.challangeAcceptTryButton);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptButton();
            }
        });

    }
}
