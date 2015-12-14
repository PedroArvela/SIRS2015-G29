package com.mobile.sirs.g29.lockerroom;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.InvalidClassException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.TreeMap;

public class ScanNetworkTask extends AsyncTask<Integer, String, ArrayList<Computer>> {

    //network private camps
    private static final String BROADCAST_IP = "192.168.1.255";
    private static final int BROADCAST_PORT = 8888;
    private static final int BROADCAST_LISTEN_PORT = 8889;

    RemoteComunication rc;
    private TextView _status;
    private Button _scan;
    private ComputerListAdapter _computerAdapter;
    private TreeMap<String, Computer> _computers;
    private Context _taskContext;

    public ScanNetworkTask(final TextView status, final Button scan, ComputerListAdapter adapter, TreeMap<String, Computer> treemap, String msgKeyPath, Context taskContext){
        _status = status;
        _scan = scan;
        _computerAdapter = adapter;
        _computers = treemap;
        rc = new RemoteComunication(taskContext.getFilesDir().toString()+"/messageCipher");
        _taskContext = taskContext;
    }

    @Override
    protected void onPreExecute(){
        _status.setText(R.string.defaultProgressText);
        _scan.setVisibility(View.GONE);
        _status.setVisibility(View.VISIBLE);

        try{
            rc.initialize_Broadcast();
            Log.d("NETWORK-BROADCAST", "Initialized sockets...");
        } catch (Exception e){
            //do nothing
        }
    }

    @Override
    protected ArrayList<Computer> doInBackground(Integer... params) {
        int parameter = params[0];
        ArrayList<Computer> incomingComputers = null;
        String inString;

        //SEND BROADCAST
        Log.d("NETWORK-BROADCAST", "Scanning Network...");

        try {
            Log.d("On-Scan", "Sending Broadcast...");
            try {
                publishProgress("Broadcasting Service...");
                rc.scanNetwork_Broadcast();
            } catch(Exception e ){
                publishProgress("Error Ocurred...");
                Log.e("EXCEPTION", "IOException");
                e.printStackTrace();
            }

            ArrayList<Computer> tempCompList = new ArrayList<Computer>();
            ArrayList<Computer> approvedCompList = new ArrayList<Computer>();
            String privKeyPath = _taskContext.getFilesDir().toString() +_taskContext.getText(R.string.messageCipherFolder) + _taskContext.getText(R.string.privateKey);

            long currentTime = System.currentTimeMillis();
            long timeLimit = currentTime + (2*parameter);
            String MESSAGE = "Awaiting responses...";
            int points = 1;
            String postMessage = ".";
            String post = "";
            Log.d("On-Scan-Key", "PrivKeyPath: " + privKeyPath);
            while(currentTime <= timeLimit){
                try {
                    //real reception code here
                    Computer potential = rc.retrive_One_CompKey(new File(privKeyPath));
                    if(_computerAdapter.exists(potential)){
                        Log.d("On-Scan-Key", "Computer: " + potential.get_comptuerName() + " Already is listed");
                    } else{
                        tempCompList.add(potential);
                    }

                } catch (Exception e){
                    Log.e("On-Scan-Rcv-Key", "Connection try timed out...");
                } finally{
                    currentTime = System.currentTimeMillis();
                    Log.d("On-Scan-Rcv-Key", String.valueOf(timeLimit) + "|" + String.valueOf(currentTime));

                    post = MESSAGE;
                    for(int i = 1; i <= points; i++){
                        post += postMessage;
                    }
                    if(points == 3){
                        points = 1;
                    } else {
                        points++;
                    }
                    this.publishProgress(post);
                }
            }

            //Print to log all computers that where found
            for(Computer c : tempCompList){
                Log.d("On-Scan-Comp-Temp", "CN: " + c.get_comptuerName() + " IP: " + c.get_computerIP() + " Key: " + c.get_computerCipher());
            }

            //send service port, answer challange, and inquire computer with challange
            publishProgress("Relaying Service connections...");
            for(Computer c: tempCompList){
                int port = ((InformedApplication)_taskContext.getApplicationContext()).getRandomAvailableUDPPort(8890, 9000);
                c.set_servicePort(port);
                rc.send_ServicePort(c);
            }

            //Await connection ack and challange response
            currentTime = System.currentTimeMillis();
            timeLimit = currentTime + (2*parameter);
            MESSAGE = "Awaiting responses...";
            points = 1;
            postMessage = ".";

            PrivateKey mKey = (new CypherMessage()).getPrivate(new File(privKeyPath));
            while(currentTime <= timeLimit){
                try {
                    //real reception code here
                    Computer valid = rc.listen_Once_Acknowledge(tempCompList, mKey);
                    if(!valid.equals(null)){
                        Log.d("On-Ack-try", "Computer: " + valid.get_comptuerName() + " added to trusted list");
                        approvedCompList.add(valid);
                        tempCompList.remove(valid);
                    }

                } catch (Exception e){
                    Log.e("On-Ack-try", "Connection try timed out...");
                } finally{
                    currentTime = System.currentTimeMillis();
                    Log.d("On-Ack-try", String.valueOf(timeLimit) + "|" + String.valueOf(currentTime));

                    post = MESSAGE;
                    for(int i = 1; i <= points; i++){
                        post += postMessage;
                    }
                    if(points == 3){
                        points = 1;
                    } else {
                        points++;
                    }
                    this.publishProgress(post);
                }
            }

            //clean computers that failed trust requirements
            for(Computer c : tempCompList){
                ((InformedApplication)_taskContext.getApplicationContext()).freePortAssign(c.get_port());
            }

            incomingComputers = approvedCompList;

        } catch (Exception e){
            //do nothing
        }
        return incomingComputers;
    }

    @Override
    protected void onProgressUpdate (String... values){
        _status.setText(values[0]);
    }

    @Override
    protected void onPostExecute (ArrayList<Computer> result) {
        Log.d("NETWORK-BROADCAST", "closing ports...");
        rc.close_Broadcast();

        _scan.setVisibility(View.VISIBLE);
        _status.setVisibility(View.GONE);

        //Modify required data structures
        //restructure recv info
        for(Computer c : result){
            Log.d("Computing list", c.get_comptuerName());
            _computers.put(c.get_comptuerName(), c);
        }

        //DEBUG: Add offline dummy computers
        _computers.put("dummy", new Computer("dummy", "127.0.0.1", 1337, "dummyofflinecomputer", null, null));

        //Populate GUI
        for(Computer comp : _computers.values()){
            if(!_computerAdapter.exists(comp)) {
                _computerAdapter.add(comp);
            }
        }
        _computerAdapter.notifyDataSetChanged();
    }
}
