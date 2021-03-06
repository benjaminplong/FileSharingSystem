/* FileServer loads files from FileList.bin.  Stores files in shared_files directory. */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer extends Server {

	protected static final String fileFile = "FileList.bin";

	public static final int SERVER_PORT = 4321;
	public static FileList fileList;

	public FileServer() {
		super(SERVER_PORT, "FilePile");
	}

	public FileServer(String _server, int _port) {
		super(_port, _server);
	}

	public void start() {
		ObjectInputStream fileStream;

		//This runs a thread that saves the lists on program exit
		Runtime runtime = Runtime.getRuntime();
		Thread catchExit = new Thread(new ShutDownListenerFS());
		runtime.addShutdownHook(catchExit);

		//Open user file to get user list
		try
		{
			FileInputStream fis = new FileInputStream(fileFile);
			fileStream = new ObjectInputStream(fis);
			fileList = (FileList)fileStream.readObject();
		}
		catch(FileNotFoundException e)
		{
			System.out.println("FileList Does Not Exist. Creating FileList...");

			fileList = new FileList();

		}
		catch(IOException e)
		{
			System.out.println("Error reading from FileList file");
			System.exit(-1);
		}
		catch(ClassNotFoundException e)
		{
			System.out.println("Error reading from FileList file");
			System.exit(-1);
		}

		File file = new File("shared_files");
		if (file.mkdir()) {
			System.out.println("Created new shared_files directory");
		}
		else if (file.exists()){
			System.out.println("Found shared_files directory");
		}
		else {
			System.out.println("Error creating shared_files directory");				 
		}

		//Autosave Daemon. Saves lists every 5 minutes
		AutoSaveFS aSave = new AutoSaveFS();
		aSave.setDaemon(true);
		aSave.start();

		boolean running = true;

		try
		{			
			final ServerSocket serverSock = new ServerSocket(port);
			
			createRSAKeyPair();
			
			System.out.printf("%s up and running\n", this.getClass().getName());

			Socket sock = null;
			Thread thread = null;

			while(running)
			{
				sock = serverSock.accept();
				// initialize thread with our RSA keys and the trusted server
				thread = new FileThread(sock, this);
				thread.start();
			}

			System.out.printf("%s shut down\n", this.getClass().getName());
			serverSock.close();
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}

	//This thread saves user and group lists
	class ShutDownListenerFS implements Runnable
	{
		public void run()
		{
			System.out.println("Shutting down server");
			ObjectOutputStream outStream;

			try
			{
				outStream = new ObjectOutputStream(new FileOutputStream(fileFile));
				outStream.writeObject(FileServer.fileList);
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
			}
		}
	}

	class AutoSaveFS extends Thread
	{
		public void run()
		{
			do
			{
				try
				{
					Thread.sleep(300000); //Save group and user lists every 5 minutes
					System.out.println("Autosave file list...");
					ObjectOutputStream outStream;
					try
					{
						outStream = new ObjectOutputStream(new FileOutputStream(fileFile));
						outStream.writeObject(FileServer.fileList);
					}
					catch(Exception e)
					{
						System.err.println("Error: " + e.getMessage());
						e.printStackTrace(System.err);
					}

				}
				catch(Exception e)
				{
					System.out.println("Autosave Interrupted");
				}
			} while(true);
		}
	}
}


