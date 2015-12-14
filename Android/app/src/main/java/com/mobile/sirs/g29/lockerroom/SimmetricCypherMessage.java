package com.mobile.sirs.g29.lockerroom;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class SimmetricCypherMessage {
	private static final String ALGORITHM = "AES";
    private static final String SIMETRIC_KEY_FILE = "secret.key";

	public final void generateKey(String location){
        KeyGenerator kGen;
        SecretKey key;
        File keyFolder = new File(location);
        File keyFile = null;

        if(this.keyExist(location)){
            //keypair already generated
            return;
        }

        try {
            Log.d("SIMKEY", "Creating folder");
            keyFolder.mkdir();
            Log.d("SIMKEY", "Folder created: " + (keyFolder.exists() && keyFolder.isDirectory()));

            Log.d("SIMKEY", "Creating keyfile");
            keyFile = new File(location, SIMETRIC_KEY_FILE);
            keyFile.createNewFile();
            Log.d("SIMKEY", "File created: " + (keyFile.exists() && keyFile.isFile()));

            Log.d("SIMKEY", "GENERATING NEW KEY");
            kGen= KeyGenerator.getInstance(ALGORITHM);
            kGen.init(256);
            key = kGen.generateKey();
            Log.d("SIMKEY", "NEW KEY GENERATED");

            ObjectOutputStream publicKeyOS = new ObjectOutputStream(new FileOutputStream(keyFile));
            publicKeyOS.writeObject(key);
            publicKeyOS.close();

        } catch (NoSuchAlgorithmException e){
        //IGNORE
        } catch (Exception e) {
            Log.e("SIMKEY", "EXCEPTION");
            e.printStackTrace();
        }
	}

    public final boolean keyExist(String location){
        File key = new File(location, SIMETRIC_KEY_FILE);

        return key.exists() && key.isFile();
    }

    public final byte[] encript(byte[] message, SecretKey key) {
        byte[] cipheredMessage = null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            cipheredMessage = cipher.doFinal(message);

        } catch (Exception e){
            e.printStackTrace();
            //nothing to do
        }

        return cipheredMessage;
    }

    public final byte[] decript(byte[] message, SecretKey key) {
        byte[] plainMessage = null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            plainMessage = cipher.doFinal(message);

        } catch (Exception e){
            e.printStackTrace();
            //nothing to do
        }

        return plainMessage;
    }

    public SecretKey getSecretKey(String path){
        return this.getSecretKey(new File(path));
    }

    public SecretKey getSecretKey(File path){
        SecretKey answer = null;
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(path.toString()));
            answer = (SecretKey) inputStream.readObject();
            inputStream.close();
        } catch(Exception e){
            e.printStackTrace();
            Log.e("Key", "Could not atain key from path");
        }

        return answer;
    }
}