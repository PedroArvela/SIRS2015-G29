package com.mobile.sirs.g29.lockerroom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;

public class Message implements Serializable{
    private String _origin;
    private String _destination;

    private BigInteger _challange1;
    private BigInteger _challange2;

    private byte[] _content;

    public String get_origin(){ return _origin; }
    public String get_destination(){ return _destination; }
    public BigInteger get_challange1(){ return _challange1; }
    public BigInteger get_challange2(){ return _challange2; }
    public byte[] get_Content(){ return _content; }

    public void set_Conent(byte[] msg){ _content = msg; }

    public Message(String origin, String destination, BigInteger challange1, BigInteger challange2){
        _origin = origin;
        _destination = destination;
        _challange1 = challange1;
        _challange2 = challange2;
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
