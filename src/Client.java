import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.Key;
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
import javax.crypto.Mac;
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
	protected SecretKey hashKey;
	private Random rand;
	private Cipher aesCipher;
	protected Mac hmac;
	
	//set up keys and random number generator
	Client()
	{
		rand = new Random(System.currentTimeMillis());
		KeyGenerator keyGen = null;
		try {
			keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(128);
			sessionKey = new SecretKeySpec(keyGen.generateKey().getEncoded(), "AES");
			
			keyGen = KeyGenerator.getInstance("HmacSHA1");
			keyGen.init(128);
			hashKey = new SecretKeySpec(keyGen.generateKey().getEncoded(), "HmacSHA1");
			
			aesCipher = Cipher.getInstance("AES");
			hmac = Mac.getInstance("HmacSHA1");
			hmac.init(hashKey);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
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
			
			// Get the Servers public key
			e = (Envelope)input.readObject();
			if(e.getMessage().equals("PUBLICKEY"))
				serverKey = (PublicKey) e.getObjContents().get(0);
			
			byte[] value = new byte[4];
			rand.nextBytes(value);
			
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, serverKey);
			
			// Send our random number with the session key and hash key encrypted with the servers public key
			e = new Envelope("SHAREDKEYS");
			e.addObject(cipher.doFinal(value));
			e.addObject(cipher.doFinal(sessionKey.getEncoded()));
			e.addObject(cipher.doFinal(hashKey.getEncoded()));
			output.writeObject(e);
			
			e = (Envelope)input.readObject();
			
			if(e.getMessage().equals("AUTHVALUE"))
			{
				// Make sure the numbers match
				if(Arrays.equals(decryptAES((byte[])e.getObjContents().get(0)), value))
				{
					// send back the servers random number generated
					byte[] decrypted = decryptAES((byte[])e.getObjContents().get(1));
					
					Envelope response = new Envelope("AUTHVALUE");
					response.addObject(encryptAES(decrypted));
					output.writeObject(response);
					
					e = (Envelope)input.readObject();
					
					if (e.getMessage().equals("OK"))
						return sock.isConnected();
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
	
	protected byte[] encryptAES(byte[] message) {
		try {
			aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey);
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
	
	protected byte[] decryptAES(byte[] message) {
		try {
			aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);
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
