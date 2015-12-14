package com.mobile.sirs.g29.lockerroom;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class CypherMessage{
	private static final String ALGORITHM = "RSA";
	private static final String PRIVATE_KEY_FILE = "private.key";
	private static final String PUBLIC_KEY_FILE = "public.key";

	public final void generateKey(int enthropy, String location) throws IOException, NoSuchAlgorithmException{
        final KeyPairGenerator kGen = KeyPairGenerator.getInstance(ALGORITHM);
        kGen.initialize(enthropy);
        final KeyPair keypair = kGen.generateKeyPair();

        if(this.keyPairExist(location)){
            //keypair already generated
            return;
        }

		File privateKeyFile = new File(location, PRIVATE_KEY_FILE);
		File publicKeyFile = new File(location, PUBLIC_KEY_FILE);

      	privateKeyFile.createNewFile();
   		publicKeyFile.createNewFile();

        ObjectOutputStream publicKeyOS = new ObjectOutputStream(new FileOutputStream(publicKeyFile));
        publicKeyOS.writeObject(keypair.getPublic());
        publicKeyOS.close();

        ObjectOutputStream privateKeyOS = new ObjectOutputStream(new FileOutputStream(privateKeyFile));
        privateKeyOS.writeObject(keypair.getPrivate());
        privateKeyOS.close();

        Log.d("Key-Generator", "Private key: " + keypair.getPrivate().toString());
        Log.d("Key-Generator", "Public key: " + keypair.getPublic().toString());
	}

    public final boolean keyPairExist(String location){
        File privateKey = new File(location, PRIVATE_KEY_FILE);
        File publicKey = new File(location, PUBLIC_KEY_FILE);

        if(privateKey.exists() && publicKey.exists()){
            return true;
        }
        return false;
    }

    public final byte[] encript(byte[] message, PublicKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        byte[] cipheredMessage = null;
        final Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        cipher.init(Cipher.ENCRYPT_MODE, key);
        cipheredMessage = cipher.doFinal(message);
        return cipheredMessage;
    }

    public final byte[] decript(byte[] message, PrivateKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        byte[] plainMessage = null;
        final Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        cipher.init(Cipher.DECRYPT_MODE, key);
        plainMessage = cipher.doFinal(message);
        return plainMessage;
    }

    public PrivateKey getPrivate(File path){
        PrivateKey answer = null;
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(path.toString()));
            answer = (PrivateKey) inputStream.readObject();
            inputStream.close();
        } catch(Exception e){
            e.printStackTrace();
            Log.e("Key", "Could not atain key from path");
        }

        return answer;
    }

    public PublicKey getPublic(File path){
        PublicKey answer = null;
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(path.toString()));
            answer = (PublicKey) inputStream.readObject();
            inputStream.close();
        } catch(Exception e){
            e.printStackTrace();
            Log.e("Key", "Could not atain key from path");
        }

        return answer;
    }
}