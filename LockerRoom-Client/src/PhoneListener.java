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
import java.nio.ByteBuffer;
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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import com.mobile.sirs.g29.lockerroom.Message;

import java.util.Base64.Decoder;

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
	private DatagramPacket reqPacketPort;
	private byte[] buffer;
	private SecureRandom random = new SecureRandom();
	private BigInteger challengePC = new BigInteger(20, random);
	private BigInteger challengeMOB = null;
	private String origin;
	private String destination;
	private PrivateKey pvkey;
	
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
		byte[] pbkey;
		boolean done = false;

		this.initialize();
		Security.addProvider(new BouncyCastleProvider());

		System.out.println(pub);
		while (!done) {
			try {

				buffer = new byte[2048];
				// Generates keypair
				key = this.generateKeys();
				pvkey = key.getPrivate();

				recvPacket = new DatagramPacket(buffer, buffer.length);

				System.out.println("Waiting for mobile...");

				// FIRST MESSAGE
				broadcast_recv_send.receive(recvPacket);

				// Saves Mobile Public Key
				byte[] data = recvPacket.getData();
				Message pubkey = Message.retriveMessage(data);
				byte[] keymob = pubkey.get_Content();

				mbKey = this.getPBKey(keymob);

				System.out.println("Mobile Key Saved!");
				System.out.println(mbKey);
				SEND_ADD = recvPacket.getAddress();

				// SECOND MESSAGE

				System.out.println("Sending PC public key...");

				// Retrieves public key encoded
				pbkey = this.pbkeyEncoded(key);

				origin = InetAddress.getLocalHost().toString();
				destination = SEND_ADD.toString();
				
				byte[] chaPC = challengePC.toByteArray();

				byte[] ENCchaPC = this.encript(chaPC, mbKey);
				byte[] ENCpbkey = this.encript(pbkey, mbKey);
						
				Message firstMsg = new Message(origin, destination, ENCchaPC, null);
				firstMsg.set_Content(ENCpbkey);
				byte[] first;

				first = Message.getEncoded(firstMsg);


				// Sends public key
				this.sendPacketBroad(first, ANDROID_PORT);
				System.out.println("Key sent!");
				System.out.println(pbkey);
				System.out.println(challengePC);

				// Listen for service port
				System.out.println("Waiting for mobile...");

				reqPacketPort = new DatagramPacket(buffer, buffer.length);
				broadcast_recv_send.receive(reqPacketPort);

				byte[] PortData = reqPacketPort.getData();
				Message PortMsg = Message.retriveMessage(PortData);

				byte[] DecPCcha = this.decript(PortMsg.get_pcChallange(), pvkey);
				byte[] DecMOBcha = this.decript(PortMsg.get_phoneChallange(), pvkey);
				
				BigInteger cha1 = new BigInteger(DecPCcha);
				System.out.println(cha1);
				challengeMOB = new BigInteger(DecMOBcha);
				System.out.println(challengeMOB);
				
				challengePC = challengePC.add(new BigInteger("1"));
				
				byte[] reqPort = PortMsg.get_Content();
				byte[] DECreqPort = this.decript(reqPort, pvkey);
				
				// Saves Request Port
				REQUEST_PORT = ByteBuffer.wrap(DECreqPort).getInt();
				System.out.println("Saved Request Port!");
				System.out.println(REQUEST_PORT);
				// Opens request socket
				this.request(REQUEST_PORT);

				
				//Second message
				challengePC = challengePC.add(new BigInteger("1"));
				System.out.println(challengePC);
				challengeMOB = challengeMOB.add(new BigInteger("1"));
				System.out.println(challengeMOB);

				
				byte[] encPC2 = this.encript(challengePC.toByteArray(), mbKey);
				byte[] encMOB2 = this.encript(challengeMOB.toByteArray(), mbKey);

				Message secondMsg = new Message(origin, destination, encPC2, encMOB2);
				firstMsg.set_Content(null);
				byte[] second;

				second = Message.getEncoded(secondMsg);


				// Sends public key
				this.sendPacketBroad(second, ANDROID_PORT);
				System.out.println("Key sent!");
				
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
				byte[] bufferDec = new byte[2048];

				// Request send
				byte[] in = this.Input();
				String inp = new String(in);

				if (inp.contentEquals("c")) {

					// DEMO - Creates byte[] from file encripted with mobile
					// public key
					Path path = Paths.get("C:/test.txt");
					byte[] file = Files.readAllBytes(path);
					byte[] enc = this.encript(file, mbKey);

					//Challenge Inc
					challengePC = challengePC.add(new BigInteger("1"));
					System.out.println(challengePC);
					challengeMOB = challengeMOB.add(new BigInteger("1"));
					System.out.println(challengeMOB);

					
					byte[] encPC2 = this.encript(challengePC.toByteArray(), mbKey);
					byte[] encMOB2 = this.encript(challengeMOB.toByteArray(), mbKey);
					
					// Send encripted message to cypher
					Message EncMsg = new Message(origin, destination, encPC2, encMOB2);
					EncMsg.set_Content(enc);
					EncMsg.set_Type(Message.REQUEST.CIPHER);
					byte[] MsgtoSend;

					MsgtoSend = Message.getEncoded(EncMsg);


					this.sendPacket(MsgtoSend, REQUEST_PORT);

					// Receive ciphered file
					DatagramPacket cipheredMsg = new DatagramPacket(buffer2, buffer2.length);
					requester.receive(cipheredMsg);
					byte[] cipMsg = cipheredMsg.getData();
					
					Message ciphMsg = Message.retriveMessage(cipMsg);

					byte[] DecPCcha = this.decript(ciphMsg.get_pcChallange(), pvkey);
					byte[] DecMOBcha = this.decript(ciphMsg.get_phoneChallange(), pvkey);
					
					challengePC = challengePC.add(new BigInteger("1"));
					System.out.println(challengePC);
					challengeMOB = challengeMOB.add(new BigInteger("1"));
					System.out.println(challengeMOB);
					
					if(challengePC.equals(new BigInteger(DecPCcha))){
						System.out.println("Challenge PC OK!");
					}
					
					if(challengeMOB.equals(new BigInteger(DecMOBcha))){
						System.out.println("Challenge MOB OK!");
					}
					
					
					
					byte[] ciphContent = ciphMsg.get_Content();

					// Saves ciphered message to file
					FileOutputStream outputStream = new FileOutputStream("C:/cyphtest.txt");
					// FileOutputStream outputStream = new
					// FileOutputStream("C:/Users/Andre/Documents/cyphtest.txt");
					outputStream.write(ciphContent);
					outputStream.close();

					// Prints ciphered message
					String cipmsg = new String(ciphContent, 0, ciphContent.length);
					System.out.println("Cyphered: " + cipmsg);

					
					
					// Decipher DEMO

					// Read bytes from encrypted file to byte[]
					Path pathDec = Paths.get("C:/cyphtest.txt");
					// Path pathDec =
					// Paths.get("C:/Users/Andre/Documents/cyphtest.txt");
					byte[] fileDec = Files.readAllBytes(pathDec);

					//Challenge Inc
					challengePC = challengePC.add(new BigInteger("1"));
					System.out.println(challengePC);
					challengeMOB = challengeMOB.add(new BigInteger("1"));
					System.out.println(challengeMOB);

					
					byte[] encPC3 = this.encript(challengePC.toByteArray(), mbKey);
					byte[] encMOB3 = this.encript(challengeMOB.toByteArray(), mbKey);
					
					// Send encripted message to decypher
					Message EncMsgDec = new Message(origin, destination, encPC3, encMOB3);
					EncMsgDec.set_Content(fileDec);
					EncMsgDec.set_Type(Message.REQUEST.DICIPHER);
					byte[] MsgtoDec;

					MsgtoDec = Message.getEncoded(EncMsgDec);

					this.sendPacket(MsgtoDec, REQUEST_PORT);

					// Receive deciphered file
					DatagramPacket DecipheredMsg = new DatagramPacket(bufferDec, bufferDec.length);
					requester.receive(DecipheredMsg);
					byte[] DecipMsg = DecipheredMsg.getData();
					
					Message DeciphMsg = Message.retriveMessage(DecipMsg);

					byte[] DecPCcha2 = this.decript(ciphMsg.get_pcChallange(), pvkey);
					byte[] DecMOBcha2 = this.decript(ciphMsg.get_phoneChallange(), pvkey);
					
					challengePC = challengePC.add(new BigInteger("1"));
					System.out.println(challengePC);
					challengeMOB = challengeMOB.add(new BigInteger("1"));
					System.out.println(challengeMOB);
					
					if(challengePC.equals(new BigInteger(DecPCcha2))){
						System.out.println("Challenge PC OK!");
					}
					
					if(challengeMOB.equals(new BigInteger(DecMOBcha2))){
						System.out.println("Challenge MOB OK!");
					}
					
					
					
					byte[] DeciphContent = DeciphMsg.get_Content();
					byte[] Content = this.decript(DeciphContent, pvkey);


					// Saves ciphered message to file
					FileOutputStream outputStreamDec = new FileOutputStream("C:/dectest.txt");
					// FileOutputStream outputStream = new
					// FileOutputStream("C:/Users/Andre/Documents/cyphtest.txt");
					outputStreamDec.write(Content);
					outputStreamDec.close();

					// Prints ciphered message
					String Decipmsg = new String(Content, 0, Content.length);
					System.out.println("Decyphered: " + Decipmsg);
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
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
