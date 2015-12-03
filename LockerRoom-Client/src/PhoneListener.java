import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class PhoneListener implements Runnable {
	private static final int RECV_PORT = 8888;
	private int ANDROID_PORT;
	private int REQUEST_PORT;
	private InetAddress SEND_ADD;
	private KeyPair key;
	private DatagramSocket broadcast_recv_send;
	private DatagramSocket requester;
	private DatagramPacket recvPacket;
	private DatagramPacket sendPacket;
	private DatagramPacket reqPacket;
	private DatagramPacket keyPacket;
	private DatagramPacket reqPacket2;
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

	private void getMobileKey() {
		// Saves Mobile Public Key

		// mobile_key = new byte[reqPacket.getLength()-6];
		// mobile_key = (new String(buffer2, 0,
		// reqPacket2.getLength())).getBytes();

		// byte[] b = Base64.decode(mobile_key, Base64.DEFAULT);

		/*
		 * KeyFactory keyfactory = KeyFactory.getInstance("RSA");
		 * //X509EncodedKeySpec publicKeySpec = new
		 * X509EncodedKeySpec(mobile_key); PKCS8EncodedKeySpec publickk = new
		 * PKCS8EncodedKeySpec(mobile_key);
		 * keyfactory.generatePrivate(publickk);
		 */
		// PublicKey mobile =
		// KeyFactory.getInstance("RSA").generatePublic(new
		// X509EncodedKeySpec(mobile_key));

	}

	private KeyPair generateKeys() throws NoSuchAlgorithmException, IOException {
		KeyPair pair = Cypher.generateKey();

		return pair;
	}

	private byte[] pbkeyEncoded(KeyPair key) {
		PublicKey pub = key.getPublic();
		byte[] pubk = pub.getEncoded();

		return pubk;
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

		while (!done) {
			try {

				buffer = new byte[2042];
				bufferAux = new byte[2042];

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

				reqPacket = new DatagramPacket(buffer, buffer.length);
				reqPacket2 = new DatagramPacket(bufferAux, buffer.length);
				broadcast_recv_send.receive(reqPacket);
				broadcast_recv_send.receive(reqPacket2);
				req_message = new String(buffer, 0, reqPacket.getLength());
				System.out.println(req_message);

				// Saves Request Port
				REQUEST_PORT = Integer.parseInt(req_message);
				System.out.println("Saved Request Port!");

				this.request(REQUEST_PORT);
				broadcast_recv_send.close();
				System.out.println("Sending PC public key...");

				// Generates keypair
				key = this.generateKeys();

				// Retrieves public key encoded
				pbkey = this.pbkeyEncoded(key);

				// Sends public key
				keyPacket = new DatagramPacket(pbkey, pbkey.length, SEND_ADD, REQUEST_PORT);
				requester.connect(SEND_ADD, REQUEST_PORT);
				requester.send(keyPacket);
				requester.disconnect();

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
			}
		}
	}

	public void transfer() {

		try {
			while (true) {

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
				
				//TO DO: Test file transfer
				/*BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("Insert File Name:");
				String filename = br.readLine().trim();
				byte[] file = this.sendFile(filename);;*/
				
				//DEMO
				byte[] file = ("gatos").getBytes();
				DatagramPacket fil = new DatagramPacket(file, file.length, SEND_ADD, REQUEST_PORT);
				requester.connect(SEND_ADD, REQUEST_PORT);
				requester.send(fil);
				requester.disconnect();

				// Receive deciphered file
				DatagramPacket reqPacket3 = new DatagramPacket(buffer, buffer.length);
				requester.receive(reqPacket3);
				String req_message3 = new String(buffer, 0, reqPacket3.getLength());
				System.out.println(req_message3);
			}

		} catch (SocketException e) {

			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}