package com.mobile.sirs.g29.lockerroom;

import android.app.Application;
import android.util.Log;

import java.net.DatagramSocket;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;

public class InformedApplication extends Application{
    private ComputerListAdapter _globalAdpater = null;
    private String _msgKeyFolder = null;

    private ArrayList<Integer> _assignedPorts = new ArrayList<Integer>();

    private boolean isPortAvailable(int port){
        return !_assignedPorts.contains(port);
    }

    private boolean assignPort(int port){
        if(_assignedPorts.contains(port)){
            return false;
        } else{
            _assignedPorts.add(port);
            return true;
        }
    }

    public boolean freePortAssign(int port){
        if(_assignedPorts.contains(port)){
            _assignedPorts.remove(_assignedPorts.indexOf(port));
            return true;
        } else{
            return false;
        }
    }

    public int getRandomAvailableUDPPort(int lowerBound, int upperBound){
        boolean found = false;
        int answer = -1;
        int test = ((Double)(Math.floor(Math.random() * upperBound) + lowerBound)).intValue();
        boolean isFree;

        while(!found){
            if(this.isPortAvailable(test)){
                this.assignPort(test);
                return test;
            } else{
                test = ((Double)(Math.floor(Math.random() * upperBound) + lowerBound)).intValue();
            }
        }

        return answer;
    }

    public void setAdapter(ComputerListAdapter adapter){
        if(_globalAdpater == null) {
            _globalAdpater = adapter;
        } else {
            Log.e("ApplicationGlobal", "Adapter already set");
        }
    }

    public void set_msgKeyFolder(String path){
        if(_msgKeyFolder == null){
            _msgKeyFolder = path;
        } else {
            Log.e("ApplicationGlobal", "Message Key path already set");
        }
    }

    public String get_msgKeyFolder(){ return _msgKeyFolder; }

    public ComputerListAdapter getAdapter(){
        return _globalAdpater;
    }
}
