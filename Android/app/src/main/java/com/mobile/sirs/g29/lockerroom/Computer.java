package com.mobile.sirs.g29.lockerroom;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Objects;

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
    private int _servicePort;
    private ServiceTask _service = null;

    private BigInteger _computerChallange;
    private BigInteger _phoneChallange;

    public Computer(String name, String ip, int port, String fingerprint, BigInteger computerChallange, BigInteger phoneChallange){
        _comptuerName = name;
        _computerIP = ip;
        _port = port;
        _authorized = false;
        _fingerprint = fingerprint;
        _status = STATUS.NONE;
        _imageTag = R.drawable.computerlistitemunauthorized;
        _servicePort = -1;
        _computerChallange = computerChallange;
        _phoneChallange = phoneChallange;
    }

    public void set_service(ServiceTask service) { _service = service; }

    public ServiceTask get_service() { return _service; }

    public int get_servicePort(){ return _servicePort; }

    public void set_servicePort(int port){ _servicePort = port; }

    public  PrivateKey get_computerSign(){ return _computerSign; }

    public void set_comptuerSign(PrivateKey key) { _computerSign = key; }

    public  PublicKey get_computerCipher(){ return _computerCipher; }

    public void set_comptuerCipher(PublicKey key) { _computerCipher = key; }

    public String get_comptuerName() { return _comptuerName; }

    public String get_computerIP() { return _computerIP; }

    public int get_port() { return _port; }

    public String get_fingerprint(){ return _fingerprint; }

    public void set_fingerprint(String fingerprint){ _fingerprint = fingerprint; }

    public boolean is_authorized() { return _authorized; }

    public int get_imageTag(){ return _imageTag; }

    public STATUS get_status(){ return _status; }

    public void set_status(STATUS status){ _status = status; }

    public BigInteger get_computerChallange(){ return _computerChallange; }

    public BigInteger get_phoneChallange(){ return _phoneChallange; }

    public void set_phoneChallange(BigInteger challange){ _phoneChallange = challange; }

    public void set_computerChallange(BigInteger challange) { _computerChallange = challange; }

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

    @Override
    public  boolean equals(Object obj){
        if(!(obj instanceof Computer))
            return false;

        return _comptuerName.equals(((Computer) obj).get_comptuerName()) && _computerIP.equals(((Computer) obj).get_computerIP()) && (_port == ((Computer) obj).get_port());
    }

}