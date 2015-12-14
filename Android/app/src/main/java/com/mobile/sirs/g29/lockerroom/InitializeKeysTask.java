package com.mobile.sirs.g29.lockerroom;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.io.File;

public class InitializeKeysTask extends AsyncTask<Void, String, Void> {

    private Context _taskContext;
    private Activity _activity;
    private TextView _status;
    private CypherMessage _cypher;
    private SignMessage _sign;
    private SimmetricCypherMessage _simmetric;

    //folder paths
    private String filesDirPath;
    private String msgDirPath;
    private String signDirPath;
    private String simmDirPath;

    //key paths
    private String signPubPath;
    private String singPrivPath;
    private String msgPubPath;
    private String msgPrivPath;
    private String simmetricPath;

    private boolean simExists = false;
    private boolean msgExists = false;
    private boolean sigExists = false;

    public InitializeKeysTask(Context taskContext, TextView status, Activity activity){
        _taskContext = taskContext;
        _activity = activity;
        _status = status;
        _cypher = new CypherMessage();
        _sign = new SignMessage();
        _simmetric = new SimmetricCypherMessage();

        filesDirPath = _taskContext.getFilesDir().toString();

        signPubPath = filesDirPath + _taskContext.getText(R.string.signFolder) + _taskContext.getText(R.string.publicKey);
        singPrivPath = filesDirPath + _taskContext.getText(R.string.signFolder) + _taskContext.getText(R.string.privateKey);
        signDirPath = filesDirPath + _taskContext.getText(R.string.signFolder);

        msgPubPath = filesDirPath + _taskContext.getText(R.string.messageCipherFolder) + _taskContext.getText(R.string.publicKey);
        msgPrivPath = filesDirPath + _taskContext.getText(R.string.messageCipherFolder) + _taskContext.getText(R.string.privateKey);
        msgDirPath = filesDirPath + _taskContext.getText(R.string.messageCipherFolder);

        simmetricPath = filesDirPath + _taskContext.getText(R.string.secretFolder) + _taskContext.getText(R.string.secretKey);
        simmDirPath = filesDirPath + _taskContext.getText(R.string.secretFolder);
    }

    @Override
    protected void onPreExecute(){
        _status.setText("Scanning for existing keys...");
        simExists = _simmetric.keyExist(simmDirPath);
        msgExists = _cypher.keyPairExist(msgDirPath);
        sigExists = _sign.keyPairExist(signDirPath);

        Log.d("KeyGen", "SIMMETRIC KEY EXISTS: " + simExists);
        Log.d("KeyGen", "MESSAGE CIPHER KEYS EXIST: " + msgExists);
        Log.d("KeyGen", "SIGNATURE KEYS EXIST: " + sigExists);
    }

    @Override
    protected Void doInBackground(Void... params) {
        if(!simExists){
            //Simmetric key is non existant
            Log.d("KeyGen", "GENERATING SIMMETRIC KEY");
            publishProgress("Generating Secret...");
            _simmetric.generateKey(simmDirPath);
            publishProgress("Secret Generated...");
        }

        if(!msgExists){
            //Simmetric key is non existant
            Log.d("KeyGen", "GENERATING MESSAGE CIPHER KEYS");

            publishProgress("Adding Security...");
            try {
                Log.d("msgKey", "Initializing...");
                File messageKeyFolder = new File(msgDirPath);
                messageKeyFolder.mkdir();
                Log.d("msgKey", "msgFolder Exists: " + (messageKeyFolder.isDirectory() && messageKeyFolder.exists()));
                _cypher.generateKey(4096, messageKeyFolder.toString());
                File msgPubKeyFile = new File(messageKeyFolder.toString()+"/public.key");
                File msgPrivKeyFile = new File(messageKeyFolder.toString()+"/private.key");
                Log.d("msgPubKey", "Message Pub File exists: " + (msgPubKeyFile.exists() && msgPubKeyFile.isFile()));
                Log.d("msgPrivKey", "Message Priv File exists: " + (msgPrivKeyFile.exists() && msgPrivKeyFile.isFile()));

            } catch (Exception e){
                e.printStackTrace();
                Log.e("KeyGen", "Could not generate Message Cipher keys, invalid algorithem or non-existant files");
            }
            publishProgress("Created Security...");
        }

        if(!sigExists){
            //Simmetric key is non existant
            Log.d("KeyGen", "GENERATING SIGNATURE KEYS");
            publishProgress("More Security...");
            try {
                Log.d("SignKey", "Initializing...");
                File signKeyFolder = new File(signDirPath);
                signKeyFolder.mkdir();
                Log.d("SignKey", "SignFolder Exists: " + (signKeyFolder.isDirectory() && signKeyFolder.exists()));
                _sign.generateKey(4096, signKeyFolder.toString());
                File signPubKeyFile = new File(signKeyFolder.toString() + "/public.key");
                File signPrivKeyFile = new File(signKeyFolder.toString() + "/private.key");
                Log.d("SignKey", "Sign Pub File exists: " + (signPubKeyFile.exists() && signPubKeyFile.isFile()));
                Log.d("SignKey", "Sign Priv File exists: " + (signPrivKeyFile.exists() && signPrivKeyFile.isFile()));
            } catch (Exception e){
                e.printStackTrace();
                Log.e("KeyGen", "Could not generate Signature keys, invalid algorithem or non-existant files");
            }
            publishProgress("More Security added...");
        }

        return null;
    }

    @Override
    protected void onProgressUpdate (String... values){
        String message = values[0];
        _status.setText(message);
    }

    @Override
    protected void onPostExecute (Void result) {
        _status.setText("Initializing app...");

        File passFile = new File(_taskContext.getFilesDir().toString() + "/passphrase"+ "/passfile.pass");
        while(!passFile.exists());
        _taskContext.startActivity(new Intent(_taskContext, MainActivity.class));
        _activity.finish();
    }
}
