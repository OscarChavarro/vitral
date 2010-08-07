//===========================================================================

// Java basic classes
import java.net.Socket;
import java.net.ServerSocket;
import java.io.InputStream;
import java.io.OutputStream;

// VitralSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Quaternion;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.io.PersistenceElement;

class VitralCommandServerProtocol implements Runnable
{
    private CommandServer parent;
    private Socket socket;
    public VitralCommandServerProtocol(CommandServer parent, Socket socket)
    {
        this.parent = parent;
        this.socket = socket;
    }

    /**
    This method implements current protocol.
    The input array is a petition string. Current supported petition strings
    are:
      - "getSelectedPosition"
      - "getSelectedRotation"
    Current selected data is extracted from selected object (if one object is
    selected) or from current camera if no object is selected.

    The output array is a codified binary answer containing two parts: a byte
    identifying the data type returned and a series of bytes containing
    the data itself.  The current supported packets are:
      - 1 - empty answer
      - 2 - double answer followed by 4 bytes of big endian double number
      - 3 - Vector3D answer followed by 12 bytes of big endian vector data
      - 4 - Quaternion answer followed by 16 bytes of big endian quaternion data
      - 5 - String answer followed by zero terminated string
    */
    private byte[] servePetition(String in)
    {
        //-----------------------------------------------------------------
        byte[] out = null;

        //-----------------------------------------------------------------
        String msg = parent.currentCommand;
        if ( in.equals("getCommand") && msg != null ) {
            System.out.println("Sending command [" + msg + "] over the network!");
            out = new byte[1+msg.length()+1];
            int i;
            out[0] = 5;
            for ( i = 0; i < msg.length(); i++ ) {
                out[i+1] = (byte)msg.charAt(i);
            }
            out[i+1] = 0;
            parent.currentCommand = null;
        }
        else {
            out = new byte[1];
            out[0] = 1;
        }

        return out;
    }

    public void run()
    {
        InputStream is;
        OutputStream os;
        String msg;

        System.out.println("Creating new connection with TCP client!");

        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
            while ( true ) {
                msg = PersistenceElement.readAsciiString(is);
                if ( msg != null && !msg.equals("") ) {
                    os.write(servePetition(msg));
                }
                else {
                    System.out.println("Connection dropped!");
                    return;
                }
            }
        }
        catch ( Exception e ) {
            System.err.println("Error on VitralCommandServerProtocol!");
            System.err.println(e);
        }
    }
}

public class VitralCommandServer implements Runnable
{
    private CommandServer parent;
    private int tcpPort;

    public VitralCommandServer(CommandServer parent)
    {
        this.parent = parent;
        tcpPort = 1235;
        Thread networkThread = new Thread(this);
        networkThread.start();
    }

    public void run()
    {
        ServerSocket ss;
        Socket cs;
        VitralCommandServerProtocol listener;

        System.out.println("Waiting for connections on TCP port " + tcpPort);

        try {
            ss = new ServerSocket(tcpPort);
            while ( true ) {
                cs = ss.accept();
                listener = new VitralCommandServerProtocol(parent, cs);
                Thread listenerThread;
                listenerThread = new Thread(listener);
                listenerThread.start();
            }
        }
        catch ( Exception e ) {
            System.err.println("Error in VitralCommandServer communications!");
            System.err.println(e);
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
