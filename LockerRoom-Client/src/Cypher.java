import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class Cypher{
	private static final String ALGORITHM = "RSA";
	


	public final static KeyPair generateKey() throws IOException, NoSuchAlgorithmException, NoSuchProviderException{
		Security.addProvider(new BouncyCastleProvider());

		final KeyPairGenerator kGen = KeyPairGenerator.getInstance(ALGORITHM, "BC");
		kGen.initialize(4096);
        final KeyPair keypair = kGen.generateKeyPair();
        System.out.println("Keys Generated Sucessfully!");

        return keypair;
	}

    public final static byte[] encript(byte[] message, PublicKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
		Security.addProvider(new BouncyCastleProvider());

    	byte[] cipheredMessage = null;
        final Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        cipher.init(Cipher.ENCRYPT_MODE, key);
        cipheredMessage = cipher.doFinal(message);
        return cipheredMessage;
    }

    public final static byte[] decript(byte[] message, PrivateKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
		Security.addProvider(new BouncyCastleProvider());

    	byte[] plainMessage = null;
        final Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        cipher.init(Cipher.DECRYPT_MODE, key);
        plainMessage = cipher.doFinal(message);
        return plainMessage;
    }
}