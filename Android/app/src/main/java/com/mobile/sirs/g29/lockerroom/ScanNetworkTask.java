package com.mobile.sirs.g29.lockerroom;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.TreeMap;

public class ScanNetworkTask extends AsyncTask<Integer, String, ArrayList<String>> {

    //network private camps
    private static final String BROADCAST_IP = "192.168.1.255";
    private static final int BROADCAST_PORT = 8888;
    private static final int BROADCAST_LISTEN_PORT = 8889;
    private static final int REQUEST_PORT = 9999;
    private DatagramSocket broadcast;
    private DatagramSocket recv_Broadcast;
    private InetAddress broadcastAddress;

    RemoteComunication rc = new RemoteComunication();
    private TextView _status;
    private Button _scan;
    private ComputerListAdapter _computerAdapter;
    private TreeMap<String, Computer> _computers;

    public ScanNetworkTask(final TextView status, final Button scan, ComputerListAdapter adapter, TreeMap<String, Computer> treemap){
        _status = status;
        _scan = scan;
        _computerAdapter = adapter;
        _computers = treemap;
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
    protected ArrayList<String> doInBackground(Integer... params) {
        int parameter = params[0];
        ArrayList<String> answer = new ArrayList<String>();

        Log.d("NETWORK-BROADCAST", "Scanning Network...");

        try {
            Log.d("On-Scan", "Sending Broadcast...");
            try {
                publishProgress("Broadcasting Service...");
                rc.scanNetwork_Broadcast();
            } catch(Exception e ){
                publishProgress("Error Ocurred...");
                Log.e("EXCEPTION", "IOException");
            }
            Log.d("On-Scan", "Listening to answers...");
            publishProgress("Listening...");

            long currentTime = System.currentTimeMillis();
            long timeLimit = currentTime + (2*parameter);
            int points = 1;
            final String MESSAGE = "Listening";
            String postMessage = ".";
            String post;

            while(currentTime <= timeLimit){
                try {
                    answer.add(rc.listen_Once_Broadcast());
                } catch (Exception e ){
                    //nothing to do
                } finally{
                    currentTime = System.currentTimeMillis();
                    Log.d("On-Scan", String.valueOf(timeLimit) + "|" + String.valueOf(currentTime));

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

            //answer = rc.listen_Broadcast(parameter);
            publishProgress("List compiled...");

        } catch (Exception e){
            //do nothing
        }
        return answer;
    }

    @Override
    protected void onProgressUpdate (String... values){
        _status.setText(values[0]);
    }

    @Override
    protected void onPostExecute (ArrayList<String> result) {
        Log.d("NETWORK-BROADCAST", "closing ports...");
        rc.close_Broadcast();

        _scan.setVisibility(View.VISIBLE);
        _status.setVisibility(View.GONE);

        //Modify required data structures
        //restructure recv info
        Computer temp;
        String[] parts;
        for(String s : result){
            Log.d("Computing list", s);
            parts = s.split("\\|");
            Log.d("Part[0]", parts[0]);
            Log.d("Part[1]", parts[1]);
            Log.d("Part[2]", parts[2]);
            temp = new Computer(parts[0], parts[1], Integer.parseInt(parts[2]), "fsad992183dasj");
            //TODO: if fingerprint exits on another computer, replace instead of adding
            _computers.put(temp.get_comptuerName(), temp);
        }

        //DEBUG: Add offline dummy computers
        _computers.put("dummy", new Computer("dummy", "127.0.0.1", 1337, "dummyofflinecomputer"));

        //Populate GUI
        for(Computer comp : _computers.values()){
            if(!_computerAdapter.exists(comp)) {
                /*
                displayText = comp.get_comptuerName() + "\n" + comp.get_computerIP().toString();
                _computerNameList.add(displayText);
                */
                _computerAdapter.add(comp);
            }
        }
        _computerAdapter.notifyDataSetChanged();
    }
}
