package com.mobile.sirs.g29.lockerroom;

import android.os.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

public class SignMessage {
    private static final String ALGORITHM = "RSA";
    private static final String PRIVATE_KEY_FILE = "private.key";
    private static final String PUBLIC_KEY_FILE = "public.key";

    public final void generateKey(int enthropy, String location) throws IOException, NoSuchAlgorithmException {
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

    public final byte[] signMessage(String message, PrivateKey key) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException{
        Signature sign = Signature.getInstance(ALGORITHM);
        sign.initSign(key);
        sign.update(message.getBytes());

        return sign.sign();
    }

    public final boolean verifyMessage(byte[] message, PublicKey key) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException{
        Signature sign = Signature.getInstance(ALGORITHM);
        sign.initVerify(key);
        sign.update(message);

        return sign.verify(message);
    }
}
