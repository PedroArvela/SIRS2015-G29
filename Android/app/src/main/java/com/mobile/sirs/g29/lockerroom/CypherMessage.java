package com.mobile.sirs.g29.lockerroom;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
	}

    public final boolean keyPairExist(String location){
        File privateKey = new File(location, PRIVATE_KEY_FILE);
        File publicKey = new File(location, PUBLIC_KEY_FILE);

        if(privateKey.exists() && publicKey.exists()){
            return true;
        }
        return false;
    }

    public final byte[] encript(String message, PublicKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        byte[] cipheredMessage = null;
        final Cipher cipher = Cipher.getInstance(ALGORITHM);

        cipher.init(Cipher.ENCRYPT_MODE, key);
        cipheredMessage = cipher.doFinal(message.getBytes());
        return cipheredMessage;
    }

    public final String decript(byte[] message, PrivateKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        String plainMessage = null;
        final Cipher cipher = Cipher.getInstance(ALGORITHM);

        cipher.init(Cipher.DECRYPT_MODE, key);
        plainMessage = new String(cipher.doFinal());
        return plainMessage;
    }
}