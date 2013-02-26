/* Driver program for FileSharing File Server */

public class RunFileServer {
	
	public static void main(String[] args) {
		if (args.length > 0) {
			try {
				FileServer server = new FileServer(args[0], Integer.parseInt(args[1]));
				server.start();
			}
			catch (NumberFormatException e) {
				System.out.printf("Enter a valid port number or pass no arguments to use the default port (%d)\n", FileServer.SERVER_PORT);
			}
		}
		else {
			FileServer server = new FileServer();
			server.start();
		}
	}

}
