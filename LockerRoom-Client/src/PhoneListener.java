import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class PhoneListener implements Runnable {
	private static final int RECV_PORT = 8888;
	private int ANDROID_PORT;
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
	private DatagramPacket keyPacket;
	private DatagramPacket reqPacketKey;
	private byte[] buffer;


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

	private PublicKey getKey(byte[] key) {
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
	
	private byte[] encript(String message, PublicKey key) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException{
		byte[] msg = Cypher.encript(message, key);
		
		return msg;
	}
	

	public byte[] sendFile(String filename) throws IOException {
		File file = new File("C:/Users/Andre/Documents/" + filename + ".txt");

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

				buffer = new byte[2042];
				bufferAux = new byte[2042];
				
				// Generates keypair
				key = this.generateKeys();

				recvPacket = new DatagramPacket(buffer, buffer.length);

				System.out.println("Waiting for mobile...");

				broadcast_recv_send.receive(recvPacket);
				recv_message = new String(buffer, 0, recvPacket.getLength());
				parts = recv_message.split("\\|");

				ANDROID_PORT = Integer.valueOf(parts[1]);
				SEND_ADD = recvPacket.getAddress();

				// Send computer info
				System.out.println("Sending info...");

				send_message = InetAddress.getLocalHost().getHostName() + "|"
						+ InetAddress.getLocalHost().getHostAddress() + "|" + "8887";
				sendPacket = new DatagramPacket(send_message.getBytes(), send_message.getBytes().length, SEND_ADD,
						ANDROID_PORT);
				broadcast_recv_send.connect(SEND_ADD, ANDROID_PORT);
				broadcast_recv_send.send(sendPacket);
				broadcast_recv_send.disconnect();

				System.out.println("Info sent!");

				// Listen for service port
				System.out.println("Waiting for mobile...");

				reqPacketPort = new DatagramPacket(buffer, buffer.length);
				reqPacketKey = new DatagramPacket(bufferAux, buffer.length);

				broadcast_recv_send.receive(reqPacketPort);
				broadcast_recv_send.receive(reqPacketKey);
				
				req_message = new String(buffer, 0, reqPacketPort.getLength());
				
				// Saves Request Port
				REQUEST_PORT = Integer.parseInt(req_message);
				System.out.println("Saved Request Port!");
				
				// Generates Mobile pbKey
				byte[] data = reqPacketKey.getData();
				
				mbKey = this.getKey(data);
				
				// Saves Mobile Public Key
				System.out.println("Mobile Key Saved!");

				//Opens request socket, closes broadcast socket
				this.request(REQUEST_PORT);
				
				System.out.println("Sending PC public key...");
				
				// Retrieves public key encoded
				pbkey = this.pbkeyEncoded(key);
				
				System.out.println(pub);

				// Sends public key
				System.out.println(ANDROID_PORT);

				keyPacket = new DatagramPacket(pbkey, pbkey.length, SEND_ADD, ANDROID_PORT);
				broadcast_recv_send.connect(SEND_ADD, ANDROID_PORT);
				broadcast_recv_send.send(keyPacket);
				broadcast_recv_send.disconnect();
				broadcast_recv_send.close();

				this.transfer();
				done = true;

				System.out.println("Key sent!");

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
			}
		}
	}

	public void transfer() {

		try {
			while (true) {
				
				byte[] buffer2 = new byte[2024];

				// Request send
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("Enter c to Encrypt or d to Decrypt");
				String type = br.readLine().trim();
				byte[] req = type.getBytes();
				DatagramPacket req_type = new DatagramPacket(req, req.length, SEND_ADD, REQUEST_PORT);
				requester.connect(SEND_ADD, REQUEST_PORT);
				requester.send(req_type);
				requester.disconnect();

				// Send File to cipher

				// TO DO: Test file transfer
				/*
				 * BufferedReader read = new BufferedReader(new
				 * InputStreamReader(System.in)); System.out.println(
				 * "Insert File Name:"); String filename = br.readLine().trim();
				 * byte[] file = this.sendFile(filename);;
				 */

				// DEMO
				byte[] file = ("gatos fazem miau").getBytes();
				
				//Not Tested
				byte[] encrypt = this.encript("gatos fazem miau", mbKey);
				//System.out.println(encrypt);

				
				DatagramPacket fil = new DatagramPacket(file, file.length, SEND_ADD, REQUEST_PORT);
				requester.connect(SEND_ADD, REQUEST_PORT);
				requester.send(fil);
				requester.disconnect();

				// Receive deciphered file
				DatagramPacket reqPacket3 = new DatagramPacket(buffer, buffer.length);
				byte[] req3 = reqPacket3.getData();
				requester.receive(reqPacket3);
				
				
				/*FileWriter fw = new FileWriter(new File("C:/Users/Andre/Documents/teste2.txt"));
			    byte[] receiveData = new byte[reqPacket3.getLength()];    
			    while (receiveData != null) {
	                fw.write(req_message3);
	                fw.flush();
	            }
	            fw.flush();
	            fw.close();*/
				
				byte[] req2 = ("d").getBytes();
				DatagramPacket req_type2 = new DatagramPacket(req2, req2.length, SEND_ADD, REQUEST_PORT);
				requester.connect(SEND_ADD, REQUEST_PORT);
				requester.send(req_type2);
				requester.disconnect();
				
				System.out.println(req3);
				DatagramPacket file2 = new DatagramPacket(req3, req3.length, SEND_ADD, REQUEST_PORT);
				requester.connect(SEND_ADD, REQUEST_PORT);
				requester.send(file2);
				requester.disconnect();
				
				// Receive deciphered file
				DatagramPacket reqPacket4 = new DatagramPacket(buffer2, buffer2.length);
				requester.receive(reqPacket3);
				String req_message4 = new String(buffer, 0, reqPacket3.getLength());
				System.out.println(req_message4);
				
			}

		} catch (SocketException e) {

			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
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
