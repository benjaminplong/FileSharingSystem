/* FileClient provides all the client functionality regarding the file server */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import com.sun.crypto.provider.AESCipher;

public class FileClient extends Client implements FileClientInterface {

	FileClient() {
		super();
	}

	public boolean delete(String filename, byte[] token) {
		String remotePath;
		if (filename.charAt(0) == '/') {
			remotePath = filename.substring(1);
		} else {
			remotePath = filename;
		}
		Envelope env = new Envelope("DELETEF"); // Success
		try {
			env.addObject(encryptAES(remotePath.getBytes()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			env.addObject(encryptAES(token));
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			output.writeObject(env);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			env = (Envelope) input.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		if (env.getMessage().compareTo("OK") == 0) {
			System.out.printf("File %s deleted successfully\n", filename);
		} else {
			System.out.printf("Error deleting file %s (%s)\n", filename,
					env.getMessage());
			return false;
		}

		return true;
	}

	public boolean download(String sourceFile, String destFile, byte[] token, ArrayList<Group> myGroups) {
		if (sourceFile.charAt(0) == '/') {
			sourceFile = sourceFile.substring(1);
		}

		File file = new File(destFile);
		try {

			if (!file.exists()) {
				file.createNewFile();
				FileOutputStream fos = new FileOutputStream(file);

				Envelope env = new Envelope("DOWNLOADF"); // Success
				env.addObject(encryptAES(sourceFile.getBytes()));
				env.addObject(encryptAES(token));
				output.writeObject(env);
				
				env = (Envelope) input.readObject();
				int keyIndex = ByteBuffer.wrap(decryptAES((byte[]) env.getObjContents().get(0))).getInt();
				String group = decryptAES((byte[]) env.getObjContents().get(1)).toString();
				
				env = new Envelope("DOWNLOADF");
				output.writeObject(env);
				
				Cipher fileCipher = null;
				try {
					fileCipher = Cipher.getInstance("AES");
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchPaddingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				SecretKey key = null;
				for(Group groupItem : myGroups){
					if(groupItem.name.equals(group)){
						key = groupItem.getKey(keyIndex);
					}
				}
				try {
					fileCipher.init(Cipher.DECRYPT_MODE, key);
				} catch (InvalidKeyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				env = (Envelope) input.readObject();

				while (env.getMessage().compareTo("CHUNK") == 0) {
					// TODO: do something about plain number passing
					try {
						fos.write(fileCipher.doFinal(decryptAES((byte[]) env.getObjContents().get(0)),
								0, (Integer) env.getObjContents().get(1)));
					} catch (IllegalBlockSizeException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (BadPaddingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.printf(".");
					env = new Envelope("DOWNLOADF"); // Success
					output.writeObject(env);
					env = (Envelope) input.readObject();
				}
				fos.close();

				if (env.getMessage().compareTo("EOF") == 0) {
					fos.close();
					System.out.printf("\nTransfer successful file %s\n",
							sourceFile);
					env = new Envelope("OK"); // Success
					output.writeObject(env);
				} else {
					System.out.printf("Error reading file %s (%s)\n",
							sourceFile, env.getMessage());
					file.delete();
					return false;
				}
			}

			else {
				System.out.printf("Error couldn't create file %s\n", destFile);
				return false;
			}

		} catch (IOException e1) {

			System.out.printf("Error couldn't create file %s\n", destFile);
			return false;

		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		return true;
	}

	public List<String> listFiles(byte[] token) {
		try {
			Envelope message = null, e = null;
			// Tell the server to return the member list
			message = new Envelope("LFILES");
			message.addObject(encryptAES(token)); // Add requester's token
			output.writeObject(message);

			e = (Envelope) input.readObject();

			ArrayList<String> files =  new ArrayList<String>();
			
			// If server indicates success, return the member list
			if (e.getMessage().equals("OK"))
			{
				for (Object o : e.getObjContents())
					files.add(new String(decryptAES((byte[])o)));
				
				return files;
			}

			return null;

		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

	public boolean upload(String sourceFile, String destFile, String group,
			byte[] token, ArrayList<Group> myGroups) {

		if (destFile.charAt(0) != '/') {
			destFile = "/" + destFile;
		}

		try {

			Envelope message = null, env = null;
			// Tell the server to return the member list
			message = new Envelope("UPLOADF");
			message.addObject(encryptAES(destFile.getBytes()));
			Cipher fileCipher = Cipher.getInstance("AES");
			SecretKey key = null;
			int keyIndex = 0;
			for(Group groupItem : myGroups){
				if(groupItem.name.equals(group)){
					keyIndex = groupItem.keys.size() - 1;
					key = groupItem.getKey(keyIndex);
				}
			}
			fileCipher.init(Cipher.ENCRYPT_MODE, key);
			message.addObject(encryptAES(group.getBytes()));
			message.addObject(encryptAES(token)); // Add requester's token
			ByteBuffer keyIndexBuf = ByteBuffer.allocate(4).putInt(keyIndex);
			message.addObject(encryptAES(keyIndexBuf.array()));
			output.writeObject(message);

			FileInputStream fis = new FileInputStream(sourceFile);

			env = (Envelope) input.readObject();

			// If server indicates success, return the member list
			if (env.getMessage().equals("READY")) {
				System.out.printf("Meta data upload successful\n");

			} else {

				System.out.printf("Upload failed: %s\n", env.getMessage());
				fis.close();
				return false;
			}

			do {
				byte[] buf = new byte[4096];
				if (env.getMessage().compareTo("READY") != 0) {
					System.out.printf("Server error: %s\n", env.getMessage());
					fis.close();
					return false;
				}
				message = new Envelope("CHUNK");
				int n = fis.read(buf); // can throw an IOException
				if (n > 0) {
					System.out.printf(".");
				} else if (n < 0) {
					System.out.println("Read error");
					fis.close();
					return false;
				}

				message.addObject(encryptAES(fileCipher.doFinal(buf)));
				// TODO: do something about plain number passing
				message.addObject(new Integer(n));

				output.writeObject(message);

				env = (Envelope) input.readObject();

			} while (fis.available() > 0);

			fis.close();

			// If server indicates success, return the member list
			if (env.getMessage().compareTo("READY") == 0) {

				message = new Envelope("EOF");
				output.writeObject(message);

				env = (Envelope) input.readObject();
				if (env.getMessage().compareTo("OK") == 0) {
					System.out.printf("\nFile data upload successful\n");
				} else {

					System.out
							.printf("\nUpload failed: %s\n", env.getMessage());
					return false;
				}

			} else {

				System.out.printf("Upload failed: %s\n", env.getMessage());
				return false;
			}

		} catch (Exception e1) {
			System.err.println("Error: " + e1.getMessage());
			e1.printStackTrace(System.err);
			return false;
		}
		return true;
	}

}
