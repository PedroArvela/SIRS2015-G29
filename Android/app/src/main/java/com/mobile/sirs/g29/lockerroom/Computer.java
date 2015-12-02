package com.mobile.sirs.g29.lockerroom;

import java.security.PrivateKey;
import java.security.PublicKey;

public class Computer{
    public enum STATUS {NONE, RECIVING, WORKING, SENDING, IDLE}

    private String _comptuerName;
    private String _computerIP;
    private int _port;
    private boolean _authorized;
    private int _imageTag;
    private String _fingerprint;
    private STATUS _status;
    private PrivateKey _computerSign = null;
    private PublicKey _computerCipher = null;

    public Computer(String name, String ip, int port, String fingerprint){
        _comptuerName = name;
        _computerIP = ip;
        _port = port;
        _authorized = false;
        _fingerprint = fingerprint;
        _status = STATUS.NONE;
        _imageTag = R.drawable.computerlistitemunauthorized;

    }

    public  PrivateKey get_computerSign(){ return _computerSign; }

    public void set_comptuerSign(PrivateKey key) { _computerSign = key; }

    public  PublicKey get_computerCipher(){ return _computerCipher; }

    public void set_comptuerCipher(PublicKey key) { _computerCipher = key; }

    public String get_comptuerName() { return _comptuerName; }

    public String get_computerIP() { return _computerIP; }

    public int get_port() { return _port; }

    public String get_fingerprint(){ return _fingerprint; }

    public boolean is_authorized() { return _authorized; }

    public int get_imageTag(){ return _imageTag; }

    public STATUS get_status(){ return _status; }

    public void set_status(STATUS status){ _status = status; }

    public void set_authorized(boolean value){
        _authorized = value;
        if(_authorized){
            _imageTag = R.drawable.computerlistitemauthorized;
            _status = STATUS.IDLE;
        } else {
            _imageTag = R.drawable.computerlistitemunauthorized;
            _status = STATUS.NONE;
        }

    }
}