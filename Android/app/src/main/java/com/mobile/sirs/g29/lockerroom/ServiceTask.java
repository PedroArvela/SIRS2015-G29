package com.mobile.sirs.g29.lockerroom;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

public class ServiceTask extends AsyncTask<Computer, Computer.STATUS, Void> {
    private enum REQUEST {CIPHER, DICIPHER, INVALID};

    private static final int TASK_LIFETIME = 60000;
    private static final int MAX_FILZE_SIZE = 4096;

    private Computer _computer;
    private ComputerListAdapter _adapter;
    private Context _taskContext;
    private boolean executing = true;
    private DatagramSocket _recvRequest;

    private long deathTime;

    public ServiceTask(Computer computer, ComputerListAdapter adapter, Context taskContext){
        _computer = computer;
        _adapter = adapter;
        _taskContext = taskContext;
    }

    @Override
    protected void onPreExecute() {
        this.initialize();
        deathTime = System.currentTimeMillis() + TASK_LIFETIME;
    }

    @Override
    protected Void doInBackground(Computer... input) {
        Computer target = input[0];
        REQUEST status;
        byte[] message = null;
        boolean skip = false;

        while(executing) {
            this.checkLifetime();
            if (!target.get_status().equals(Computer.STATUS.NONE)) {
                log("Listening for requests");
                publishProgress(Computer.STATUS.IDLE);
                status = this.recvRequest(1500);

                if(status.equals(REQUEST.CIPHER) || status.equals(REQUEST.DICIPHER)){
                    publishProgress(Computer.STATUS.RECIVING);
                    if(status.equals(REQUEST.CIPHER)) {
                        log("Ciphering file");
                    } else {
                        log("Deciphering file");
                    }
                    try {
                        message = this.recvFile(2000, status);
                        log("Recived: \"" + new String(message, 0, message.length) + "\"");
                        publishProgress(Computer.STATUS.WORKING);
                        message = this.work(message, status);
                    } catch (SocketTimeoutException e){
                        if(status.equals(REQUEST.CIPHER)) {
                            this.logError("Reciving file for ciphering timed out");
                        } else {
                            this.logError("Reciving file for deciphering timed out");
                        }
                        skip = true;
                    }

                    if(!skip) {
                        publishProgress(Computer.STATUS.SENDING);
                        if(status.equals(REQUEST.CIPHER)) {
                            log("Sending ciphered file");
                        } else {
                            log("Sending deciphered file");
                        }
                        try {
                            this.sendFile(2000, message, status);
                        } catch (SocketTimeoutException e){
                            if(status.equals(REQUEST.CIPHER)) {
                                this.logError("Sending ciphered file timed out");
                            } else {
                                this.logError("Sending deciphered file timed out");
                            }
                        }
                    }
                } else if(status.equals(REQUEST.INVALID)){
                    publishProgress(Computer.STATUS.IDLE);
                    this.logError("Unexpected request or timeout");
                }
                status = REQUEST.INVALID;
                message = null;
                skip = false;
            }

        }
        this.cancelService();
        log("Service was cancelled");
        return null;
    }

    @Override
    protected void onPostExecute (Void answer) {
        this.cancelService();
        _computer.set_status(Computer.STATUS.NONE);
        _adapter.notifyDataSetChanged();
        log("Service was Stoped");
    }

    @Override
    protected void onProgressUpdate (Computer.STATUS... values){
        Computer.STATUS updateVal = values[0];
        _computer.set_status(updateVal);
        _adapter.notifyDataSetChanged();
    }

    @Override
    protected void onCancelled(){
        executing = false;
    }

    private void log(String message) {
        Log.d("SERVICE-" + _computer.get_comptuerName(), message);
    }
    private void logError(String message) {
        Log.e("SERVICE-" + _computer.get_comptuerName(), message);
    }

    private void initialize(){
        try {
            _recvRequest = new DatagramSocket(_computer.get_servicePort());
        } catch (SocketException e){
            //ignore
            e.printStackTrace();
        }
    }

