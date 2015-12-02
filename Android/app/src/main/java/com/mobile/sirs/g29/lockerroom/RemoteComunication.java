package com.mobile.sirs.g29.lockerroom;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

public class RemoteComunication {
    //network private camps
    private static final String BROADCAST_IP = "192.168.1.255";
    private static final int BROADCAST_PORT = 8888;
    private static final int BROADCAST_LISTEN_PORT = 8889;
    private static final int REQUEST_PORT = 9999;
    private DatagramSocket broadcast;
    private DatagramSocket recv_Broadcast;
    private InetAddress broadcastAddress;

    //datastructures

    public void initialize_Broadcast() throws SocketException, UnknownHostException{
        Log.d("On-Scan", "Initialization start...");
        broadcast = new DatagramSocket(BROADCAST_PORT);
        recv_Broadcast = new DatagramSocket(BROADCAST_LISTEN_PORT);

        broadcastAddress = InetAddress.getByName(BROADCAST_IP);
        broadcast.setBroadcast(true);
        broadcast.connect(broadcastAddress, BROADCAST_PORT);
        Log.d("On-Scan", "Initialization END...");
    }

    public void close_Broadcast(){
        broadcast.close();
        recv_Broadcast.close();
    }

    public void scanNetwork_Broadcast() throws IOException{
        String outbound_String = "androidAuth|8889";
        byte[] outbound_Content = outbound_String.getBytes();

        DatagramPacket outbound = new DatagramPacket(outbound_Content, outbound_Content.length, broadcastAddress, BROADCAST_PORT);
        broadcast.send(outbound);
    }

    public String listen_Once_Broadcast() throws IOException{
        byte[] buffer = new byte[2048];
        DatagramPacket inbound = new DatagramPacket(buffer, buffer.length);
        recv_Broadcast.setSoTimeout(1000);
        String recv = "";

        try {
            recv_Broadcast.receive(inbound);
            recv = new String(buffer, 0, inbound.getLength());
            inbound.setLength(buffer.length);
        } catch(SocketTimeoutException e ){
            Log.d("On-Scan", "Attempt has timedout...");
            throw e;
        }

        //TODO: check incoming message format validity

        return recv;
    }

    public ArrayList<String> listen_Broadcast(int time) throws IOException{
        ArrayList<String> recived_Computers = new ArrayList<String>();
        recv_Broadcast.setSoTimeout(1000);

        byte[] buffer = new byte[2048];
        DatagramPacket inbound = new DatagramPacket(buffer, buffer.length);

        long currentTime = System.currentTimeMillis();
        long timeLimit = currentTime + (2*time);
        int points = 1;
        final String MESSAGE = "Listening";
        String postMessage = ".";
        while(currentTime <= timeLimit){
            try {
                recv_Broadcast.receive(inbound);
                String recv = new String(buffer, 0, inbound.getLength());
                recived_Computers.add(recv);
                inbound.setLength(buffer.length);
            } catch(SocketTimeoutException e ){
                Log.d("On-Scan", "Attempt has timedout...");
            }

            currentTime = System.currentTimeMillis();
            Log.d("On-Scan", String.valueOf(timeLimit) + "|" + String.valueOf(currentTime));
        }

        //TODO: check incoming message format validity

        return recived_Computers;
    }
}
