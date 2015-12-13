import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import com.mobile.sirs.g29.lockerroom.Message;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class PhoneListener implements Runnable {
	private static final int RECV_PORT = 8888;
	private int ANDROID_PORT = 8889;
	private int REQUEST_PORT;
	private InetAddress SEND_ADD;
	private KeyPair key;
	private PublicKey mbKey;
	private PublicKey pub;
	private DatagramSocket broadcast_recv_send;
	private DatagramSocket requester;
	private DatagramPacket recvPacket;
	private DatagramPacket sendPacket;
	private DatagramPacket reqPacketPort;
	private DatagramPacket reqPacketKey;
	private byte[] buffer;
    private SecureRandom random = new SecureRandom();

	private void initialize() {
		try {
			broadcast_recv_send = new DatagramSocket(RECV_PORT);
		} catch (SocketException e) {
			// IGNORE
			e.printStackTrace();
		}
	}

	private void request(int req_port) {
		try {
			requester = new DatagramSocket(REQUEST_PORT);
		} catch (SocketException e) {
			// IGNORE
			e.printStackTrace();
		}
	}

	private PublicKey getPBKey(byte[] key) {
		try {

			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
			PublicKey publicKey = keyFactory.generatePublic(keySpec);

			return publicKey;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private KeyPair generateKeys() throws NoSuchAlgorithmException, IOException, NoSuchProviderException {
		KeyPair pair = Cypher.generateKey();

		return pair;
	}

	private byte[] pbkeyEncoded(KeyPair key) {
		pub = key.getPublic();
		byte[] pubk = pub.getEncoded();

		return pubk;
	}

	private byte[] encript(byte[] message, PublicKey key) throws InvalidKeyException, NoSuchPaddingException,
			NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		byte[] msg = Cypher.encript(message, key);

		return msg;
	}

	private byte[] decript(byte[] message, PrivateKey key) throws InvalidKeyException, NoSuchPaddingException,
			NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException {
		byte[] msg = Cypher.decript(message, key);

		return msg;
	}

	public byte[] sendFile(String filename) throws IOException {
		File file = new File("C:/" + filename + ".txt");

		// create a buffer for the file data
		int len = (int) file.length();
		byte[] message = new byte[len];
		FileInputStream in = new FileInputStream(file);
		int bytes_read = 0, n;
		do {
			n = in.read(message, bytes_read, len - bytes_read);
			bytes_read += n;
		} while ((bytes_read < len) && (n != -1));

		in.close();
		return message;
	}

	private void sendPacket(byte[] send, int PORT) throws IOException {
		DatagramPacket dat = new DatagramPacket(send, send.length, SEND_ADD, PORT);
		requester.connect(SEND_ADD, PORT);
		requester.send(dat);
		requester.disconnect();

	}
	
	private void sendPacketBroad(byte[] send, int PORT) throws IOException {
		DatagramPacket dat = new DatagramPacket(send, send.length, SEND_ADD, PORT);
		broadcast_recv_send.connect(SEND_ADD, PORT);
		broadcast_recv_send.send(dat);
		broadcast_recv_send.disconnect();

	}


	@Override
	public void run() {
		String recv_message;
		String send_message;
		String req_message;
		String[] parts;
		byte[] bufferAux;
		byte[] pbkey;
		boolean done = false;

		this.initialize();
		Security.addProvider(new BouncyCastleProvider());

		System.out.println(pub);
		while (!done) {
			try {

				buffer = new byte[2048];
				bufferAux = new byte[2048];
				BigInteger challenge1 = new BigInteger(20,random);
				BigInteger challenge2 = null;

				// Generates keypair
				key = this.generateKeys();
				recvPacket = new DatagramPacket(buffer, buffer.length);

				System.out.println("Waiting for mobile...");

				//FIRST MESSAGE
				broadcast_recv_send.receive(recvPacket);

				// Saves Mobile Public Key
				byte[] data = recvPacket.getData();
				Message pubkey = Message.retriveMessage(data);
				byte[] keymob = pubkey.get_Content();
				
				mbKey = this.getPBKey(keymob);
				
				System.out.println("Mobile Key Saved!");
				System.out.println(mbKey);
				SEND_ADD = recvPacket.getAddress();

				
				//SECOND MESSAGE
				
				System.out.println("Sending PC public key...");

				// Retrieves public key encoded
				pbkey = this.pbkeyEncoded(key);

				String origin = InetAddress.getLocalHost().toString();
				String destination = SEND_ADD.toString();
				
				Message firstMsg = new Message(origin,destination,challenge1,challenge2);
				firstMsg.set_Conent(pbkey);
				byte[] first;
				byte[] firstEnc;

				first = Message.getEncoded(firstMsg);

				firstEnc = this.encript(first, mbKey);
				
				// Sends public key
				this.sendPacketBroad(firstEnc, ANDROID_PORT);
				System.out.println("Key sent!");
				System.out.println(pbkey);
				System.out.println(challenge1);

				// Listen for service port
				System.out.println("Waiting for mobile...");

				reqPacketPort = new DatagramPacket(buffer, buffer.length);
				broadcast_recv_send.receive(reqPacketPort);

				byte[] PortDataEnc = reqPacketPort.getData();
				byte[] PortData = this.decript(PortDataEnc, key.getPrivate());
				
				Message PortMsg = Message.retriveMessage(PortData);
				
				BigInteger cha1 = PortMsg.get_challange1();
				challenge2 = PortMsg.get_challange2();
				challenge1.add(new BigInteger("1"));
				if(cha1==challenge1){
					System.out.println("challenge OK");
				}
				
				byte[] reqPort = PortMsg.get_Content();
				req_message = new String(buffer, 0, reqPort.length);

				// Saves Request Port
				REQUEST_PORT = Integer.parseInt(req_message);
				System.out.println("Saved Request Port!");


				// Opens request socket
				this.request(REQUEST_PORT);

				// Closes broadcast socket
				broadcast_recv_send.close();

				this.transfer();
				done = true;

			} catch (SocketException e) {

				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchProviderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private byte[] Input() throws IOException {

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Enter c to Encrypt or d to Decrypt");
		String type = br.readLine().trim().toLowerCase();
		byte[] req = type.getBytes();

		return req;
	}

	
	public void transfer() {

		try {
			while (true) {

				byte[] buffer2 = new byte[2048];

				// Request send
				byte[] in = this.Input();
				String inp = new String(in);

				if (inp.contentEquals("c")) {
					this.sendPacket(in, REQUEST_PORT);
					//filename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");

					// DEMO - Creates byte[] from file encripted with mobile
					// public key
					Path path = Paths.get("C:/test.txt");
				    byte[] file = Files.readAllBytes(path);
					byte[] enc = this.encript(file, mbKey);

					// Send encripted message to cypher
					this.sendPacket(enc, REQUEST_PORT);

					// Receive ciphered file
					DatagramPacket reqPacket3 = new DatagramPacket(buffer2, buffer2.length);
					requester.receive(reqPacket3);
					byte[] req3 = reqPacket3.getData();


					// Saves ciphered message to file
					FileOutputStream outputStream = new FileOutputStream("C:/cyphtest.txt");
					//FileOutputStream outputStream = new FileOutputStream("C:/Users/Andre/Documents/cyphtest.txt");
		            outputStream.write(req3);
		            outputStream.close();
					
		            //Prints ciphered message
					String cipmsg = new String(req3, 0, reqPacket3.getLength());
					System.out.println("Cyphered: " + cipmsg);

					//Decipher DEMO
					byte[] dec = "d".getBytes();
					this.sendPacket(dec, REQUEST_PORT);
					
					//Read bytes from encrypted file to byte[]
					Path pathDec = Paths.get("C:/cyphtest.txt");
					//Path pathDec = Paths.get("C:/Users/Andre/Documents/cyphtest.txt");
				    byte[] fileDec = Files.readAllBytes(pathDec);
				    
				    //Sends encrypted message to decipher
					DatagramPacket file2 = new DatagramPacket(fileDec, reqPacket3.getLength(), SEND_ADD, REQUEST_PORT);
					requester.connect(SEND_ADD, REQUEST_PORT);
					requester.send(file2);
					requester.disconnect();

					// Receive deciphered file
					byte[] test = new byte[128];
					DatagramPacket file3 = new DatagramPacket(test, test.length);
					requester.receive(file3);

					//Use PC private key to get real message
					byte[] dc = file3.getData();
					PrivateKey pk = key.getPrivate();
					byte[] mymessage = this.decript(dc, pk);

					
					// Saves deciphered message to file
					FileOutputStream outputStreamDec = new FileOutputStream("C:/dectest.txt");
					//FileOutputStream outputStreamDec = new FileOutputStream("C:/Users/Andre/Documents/dectest.txt");
		            outputStreamDec.write(mymessage);
		            outputStreamDec.close();
					// Print Deciphered message
					String dech = new String(mymessage, 0, mymessage.length);
					System.out.println("Deciphered: " + dech);
				} else {
					System.out.println("Please choose c for the DEMO");
				}
			}

		} catch (

		SocketException e)

		{

			e.printStackTrace();
		} catch (

		IOException e)

		{
			e.printStackTrace();
		} catch (

		InvalidKeyException e)

		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (

		NoSuchPaddingException e)

		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (

		NoSuchAlgorithmException e)

		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (

		IllegalBlockSizeException e)

		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (

		BadPaddingException e)

		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (

		NoSuchProviderException e)

		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
