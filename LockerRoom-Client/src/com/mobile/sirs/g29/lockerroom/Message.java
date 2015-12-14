package com.mobile.sirs.g29.lockerroom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;

public class Message implements Serializable{
    public enum REQUEST {CIPHER, DICIPHER, INVALID};

    private String _origin;
    private String _destination;
    private REQUEST _type;

    private byte[] _pcChallange;
    private byte[] _phoneChallange;

    private byte[] _content;

    public String get_origin(){ return _origin; }
    public String get_destination(){ return _destination; }
    public byte[] get_pcChallange(){ return _pcChallange; }
    public byte[] get_phoneChallange(){ return _phoneChallange; }
    public byte[] get_Content(){ return _content; }
    public REQUEST get_type(){ return _type; }

    public void set_Content(byte[] msg){ _content = msg; }
    public void set_Type(REQUEST type){ _type = type; }

    public Message(String origin, String destination, byte[] challange1, byte[] challange2){
        _origin = origin;
        _destination = destination;
        _pcChallange = challange1;
        _phoneChallange = challange2;
        _content = null;
    }

    static final public byte[] getEncoded(Message msg) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(msg);
        return baos.toByteArray();
    }

    static final public Message retriveMessage(byte[] content) throws IOException, ClassNotFoundException{
        ByteArrayInputStream bais = new ByteArrayInputStream(content);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (Message)ois.readObject();
    }
}
