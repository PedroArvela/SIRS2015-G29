package com.mobile.sirs.g29.lockerroom;

public class Computer{
    private String _comptuerName;
    private String _computerIP;
    private int _port;
    private boolean _authorized;
    private int _imageTag;

    public Computer(String name, String ip, int port){
        _comptuerName = name;
        _computerIP = ip;
        _port = port;
        _authorized = false;
        _imageTag = R.drawable.computerlistitemunauthorized;

    }

    public String get_comptuerName() { return _comptuerName; }

    public String get_computerIP() { return _computerIP; }

    public int get_port() { return _port; }

    public boolean is_authorized() { return _authorized; }

    public int get_imageTag(){ return _imageTag; }

    public void set_authorized(boolean value){
        _authorized = value;
        if(_authorized){
            _imageTag = R.drawable.computerlistitemauthorized;
        } else {
            _imageTag = R.drawable.computerlistitemunauthorized;
        }

    }
}