    private byte[] work(byte[] message, REQUEST type){
        byte[] answer = null;
        SimmetricCypherMessage scm = new SimmetricCypherMessage();

        this.log("Working on: \"" + new String(message, 0, message.length) + "\"");

        String filesPath = _taskContext.getFilesDir().toString();
        String fullPath = filesPath + _taskContext.getText(R.string.secretFolder) + _taskContext.getText(R.string.secretKey);
        this.log(fullPath);

        SecretKey key = scm.getSecretKey(fullPath);

        if(type.equals(REQUEST.CIPHER)) {
            answer = scm.encript(message, key);
        }
        else if(REQUEST.DICIPHER.equals(type)){
            answer = scm.decript(message, key);
        }

        this.log("Produced: \"" + new String(answer, 0, answer.length) + "\"");
        this.log("\tOf size: " + answer.length);

        return answer;
    }

    private void sendFile( int timeout, byte[] content, REQUEST type) throws SocketTimeoutException{
        String targetIP = _computer.get_computerIP();
        InetAddress address;
        byte[] outbound_message;

        CypherMessage cm = new CypherMessage();
        PublicKey cKey = _computer.get_computerCipher();

        try {
            //this.logError(_computer.get_computerCipher().toString());

            this.log("Sending: \"" + new String(content, 0, content.length) + "\"");

            address = InetAddress.getByName(targetIP);
            if(type.equals(REQUEST.DICIPHER)) {
                outbound_message = cm.encript(content, cKey);
            }
            else {
                outbound_message = content;
            }

            _recvRequest.connect(address, _computer.get_servicePort());
            DatagramPacket outbound = new DatagramPacket(outbound_message, outbound_message.length, address, _computer.get_servicePort());
            _recvRequest.send(outbound);
            this.log("Sent message: " + new String(content, 0, content.length));
        } catch (Exception e){
            e.printStackTrace();
            throw new SocketTimeoutException();
        }
        this.log("Message was sent...");
    }

    private byte[] recvFile(int timeout, REQUEST type) throws SocketTimeoutException{
        byte[] answer = null;
        byte[] buffer = new byte[MAX_FILZE_SIZE];

        CypherMessage cm = new CypherMessage();
        PrivateKey pKey = cm.getPrivate(new File(_taskContext.getFilesDir().toString()+_taskContext.getText(R.string.messageCipherFolder)+_taskContext.getText(R.string.privateKey)));

        DatagramPacket inbound = new DatagramPacket(buffer, buffer.length);
        try {
            _recvRequest.setSoTimeout(timeout);
            _recvRequest.receive(inbound);

            this.log("Recived: \"" + new String(buffer, 0, inbound.getLength()) + "\"");
            this.log("Size of recived: " + inbound.getLength());
            answer = inbound.getData();
            answer = shortArray(answer, inbound.getLength());
            if(type.equals(REQUEST.CIPHER))
                answer = cm.decript(answer, pKey);
            this.log("Decripted Recived: \"" + new String(answer, 0, answer.length) + "\"");
        } catch (Exception e){
            e.printStackTrace();
            throw new SocketTimeoutException();
        }



        this.log("Size of recv:" + answer.length + " | " + inbound.getData().length + " | " + new String(answer, 0, answer.length));
        return answer;
    }

    private byte[] shortArray(byte[] array, int to){
        byte[] answer = new byte[to];
        for(int i = 0; i < to; i++){
            answer[i] = array[i];
        }
        return answer;
    }

    private REQUEST recvRequest(int timeout){
        byte[] buffer = new byte[MAX_FILZE_SIZE];
        DatagramPacket inbound = new DatagramPacket(buffer, buffer.length);

        try {
            _recvRequest.setSoTimeout(timeout);
            _recvRequest.receive(inbound);
            String recv = new String(buffer, 0, inbound.getLength());
            this.log("Request recived: " + recv);

            if(recv.equals("c")){
                this.log("Recived cipher Request");
                return REQUEST.CIPHER;
            } else if(recv.equals("d")){
                this.log("Recived decipher Request");
                return REQUEST.DICIPHER;
            } else {
                this.log("Recived invalid Request");
                return REQUEST.INVALID;
            }

        } catch(SocketTimeoutException d) {
            return REQUEST.INVALID;
        } catch (Exception e){
            e.printStackTrace();
        }
        return REQUEST.INVALID;
    }

    private void cancelService(){
        _recvRequest.close();

    }

    private final void checkLifetime(){
        long currentTime = System.currentTimeMillis();
        if(currentTime > this.deathTime){
            executing = false;
        }
    }
}