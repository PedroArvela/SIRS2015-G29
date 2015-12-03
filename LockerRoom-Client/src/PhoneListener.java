import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class PhoneListener implements Runnable {
	private static final int RECV_PORT = 8888;
	private int ANDROID_PORT;
	private int REQUEST_PORT;
	private KeyPair key;
	private DatagramSocket broadcast_recv_send;
	private DatagramSocket requester;
	private ServerSocket cypher;
	private DatagramPacket recvPacket;
	private DatagramPacket sendPacket;
	private DatagramPacket reqPacket;
	private DatagramPacket keyPacket;
	private DatagramPacket reqPacket2;
	Socket sock = null;

	FileInputStream fis = null;
	BufferedInputStream bis = null;
	OutputStream os = null;

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

	private void cypher(int req_port) throws IOException {
		try {
			cypher = new ServerSocket(req_port);
		} catch (SocketException e) {
			// IGNORE
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		byte[] buffer;
		String recv_message;
		String send_message;
		String req_message;
		byte[] mobile_key;
		String[] parts;
		String[] portkey;
		byte[] buffer2;

		this.initialize();

		while (true) {
			try {

				buffer = new byte[2042];
				buffer2 = new byte[2042];
				recvPacket = new DatagramPacket(buffer, buffer.length);

				System.out.println("Waiting for mobile...");

				broadcast_recv_send.receive(recvPacket);
				recv_message = new String(buffer, 0, recvPacket.getLength());
				parts = recv_message.split("\\|");

				ANDROID_PORT = Integer.valueOf(parts[1]);
				InetAddress SEND_ADD = recvPacket.getAddress();

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
				reqPacket2 = new DatagramPacket(buffer2, buffer.length);
				broadcast_recv_send.receive(reqPacket);
				broadcast_recv_send.receive(reqPacket2);
				req_message = new String(buffer, 0, reqPacket.getLength());
				System.out.println(req_message);

				// portkey = req_message.split("\\|");

				// Saves Request Port
				REQUEST_PORT = Integer.parseInt(req_message);
				System.out.println("Saved Request Port!");

				// Saves Mobile Public Key

				// mobile_key = new byte[reqPacket.getLength()-6];
				// mobile_key = (new String(buffer2, 0,
				// reqPacket2.getLength())).getBytes();

				// byte[] b = Base64.decode(mobile_key, Base64.DEFAULT);

				/*
				 * KeyFactory keyfactory = KeyFactory.getInstance("RSA");
				 * //X509EncodedKeySpec publicKeySpec = new
				 * X509EncodedKeySpec(mobile_key); PKCS8EncodedKeySpec publickk
				 * = new PKCS8EncodedKeySpec(mobile_key);
				 * keyfactory.generatePrivate(publickk);
				 */
				// PublicKey mobile =
				// KeyFactory.getInstance("RSA").generatePublic(new
				// X509EncodedKeySpec(mobile_key));
				System.out.println("Saved mobile pbkey!");

				this.request(REQUEST_PORT);

				System.out.println("Sending PC public key...");

				// Generates keypair
				try {
					key = Cypher.generateKey();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// Retrieves public key and encodes
				PublicKey pub = key.getPublic();
				byte[] pubk = pub.getEncoded();
				System.out.println(pub);

				// Sends public key
				keyPacket = new DatagramPacket(pubk, pubk.length, SEND_ADD, REQUEST_PORT);
				requester.connect(SEND_ADD, REQUEST_PORT);
				requester.send(keyPacket);
				requester.disconnect();

				System.out.println("Key sent!");

				// Request send
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("Enter c to Encrypt or d to Decrypt");
				String s = br.readLine().trim();
				byte[] req = s.getBytes();
				DatagramPacket type = new DatagramPacket(req, req.length, SEND_ADD, REQUEST_PORT);
				requester.connect(SEND_ADD, REQUEST_PORT);
				requester.send(type);
				requester.disconnect();
				
				//Send File to cipher
				String FILE_TO_SEND = "C:/Users/Andre/Documents/test.txt";
				byte[] file = FILE_TO_SEND.getBytes();
				DatagramPacket fil = new DatagramPacket(file, file.length, SEND_ADD, REQUEST_PORT);
				requester.connect(SEND_ADD, REQUEST_PORT);
				requester.send(type);
				requester.disconnect();
				
				
				//Receive deciphered file
				DatagramPacket reqPacket3 = new DatagramPacket(buffer, buffer.length);
				requester.receive(reqPacket3);
				String req_message3 = new String(buffer, 0, reqPacket3.getLength());
				System.out.println(req_message3);
				
				 this.cypher(REQUEST_PORT);
				 sock = cypher.accept();
		         System.out.println("Accepted connection : " + sock);
		         
		         
				// send file
				try {
					String FILETOSEND = "C:/Users/Andre/Documents/test.txt";
					File myFile = new File(FILE_TO_SEND);

					byte[] mybytearray = new byte[(int) myFile.length()];
					fis = new FileInputStream(myFile);
					bis = new BufferedInputStream(fis);
					bis.read(mybytearray, 0, mybytearray.length);
					os = sock.getOutputStream();
					System.out.println("Sending " + FILE_TO_SEND + "(" + mybytearray.length + " bytes)");
					os.write(mybytearray, 0, mybytearray.length);
					os.flush();
					System.out.println("Done.");
				} finally {
					if (bis != null)
						bis.close();
					if (os != null)
						os.close();
					if (sock != null)
						sock.close();
				    if (cypher != null) cypher.close();

				}
				

			} catch (SocketException e) {

				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
