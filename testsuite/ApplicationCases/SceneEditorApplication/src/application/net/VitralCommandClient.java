package application.net;

// Java basic classes
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;

// VitralSDK classes
import vsdk.toolkit.io.PersistenceElement;

// Application classes
import application.SceneEditorApplication;

public class VitralCommandClient implements Runnable
{
    private SceneEditorApplication parent;
    public boolean running;
    private String ip;
    private int port;
    private Socket socket;

    public VitralCommandClient(SceneEditorApplication parent, String ip, int port)
    {
        this.parent = parent;
        this.ip = ip;
        this.port = port;
        socket = null;
        running = true;
    }

    private void requestServiceTransaction(InputStream is,
                                           OutputStream os)
    {
        String petition = "getCommand";
        String msg;
        byte responseType[] = new byte[1];

        try {
            PersistenceElement.writeAsciiString(os, petition);
            PersistenceElement.readBytes(is, responseType);
            if ( responseType[0] == 1 ) {
                ; // Empty answer
            }
            else if ( responseType[0] == 5 ) {
                msg = PersistenceElement.readAsciiString(is);
                parent.externalCommand(msg);
            }
            else {
                // ??
                System.err.println("Warning: unsupported answer from server (" +responseType[0] + ").");
                System.err.println("Closing connection.");
                running = false;
            }
        }
        catch ( Exception e ) {
            System.err.println("Error in requestServiceTransaction.");
            System.err.println("Closing connection.");
            System.err.println(e);
            running = false;
        }
    }

    @Override
    public void run()
    {
        System.out.println("Starting VitralCommandClient thread.");
        System.out.println("Trying to connect to IP " + ip + " at port " + port + "... ");
        InputStream is;
        OutputStream os;

        try {
            socket = new Socket(ip, port);
            is = socket.getInputStream();
            os = socket.getOutputStream();
        }
        catch ( Exception e ) {
            System.err.println("Error trying to connect.");
            System.err.println("Check application has access to network and server side application is running!");
            System.err.println(e);
            return;
        }
        while ( running ) {
            requestServiceTransaction(is, os);
            try {
                Thread.sleep(500);
            }
            catch ( Exception e ) {
                ;
            }
        }
        if ( socket != null ) {
            try {
                socket.close();
            }
            catch( Exception e ) {
                ;
            }
            socket = null;
        }
        System.out.println("\nEnding VitralCommandClient thread.\n");
    }

    public void end()
    {
        if ( socket != null ) {
            try {
                socket.close();
            }
            catch( Exception e ) {
                ;
            }
            socket = null;
        }
        running = false;
    }
}
