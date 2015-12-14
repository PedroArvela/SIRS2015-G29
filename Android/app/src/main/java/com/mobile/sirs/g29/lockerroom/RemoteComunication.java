package com.mobile.sirs.g29.lockerroom;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RemoteComunication {
    //network private camps
    private static final String BROADCAST_IP = "192.168.1.255";
    private static final int BROADCAST_PORT = 8888;
    private static final int BROADCAST_LISTEN_PORT = 8889;
    private DatagramSocket broadcast;
    private DatagramSocket recv_Broadcast;
    private InetAddress broadcastAddress;

    String _pathPubKey = null;

    //datastructures

    public RemoteComunication(String pathToPubKey){
        _pathPubKey = pathToPubKey;
    }

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

    private byte[] append(byte[] a, byte[] b ){
        int size = a.length + b.length;
        byte[] answer = new byte[size];

        for(int i = 0; i < a.length; i++){
            answer[i] = a[i];
        }
        for(int i = 0; i < b.length; i++){
            answer[a.length+i] = b[i];
        }
        return answer;
    }

    public void scanNetwork_Broadcast() throws IOException{
        PublicKey phonePkey = (new CypherMessage()).getPublic(new File(_pathPubKey + "/public.key"));
        Log.d("On-Broadcast", "Public key: " + phonePkey);
        byte[] ppk = phonePkey.getEncoded();

        Message outContent = new Message(InetAddress.getLocalHost().getHostName(), "Many", null, null);
        outContent.set_Content(ppk);

        byte[] outByte = Message.getEncoded(outContent);
        Log.d("On-Broadcast", "Broadcast Message size: " + outByte.length);

        DatagramPacket outbound = new DatagramPacket(outByte, outByte.length, broadcastAddress, BROADCAST_PORT);
        broadcast.send(outbound);
    }

    public void send_ServicePort(int port, String ip){
        String portContent = (Integer.toString(port));
        byte[] outbound_Port = portContent.getBytes();
        Log.d("On-Scan", _pathPubKey + "/public.key");
        PublicKey msgPub = (new CypherMessage()).getPublic(new File(_pathPubKey + "/public.key"));
        byte[] outbound_key = msgPub.getEncoded();

        InetAddress address = null;

        try{
            address = InetAddress.getByName(ip);
            broadcast.setBroadcast(false);
            broadcast.connect(address, BROADCAST_PORT);
            Log.d("Send-Port", "ADDRESS-Sending Port " + port + " to: " + address.toString());
            DatagramPacket outbound = new DatagramPacket(outbound_Port, outbound_Port.length, address, BROADCAST_PORT);
            broadcast.send(outbound);
            broadcast.setBroadcast(true);

        } catch (Exception e){
            e.printStackTrace();
            Log.e("On-Scan", "EXCEPTION-PORT");
        } finally {
            broadcast.disconnect();
        }
        try{
            address = InetAddress.getByName(ip);
            broadcast.connect(address, BROADCAST_PORT);
            Log.d("Send-PubKey", "ADDRESS-Sending key \"" + msgPub.toString() + "\" to: " + address.toString());
            DatagramPacket outbound = new DatagramPacket(outbound_key, outbound_key.length, address, BROADCAST_PORT);
            broadcast.send(outbound);
            broadcast.setBroadcast(true);

        } catch (Exception e){
            e.printStackTrace();
            Log.e("On-Scan", "EXCEPTION-PUBLICKEY");
        } finally {
            broadcast.disconnect();
        }

    }

    public void listen_Once_Key(ArrayList<Computer> list){
        PublicKey answer = null;
        byte[] buffer = new byte[2024];
        DatagramPacket inbound = new DatagramPacket(buffer, buffer.length);

        try{
            Log.d("On-Scan-Key", "Attempting to read incoming key");
            recv_Broadcast.setSoTimeout(1500);
            recv_Broadcast.receive(inbound);

            Log.d("On-Scan-Key", "Recived Message from: " + inbound.getAddress().toString());
            byte[] recv = inbound.getData();
            byte[] recvTrans = new byte[inbound.getLength()];
            for(int i = 0; i < inbound.getLength(); i++){
                recvTrans[i] = recv[i];
            }
            X509EncodedKeySpec encoded = new X509EncodedKeySpec(recvTrans);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            answer = kf.generatePublic(encoded);

            Log.d("On-Scan-Key", "Recived key form: " + inbound.getAddress().toString() + " is\n" + answer.toString());

            //Generate computer entry
            String hostname = InetAddress.getByName(inbound.getAddress().toString()).getHostName();
            String ip = inbound.getAddress().toString();
            int port = inbound.getPort();
            String f = "replacethis";

            Computer addToList = new Computer(hostname, ip, port, f, null, null);
            addToList.set_comptuerCipher(answer);
            list.add(addToList);

            /*
            for(Computer c : list){
                Log.d("On-Scan-Key", "Testing IP match: " + c.get_computerIP() + " | " + inbound.getAddress().toString());
                if(inbound.getAddress().toString().equals("/"+c.get_computerIP())){
                    Log.d("On-Scan-Key", "Recived Packet with key from: " + c.get_comptuerName());
                    c.set_comptuerCipher(answer);
                }
            }
            */

            inbound.setLength(buffer.length);
        } catch(SocketTimeoutException e ){
            Log.d("On-Scan-key", "Attempt has timedout...");
        } catch(Exception e){
            e.printStackTrace();
            Log.e("On-Scan-key", "EXECPTION");
            //nothing to do
        }
    }

    public Computer retrive_One_CompKey(File PrivKeyFile) throws SocketTimeoutException{
        Computer answer = null;
        byte[] buffer = new byte[2048];
        DatagramPacket inbound = new DatagramPacket(buffer, buffer.length);
        try{
            recv_Broadcast.setSoTimeout(1000);
        } catch (SocketException e){
            e.printStackTrace();
            Log.e("On-Scan-Key", "Socket exception, should not happen");
        }

        try{
            recv_Broadcast.receive(inbound);
            Log.e("On-Scan-Key", "Recived answer from: " + inbound.getAddress());
            CypherMessage cm = new CypherMessage();
            PrivateKey mykey = cm.getPrivate(PrivKeyFile);

            byte[] recv = new byte[inbound.getLength()];
            for(int i = 0; i < recv.length; i++){
                recv[i] = buffer[i];
            }

            recv = cm.decript(recv, mykey);

            Message recvMessage = Message.retriveMessage(recv);
            String hostname = inbound.getAddress().getHostName();
            answer = new Computer(hostname, recvMessage.get_origin(), inbound.getPort(), "replaceme", recvMessage.get_challange1(), recvMessage.get_challange2());

            X509EncodedKeySpec encoded = new X509EncodedKeySpec(recvMessage.get_Content());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey compKey = kf.generatePublic(encoded);
            Log.d("On-Scan-Key", "Recived key is: " + compKey);

            answer.set_comptuerCipher(compKey);
            answer.set_fingerprint(compKey.hashCode() + "");

        } catch (SocketTimeoutException e){
            Log.d("On-Scan-Key", "Attempt has timedout...");
            throw e;
        } catch (Exception e){
            e.printStackTrace();
            Log.e("On-Scan-key", "Exception has occured...");
        }


        return answer;
    }

    public void send_ServicePort(Computer target){
        BigInteger challange = new BigInteger(501, new SecureRandom());
        target.set_phoneChallange(challange);
        try {
            target.set_computerChallange(target.get_computerChallange().add(BigInteger.ONE));
            BigInteger chaPC = target.get_computerChallange();

            Message outbound = new Message(InetAddress.getLocalHost().getHostName(), target.get_comptuerName(), chaPC, challange);
            String content = target.get_servicePort() + "";
            Log.d("On-Service-Distribution", "Sending service port: " + content + " to: " + target.get_comptuerName());
            outbound.set_Content(content.getBytes());

            byte[] outMsg = Message.getEncoded(outbound);
            byte[] cipherdOutMsg = (new CypherMessage()).encript(outMsg, target.get_computerCipher());
            InetAddress address = InetAddress.getByName(target.get_computerIP());

            broadcast.setBroadcast(false);
            broadcast.connect(address, BROADCAST_PORT);
            DatagramPacket outPacket = new DatagramPacket(cipherdOutMsg, cipherdOutMsg.length, address, BROADCAST_PORT);
            broadcast.send(outPacket);
            broadcast.disconnect();
            broadcast.setBroadcast(true);

        } catch(UnknownHostException e){
            e.printStackTrace();
            Log.e("On-Service-Distribution", "Could not attain host");
        } catch(IOException e){
            e.printStackTrace();
            Log.e("On-Service-Distribution", "Could not get message encoding");
        } catch(Exception e){
            e.printStackTrace();
            Log.e("On-Service-Distribution", "Could not cipher outgoing message...");
        }
    }

    public Computer listen_Once_Acknowledge(ArrayList<Computer> compList, PrivateKey myKey){
        Computer answer = null;
        byte[] buffer = new byte[2048];
        DatagramPacket inbound = new DatagramPacket(buffer, buffer.length);

        try{
            recv_Broadcast.setSoTimeout(1000);

            recv_Broadcast.receive(inbound);
            byte[] recvMsgBytes = new byte[inbound.getLength()];
            for(int i = 0; i < inbound.getLength(); i++){
                recvMsgBytes[i] = inbound.getData()[i];
            }
            byte[] recvMsgDecodedBytes = (new CypherMessage()).decript(recvMsgBytes, myKey);
            Message recvMsg = Message.retriveMessage(recvMsgDecodedBytes);

            Computer target = null;
            for(Computer c : compList){
                if(recvMsg.get_origin().equals(c.get_computerIP())){
                    Log.d("On-Ack-Try", "Matched with: " + c.get_comptuerName() + " (local) with " + recvMsg.get_origin() + " (remote)");
                    target  = c;
                    break;
                }
            }
            if(target == null){
                throw new UnknownError();
            }

            //validate possible computer target
            if(target.get_phoneChallange().compareTo(recvMsg.get_challange2()) == -1){
                Log.d("On-Ack-Try", "Challange was matched...");
                target.set_phoneChallange(recvMsg.get_challange2());

                answer = target;
            } else {
                throw new InvalidParameterException();
            }

        } catch(InvalidParameterException e){
            Log.e("On-Ack-Try", "Challange wasn't passed...");
        } catch(UnknownError e){
            Log.e("On-Ack-Try", "Recived packet didn't match any expected computers...");
        } catch (SocketException e){
            e.printStackTrace();
            Log.e("On-Ack-Try", "Could not set timeout...");
        } catch (SocketTimeoutException e){
            Log.e("On-Ack-Try", "Attempt has timedout...");
        } catch (IOException e){
            e.printStackTrace();
            Log.e("On-Ack-Try", "Socket has thrown exception...");
        } catch (ClassNotFoundException e){
            e.printStackTrace();
            Log.e("On-Ack-Try", "Recived packet didn't contain expected format...");
        } catch (NoSuchPaddingException |NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e){
            e.printStackTrace();
           Log.e("On-Ack-Try", "Could not decript recv message...");
        }


        return answer;
    }

    public String listen_Once_Broadcast() throws InvalidClassException, SocketTimeoutException{
        byte[] buffer = new byte[2048];
        DatagramPacket inbound = new DatagramPacket(buffer, buffer.length);
        try {
            recv_Broadcast.setSoTimeout(1000);
        } catch(SocketException e){
            e.printStackTrace();
            //nothing to do
        }
        String recv = "";
        String regEx = "[A-Za-z0-9\\-]+\\\\|((([0-9]{1,3})\\\\.){3})[0-9]{1,3}\\\\|[0-9]+";

        try {
            recv_Broadcast.receive(inbound);
            recv = new String(buffer, 0, inbound.getLength());
            inbound.setLength(buffer.length);

            //validate input
            //TODO: FIX REGEX
            //if(!recv.matches(regEx)){
            //    throw new InvalidClassException("Recived string didn't match expected result");
            //}
        } catch(SocketTimeoutException e ){
            Log.d("On-Scan", "Attempt has timedout...");
            throw e;
        } catch(InvalidClassException e){
            throw e;
        } catch(IOException e){
            e.printStackTrace();
            //nothing to do
        }

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
                Log.d("On-Scan", "Computer scaned...");
                recived_Computers.add(recv);
                inbound.setLength(buffer.length);
            } catch(SocketTimeoutException e ){
                Log.d("On-Scan", "Attempt has timedout...");
            }

            currentTime = System.currentTimeMillis();
            Log.d("On-Scan", String.valueOf(timeLimit) + "|" + String.valueOf(currentTime));
        }

        return recived_Computers;
    }
}
