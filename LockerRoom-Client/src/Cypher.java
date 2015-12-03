import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Cypher{
	private static final String ALGORITHM = "RSA";

	public final static KeyPair generateKey() throws IOException, NoSuchAlgorithmException{
		final KeyPairGenerator kGen = KeyPairGenerator.getInstance(ALGORITHM);
		kGen.initialize(1024);
        final KeyPair keypair = kGen.generateKeyPair();

        return keypair;
	}

    public final static byte[] encript(String message, PublicKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        byte[] cipheredMessage = null;
        final Cipher cipher = Cipher.getInstance(ALGORITHM);

        cipher.init(Cipher.ENCRYPT_MODE, key);
        cipheredMessage = cipher.doFinal(message.getBytes());
        return cipheredMessage;
    }

    public final static String decript(byte[] message, PrivateKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        String plainMessage = null;
        final Cipher cipher = Cipher.getInstance(ALGORITHM);

        cipher.init(Cipher.DECRYPT_MODE, key);
        plainMessage = new String(cipher.doFinal());
        return plainMessage;
    }
}