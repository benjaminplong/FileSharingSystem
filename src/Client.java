import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException; 
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Random;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public abstract class Client {

	/* protected keyword is like private but subclasses have access
	 * Socket and input/output streams
	 */
	protected Socket sock;
	protected ObjectOutputStream output;
	protected ObjectInputStream input;
	protected PublicKey serverKey;
	protected SecretKey sessionKey;
	protected SecretKeySpec sessionKeySpec;
	private Random rand;
	private Cipher aesCipher;
	
	//set up keys and random number generator
	Client() {
		rand = new Random();
		KeyGenerator keyGen = null;
		try {
			keyGen = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//128 bit key length
		keyGen.init(128);
		sessionKey = keyGen.generateKey();
		sessionKeySpec = new SecretKeySpec(sessionKey.getEncoded(),"AES");
		try {
			aesCipher = Cipher.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
			{
				serverKey = (PublicKey) e.getObjContents().get(0);
			}
			byte[] value = new byte[4];
			rand.nextBytes(value);
			
			
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, serverKey);
			
			e = new Envelope("SESSIONKEY");
			e.addObject(cipher.doFinal(value));
			e.addObject(cipher.doFinal(sessionKey.getEncoded()));
			output.writeObject(e);
			
			e = (Envelope)input.readObject();
			
			if(e.getMessage().equals("AUTHVALUE")){
				if(Arrays.equals((byte[])e.getObjContents().get(0), value)){
					return sock.isConnected();
				}
				else{
					return false;
				}
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
	
	public byte[] encryptAES(byte[] message) {
		try {
			aesCipher.init(Cipher.ENCRYPT_MODE, sessionKeySpec);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			return aesCipher.doFinal(message);
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public byte[] decryptAES(byte[] message){
		try {
			aesCipher.init(Cipher.DECRYPT_MODE, sessionKeySpec);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			return aesCipher.doFinal(message);
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
}
