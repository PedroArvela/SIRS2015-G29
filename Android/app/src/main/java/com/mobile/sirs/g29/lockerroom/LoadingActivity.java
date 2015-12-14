package com.mobile.sirs.g29.lockerroom;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class LoadingActivity extends AppCompatActivity {
    static final String USER_FILE = "userfile";

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    private View mContentView;
    private View mControlsView;
    private boolean mVisible;

    private void createPassphraseMenu(String menuTitle){
        final Intent mainActivity = new Intent(this, MainActivity.class);
        final String passphraseFolderPath = this.getFilesDir().toString()+"/passphrase";

        File passphraseFolder = new File(passphraseFolderPath);
        File passphrase = new File(passphraseFolderPath+"/passfile.pass");
        boolean firstBoot = !passphrase.isFile();

        Log.d("BOOT", Boolean.toString(firstBoot));

        passphraseFolder.mkdir();
        Log.d("BOOT", "FOLDER CREATED: " + Boolean.toString(passphraseFolder.exists()));

        //popout message that will insert passphrase
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(menuTitle);
        builder.setCancelable(false);

        /* Set up the input */
        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String passphrase = "";
                passphrase = input.getText().toString();

                try {
                    File passFile = new File(passphraseFolderPath, "passfile.pass");
                    passFile.createNewFile();
                    Log.d("BOOT", "PASSFILE CREATED: " + passFile.exists());
                    Log.d("BOOT", "PASSFILE CONTENT: " + passphrase);

                    Log.d("BOOT", "PATH: "+ passphraseFolderPath);

                    ObjectOutputStream publicKeyOS = new ObjectOutputStream(new FileOutputStream(passFile));
                    publicKeyOS.writeObject(passphrase);
                    publicKeyOS.close();
                    publicKeyOS.flush();

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putBoolean(getString(R.string.pref_previously_started), Boolean.TRUE);
                    edit.commit();

                } catch (IOException e){
                    Log.e("BOOT", "EXCEPTION");
                    e.printStackTrace();
                }
            }
        });

        builder.show();
    }

    private void initializeKeys(){
        SignMessage sm = new SignMessage();
        SimmetricCypherMessage scm = new SimmetricCypherMessage();
        CypherMessage cm = new CypherMessage();

        File signKeyFolder = new File(this.getFilesDir().toString()+"/sign");
        File messageKeyFolder = new File(this.getFilesDir().toString()+"/messageCipher");
        File cipherKeyFolder = new File(this.getFilesDir().toString()+"/secret");

        try{
            //update globals
            ((InformedApplication)getApplicationContext()).set_msgKeyFolder(messageKeyFolder.toString());
            Log.d("GLOBAL", ((InformedApplication) getApplicationContext()).get_msgKeyFolder());

            scm.generateKey(cipherKeyFolder.toString());

            Log.d("SignKey", "Initializing...");
            signKeyFolder.mkdir();
            Log.d("SignKey", "SignFolder Exists: " + (signKeyFolder.isDirectory() && signKeyFolder.exists()));
            sm.generateKey(4096, signKeyFolder.toString());
            File signPubKeyFile = new File(signKeyFolder.toString()+"/public.key");
            File signPrivKeyFile = new File(signKeyFolder.toString()+"/private.key");
            Log.d("SignKey", "Sign Pub File exists: " + (signPubKeyFile.exists() && signPubKeyFile.isFile()));
            Log.d("SignKey", "Sign Priv File exists: " + (signPrivKeyFile.exists() && signPrivKeyFile.isFile()));

            Log.d("msgKey", "Initializing...");
            messageKeyFolder.mkdir();
            Log.d("msgKey", "msgFolder Exists: " + (messageKeyFolder.isDirectory() && messageKeyFolder.exists()));
            cm.generateKey(4096, messageKeyFolder.toString());
            File msgPubKeyFile = new File(messageKeyFolder.toString()+"/public.key");
            File msgPrivKeyFile = new File(messageKeyFolder.toString()+"/private.key");
            Log.d("msgPubKey", "Message Pub File exists: " + (msgPubKeyFile.exists() && msgPubKeyFile.isFile()));
            Log.d("msgPrivKey", "Message Priv File exists: " + (msgPrivKeyFile.exists() && msgPrivKeyFile.isFile()));
        } catch(Exception e){
            e.printStackTrace();
            Log.e("Keys", "Exception");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_loading_screen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.

        //actual loading activities
        InitializeKeysTask ikt = new InitializeKeysTask(this, (TextView)findViewById(R.id.fullscreen_content), LoadingActivity.this);
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            boolean previouslyStarted = prefs.getBoolean(getString(R.string.pref_previously_started), false);
            if(!previouslyStarted) {
                //CODE TO BE EXECUTED ON APPLICATION FIRST RUN

                //this.initializeKeys();
                this.createPassphraseMenu("New passphrase");
            }
            else{
                //CODE TO BE EXECUTED ON EVERY APPLICATION RUN

            }

        } catch (Exception e){

        }
        ikt.execute();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };

    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
