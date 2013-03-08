import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException; 
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Random;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public abstract class Client {

	/* protected keyword is like private but subclasses have access
	 * Socket and input/output streams
	 */
	protected Socket sock;
	protected ObjectOutputStream output;
	protected ObjectInputStream input;
	protected PublicKey serverKey;
	private SecretKey sessionKey;
	private Random rand;

	//set up keys and random number generator
	Client() throws NoSuchAlgorithmException{
		rand = new Random();
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		//128 bit key length
		keyGen.init(128);
		sessionKey = keyGen.generateKey();
	}

	public boolean connect(final String server, final int port) {
		System.out.println("attempting to connect");

		try {						
			sock = new Socket(server, port);

			output = new ObjectOutputStream(sock.getOutputStream());
			input = new ObjectInputStream(sock.getInputStream());

			Envelope e = new Envelope("CONNECT");
			output.writeObject(e);

			e = (Envelope)input.readObject();
			if(e.getMessage().equals("PUBLICKEY"))
				serverKey = (PublicKey) e.getObjContents().get(0);
			byte[] value = new byte[4];
			rand.nextBytes(value);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
			outputStream.write(value);
			outputStream.write(sessionKey.getEncoded());

			byte[] message = outputStream.toByteArray();

			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, serverKey);
			byte[] encrypted = cipher.doFinal(message);

			e = new Envelope("SESSIONKEY");
			e.addObject(encrypted);
			output.writeObject(e);

			e = (Envelope)input.readObject();

			if(e.getMessage().equals("AUTHVALUE"))
			{
				if(Arrays.equals(((byte[])e.getObjContents().get(0)), value))
					return sock.isConnected();
				else
					return false;
			}
		} catch (UnknownHostException e) {
			System.out.println("host was not found");
		} catch (IOException e) {
			System.out.println("an IO error occured");
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		return false;
	}

	public boolean isConnected() {
		return (sock != null && sock.isConnected());
	}

	public void disconnect()	 {
		if (isConnected()) {
			try
			{
				Envelope message = new Envelope("DISCONNECT");
				output.writeObject(message);
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
			}
		}
	}
}
