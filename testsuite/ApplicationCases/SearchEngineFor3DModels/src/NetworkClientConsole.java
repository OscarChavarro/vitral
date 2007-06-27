//===========================================================================
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.net.Socket;

public class NetworkClientConsole {
    public static void main(String[] args) throws Exception {

        Socket kkSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        String hostname = "localhost";

        try {
            kkSocket = new Socket(hostname, 1234);
            out = new PrintWriter(kkSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + hostname + ".");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + hostname + ".");
            System.exit(1);
        }

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String fromServer;
        String fromUser;

        while ( (fromServer = in.readLine()) != null ) {
            System.out.println("Server: " + fromServer);
            if ( fromServer.equals("disconnect") ) {
                break;
            }
                    
            fromUser = stdIn.readLine();
            if (fromUser != null) {
                System.out.println("Client: " + fromUser);
                out.println(fromUser);
            }
        }

        out.close();
        in.close();
        stdIn.close();
        kkSocket.close();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
