package com.mobile.sirs.g29.lockerroom;

import android.os.AsyncTask;
import android.support.v4.content.res.TypedArrayUtils;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;

public class DecypherTask extends AsyncTask<byte[], Computer.STATUS, byte[]> {
    private Computer _target;
    private ComputerListAdapter _adapter;
    private String _keyFolderPath;
    private Lock _processLock;

    private PrivateKey _pkey = null;

    CypherMessage cm = new CypherMessage();
    SignMessage sm = new SignMessage();

    public DecypherTask(String pathToKeys, Lock lock, Computer target, ComputerListAdapter adapter){
        _target = target;
        _adapter = adapter;
        _keyFolderPath = pathToKeys;
        _processLock = lock;
    }

    @Override
    protected void onPreExecute() {
        try {
            _processLock.lock();
            try {
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(_keyFolderPath + "/private.key"));
                _pkey = (PrivateKey) inputStream.readObject();
                inputStream.close();
            } finally {
                _processLock.unlock();
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected byte[] doInBackground(byte[]... params) {
        byte[] message = params[0];
        byte[] answer = null;
        //decipher message
        try {
            publishProgress(Computer.STATUS.WORKING);
            String deciphered = cm.decript(message, _pkey);
            answer = deciphered.getBytes();
        } catch (Exception e){
            //ignore
            e.printStackTrace();
        }

        //sign and encript for computer reading ability
        try {
            publishProgress(Computer.STATUS.WORKING);
            answer = cm.encript(answer.toString(), _target.get_computerCipher());
            answer = sm.signMessage(answer.toString(), _target.get_computerSign());
        } catch (Exception e){
            //ignore
            e.printStackTrace();
        }

        //TODO: Send contents
        try{
            publishProgress(Computer.STATUS.SENDING);
        }catch (Exception e){
            e.printStackTrace();
        }

        return answer;
    }

    @Override
    protected void onPostExecute (byte[] answer) {
        cm = null;
        sm = null;
        _target.set_status(Computer.STATUS.IDLE);
        _adapter.notifyDataSetChanged();
    }

    @Override
    protected void onProgressUpdate (Computer.STATUS... values){
        _target.set_status(values[0]);
        _adapter.notifyDataSetChanged();
    }
}