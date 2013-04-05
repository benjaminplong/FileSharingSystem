/* File worker thread handles the business of uploading, downloading, and removing files 
 * for clients with valid tokens
 */

import java.lang.Thread;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class FileThread extends Thread
{
	private final Socket socket;
	private FileServer my_fs;
	private SecretKey sessionKey;
	private PublicKey groupKey;
	private Random rand;

	public FileThread(Socket _socket, FileServer fs)
	{
		socket = _socket;
		my_fs = fs;

		rand = new Random(System.currentTimeMillis());
	}

	public void run()
	{
		boolean proceed = true;
		try
		{
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			Envelope response;

			Cipher rsaCipher = Cipher.getInstance("RSA");
			Cipher aesCipher = Cipher.getInstance("AES");
			Mac hmac = Mac.getInstance("HmacSHA1");
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			byte[] decrypted;
			byte[] encrypted;
			int msgIndex = 0;

			do
			{				
				Envelope e = (Envelope)input.readObject();
				System.out.println("Request received: " + e.getMessage());

				if(e.getMessage().equals("CONNECT"))//Client wants a token
				{
					// alert the user of the file servers public key on connect
					response = new Envelope("PUBLICKEY");
					response.addObject(my_fs.RSAkeys.getPublic());
					output.writeObject(response);
				}
				else if (e.getMessage().equals("SHAREDKEYS"))
				{
					// return the random number to the client
					response = new Envelope("AUTHVALUE");

					// initialize your cipher
					rsaCipher.init(Cipher.DECRYPT_MODE, my_fs.RSAkeys.getPrivate());

					decrypted = rsaCipher.doFinal((byte[]) e.getObjContents().get(1));
					sessionKey = new SecretKeySpec(decrypted, "AES");

					// decrypt the random number from the client
					aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey);
					decrypted = rsaCipher.doFinal((byte[]) e.getObjContents().get(0));
					response.addObject(aesCipher.doFinal(decrypted));

					byte[] value = new byte[4];
					rand.nextBytes(value);

					// send a new random number
					response.addObject(aesCipher.doFinal(value));

					output.writeObject(response);

					// get the hash key value for HMAC
					decrypted = rsaCipher.doFinal((byte[]) e.getObjContents().get(2));
					SecretKey hashKey = new SecretKeySpec(decrypted, "HmacSHA1");
					hmac.init(hashKey);

					e = (Envelope)input.readObject();

					if(e.getMessage().equals("AUTHVALUE"))
					{
						aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);
						decrypted = aesCipher.doFinal((byte[])e.getObjContents().get(0));
						// Close the connection if the numbers don't match
						if(!Arrays.equals(decrypted, value))
						{
							socket.close();
							proceed = false;
						}

						response = new Envelope("OK");
						output.writeObject(response);
					}
				}
				else if(e.getMessage().equals("GROUPKEY"))
				{
					aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);
					decrypted = aesCipher.doFinal((byte[]) e.getObjContents().get(0));

					groupKey = keyFactory.generatePublic(new X509EncodedKeySpec(decrypted));

					response = new Envelope("OK");
					output.writeObject(response);
				}
				// Handler to list files that this user is allowed to see
				else if(e.getMessage().equals("LFILES"))
				{					
					aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);
					decrypted = aesCipher.doFinal((byte[]) e.getObjContents().get(0));

					response = new Envelope("FAIL");

					if (Arrays.equals(hmac.doFinal(decrypted), e.getChecksum()))
					{
						response = new Envelope("OK");

						rsaCipher.init(Cipher.DECRYPT_MODE, groupKey);
						decrypted = rsaCipher.doFinal(decrypted);

						String tokenParts = new String(decrypted);
						UserToken t = new Token(tokenParts); //Extract the token

						ArrayList<String> groups = new ArrayList<String>(t.getGroups());

						aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey);

						for (ShareFile f : FileServer.fileList.getFiles())
						{
							for (String g : groups)
							{
								if (f.getGroup().equals(g))
								{
									hmac.update(f.getPath().getBytes());
									encrypted = aesCipher.doFinal(f.getPath().getBytes());
									response.addObject(encrypted);
								}
							}
						}
					}

					response.setChecksum(hmac.doFinal());
					output.writeObject(response);
				}
				else if(e.getMessage().equals("UPLOADF"))
				{

					if(e.getObjContents().size() < 3)
					{
						response = new Envelope("FAIL-BADCONTENTS");
					}
					else
					{
						if(e.getObjContents().get(0) == null) {
							response = new Envelope("FAIL-BADPATH");
						}
						if(e.getObjContents().get(1) == null) {
							response = new Envelope("FAIL-BADGROUP");
						}
						if(e.getObjContents().get(2) == null) {
							response = new Envelope("FAIL-BADTOKEN");
						}
						else {

							aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);

							decrypted = aesCipher.doFinal((byte[]) e.getObjContents().get(0));
							String remotePath = new String(decrypted); // Extract the remotePath
							hmac.update(decrypted);

							decrypted = aesCipher.doFinal((byte[]) e.getObjContents().get(1));
							String group = new String(decrypted); // Extract the group
							hmac.update(decrypted);

							decrypted = aesCipher.doFinal((byte[]) e.getObjContents().get(2));
							hmac.update(decrypted);
							rsaCipher.init(Cipher.DECRYPT_MODE, groupKey);
							decrypted = rsaCipher.doFinal(decrypted);

							String tokenParts = new String(decrypted);
							UserToken yourToken = new Token(tokenParts); //Extract the token

							response = new Envelope("FAIL");

							if (Arrays.equals(e.getChecksum(), hmac.doFinal()))
							{
								if (FileServer.fileList.checkFile(remotePath)) {
									System.out.printf("Error: file already exists at %s\n", remotePath);
									response = new Envelope("FAIL-FILEEXISTS");
								}
								else if (!yourToken.getGroups().contains(group)) {
									System.out.printf("Error: user missing valid token for group %s\n", group);
									response = new Envelope("FAIL-UNAUTHORIZED");
								}
								else  {
									File file = new File("shared_files/"+remotePath.replace('/', '_'));
									file.createNewFile();
									FileOutputStream fos = new FileOutputStream(file);
									System.out.printf("Successfully created file %s\n", remotePath.replace('/', '_'));

									response = new Envelope("READY"); //Success
									output.writeObject(response);

									e = (Envelope)input.readObject();
									while (e.getMessage().compareTo("CHUNK")==0) {
										decrypted = aesCipher.doFinal((byte[]) e.getObjContents().get(0));

										if (Arrays.equals(e.getChecksum(), hmac.doFinal(decrypted)))
										{
											fos.write(decrypted, 0, (Integer)e.getObjContents().get(1));
											response = new Envelope("READY"); //Success
											output.writeObject(response);
											e = (Envelope)input.readObject();
										}
										else
											break;
									}

									if(e.getMessage().compareTo("EOF")==0) {
										System.out.printf("Transfer successful file %s\n", remotePath);
										FileServer.fileList.addFile(yourToken.getSubject(), group, remotePath);
										response = new Envelope("OK"); //Success
									}
									else {
										System.out.printf("Error reading file %s from client\n", remotePath);
										response = new Envelope("ERROR-TRANSFER"); //Success
									}
									fos.close();
								}
							}
						}
					}

					output.writeObject(response);
				}
				else if (e.getMessage().compareTo("DOWNLOADF")==0) {

					aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);

					decrypted = aesCipher.doFinal((byte[]) e.getObjContents().get(0));
					String remotePath = new String(decrypted); // Extract the remotePath
					hmac.update(decrypted);

					decrypted = aesCipher.doFinal((byte[]) e.getObjContents().get(1));
					hmac.update(decrypted);
					rsaCipher.init(Cipher.DECRYPT_MODE, groupKey);
					decrypted = rsaCipher.doFinal(decrypted);

					String tokenParts = new String(decrypted);
					UserToken t = new Token(tokenParts); //Extract the token

					ShareFile sf = FileServer.fileList.getFile("/"+remotePath);
					if (sf == null) {
						System.out.printf("Error: File %s doesn't exist\n", remotePath);
						e = new Envelope("ERROR_FILEMISSING");
						output.writeObject(e);

					}
					else if (!t.getGroups().contains(sf.getGroup())){
						System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
						e = new Envelope("ERROR_PERMISSION");
						output.writeObject(e);
					}
					else {

						try
						{
							if (Arrays.equals(e.getChecksum(), hmac.doFinal()))
							{
								File f = new File("shared_files/_"+remotePath.replace('/', '_'));
								if (!f.exists()) {
									System.out.printf("Error file %s missing from disk\n", "_"+remotePath.replace('/', '_'));
									e = new Envelope("ERROR_NOTONDISK");
									output.writeObject(e);

								}
								else {
									FileInputStream fis = new FileInputStream(f);

									aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey);

									do {
										byte[] buf = new byte[4096];
										if (e.getMessage().compareTo("DOWNLOADF")!=0) {
											System.out.printf("Server error: %s\n", e.getMessage());
											break;
										}
										e = new Envelope("CHUNK");
										int n = fis.read(buf); //can throw an IOException
										if (n > 0) {
											System.out.printf(".");
										} else if (n < 0) {
											System.out.println("Read error");

										}

										e.setChecksum(hmac.doFinal(buf));
										encrypted = aesCipher.doFinal(buf);

										e.addObject(encrypted);
										e.addObject(new Integer(n));

										output.writeObject(e);

										e = (Envelope)input.readObject();
									} while (fis.available() > 0);

									fis.close();

									if(e.getMessage().compareTo("DOWNLOADF")==0)
									{

										e = new Envelope("EOF");
										output.writeObject(e);

										e = (Envelope)input.readObject();
										if(e.getMessage().compareTo("OK")==0) {
											System.out.printf("File data upload successful\n");
										}
										else {

											System.out.printf("Upload failed: %s\n", e.getMessage());

										}

									}
									else {

										System.out.printf("Upload failed: %s\n", e.getMessage());

									}
								}
							}
						}
						catch(Exception e1)
						{
							System.err.println("Error: " + e.getMessage());
							e1.printStackTrace(System.err);

						}
					}
				}
				else if (e.getMessage().compareTo("DELETEF")==0) {

					aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);

					decrypted = aesCipher.doFinal((byte[]) e.getObjContents().get(0));
					String remotePath = new String(decrypted); // Extract the remotePath
					hmac.update(decrypted);

					decrypted = aesCipher.doFinal((byte[]) e.getObjContents().get(1));
					hmac.update(decrypted);
					rsaCipher.init(Cipher.DECRYPT_MODE, groupKey);
					decrypted = rsaCipher.doFinal(decrypted);

					String tokenParts = new String(decrypted);
					UserToken t = new Token(tokenParts); //Extract the token

					if (Arrays.equals(e.getChecksum(), hmac.doFinal()))
					{
						ShareFile sf = FileServer.fileList.getFile("/"+remotePath);
						if (sf == null) {
							System.out.printf("Error: File %s doesn't exist\n", remotePath);
							e = new Envelope("ERROR_DOESNTEXIST");
						}
						else if (!t.getGroups().contains(sf.getGroup())){
							System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
							e = new Envelope("ERROR_PERMISSION");
						}
						else {

							try
							{
								File f = new File("shared_files/"+"_"+remotePath.replace('/', '_'));

								if (!f.exists()) {
									System.out.printf("Error file %s missing from disk\n", "_"+remotePath.replace('/', '_'));
									e = new Envelope("ERROR_FILEMISSING");
								}
								else if (f.delete()) {
									System.out.printf("File %s deleted from disk\n", "_"+remotePath.replace('/', '_'));
									FileServer.fileList.removeFile("/"+remotePath);
									e = new Envelope("OK");
								}
								else {
									System.out.printf("Error deleting file %s from disk\n", "_"+remotePath.replace('/', '_'));
									e = new Envelope("ERROR_DELETE");
								}


							}
							catch(Exception e1)
							{
								System.err.println("Error: " + e1.getMessage());
								e1.printStackTrace(System.err);
								e = new Envelope(e1.getMessage());
							}
						}
						output.writeObject(e);
					}

				}
				else if(e.getMessage().equals("DISCONNECT"))
				{
					socket.close();
					proceed = false;
				}
			} while(proceed);
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}

}
