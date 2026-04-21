// Java basic classes

import java.net.Socket;
import java.net.ServerSocket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.File;

// VitralSDK classes
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.environment.CameraSnapshot;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.gui.ProgressMonitorConsole;
import vsdk.toolkit.render.Raytracer;

import vsdk.toolkit.io.image.ImagePersistence;

class VitralVisualizationServerProtocol implements Runnable
{
    private Socket socket;
    private SimpleScene theScene;
    private Raytracer visualizationEngine;
    private RendererConfiguration rendererConfiguration;
    private RGBImage theResultingImage;
    private ProgressMonitorConsole reporter;
    private int id;
    private int x0;
    private int y0;
    private int x1;
    private int y1;

    private int readInt32BE(InputStream is) throws Exception
    {
        long value = PersistenceElement.readLongBE(is);
        if ( value < Integer.MIN_VALUE || value > Integer.MAX_VALUE ) {
            throw new IOException("Value out of 32-bit range: " + value);
        }
        return (int)value;
    }

    public VitralVisualizationServerProtocol(Socket socket)
    {
        this.socket = socket;
        theScene = null;
        visualizationEngine = new Raytracer();
        rendererConfiguration = new RendererConfiguration();
        theResultingImage = new RGBImage();
        id = 0;
        x0 = 0;
        y0 = 0;
        x1 = 100;
        y1 = 100;
    }

    /**
    This method implements current protocol.
    The input array is a petition string. Current supported petition strings
    are:
      - ping
      - scene: after this command is recieved, it is expected a serialized
        java object containing a SimpleScene.
      - render
      - id
      - x0
      - y0
      - x1
      - y1
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
      - 6 - Image answer... (?)
    */
    private byte[] servePetition(String in, InputStream is, OutputStream os) throws Exception
    {
        //-----------------------------------------------------------------
        byte[] out = new byte[5];

        //-----------------------------------------------------------------
        if ( in.equals("ping") ) {
            out = new byte[1];
            out[0] = 1;
        }
        else if ( in.equals("id") ) {
            id = readInt32BE(is);
            System.out.println("Server set to id " + id);
            out = new byte[1];
            out[0] = 1;
        }
        else if ( in.equals("x0") ) {
            x0 = readInt32BE(is);
            out = new byte[1];
            out[0] = 1;
        }
        else if ( in.equals("y0") ) {
            y0 = readInt32BE(is);
            out = new byte[1];
            out[0] = 1;
        }
        else if ( in.equals("x1") ) {
            x1 = readInt32BE(is);
            out = new byte[1];
            out[0] = 1;
        }
        else if ( in.equals("y1") ) {
            y1 = readInt32BE(is);
            out = new byte[1];
            out[0] = 1;
        }
        else if ( in.equals("scene") ) {
            ObjectInputStream inserializer;
            inserializer = new ObjectInputStream(is);
            theScene = (SimpleScene)inserializer.readObject();
            System.out.println("SCENE RECIEVED!");
            out = new byte[1];
            out[0] = 1;
        }
        else if ( in.equals("render") ) {
            if ( theScene != null ) {
                theResultingImage.init(x1 - x0, y1 - y0);
                reporter = null; //new ProgressMonitorConsole();
                CameraSnapshot cameraSnapshot =
                    theScene.getActiveCamera().exportToCameraSnapshot();

                visualizationEngine.execute(
                                theResultingImage,
                                rendererConfiguration,
                                theScene.getSimpleBodies(),
                                theScene.getLights(),
                                theScene.getActiveBackground(),
                                cameraSnapshot,
                                reporter, null,
                                x0, y0, x1, y1);
                System.out.println("SCENE RAYTRACED!");
                System.out.printf("Extends: <%d, %d> - <%d, %d>\n", x0, y0, x1, y1);
                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeObject(theResultingImage);
                oos.flush();

                /*
                File fd = new File("distributed" + id + ".png");
                if ( !ImagePersistence.exportPNG(fd, theResultingImage) ) {
                    System.err.println("Error grabando la imagen!!");
                }
                */
                out = new byte[1];
                out[0] = 1;
            }
        }
        //-----------------------------------------------------------------
        //String msg = "HOLA";
        //int i;
        //for ( i = 0; i < msg.length(); i++ ) {
        //    out[i] = (byte)msg.charAt(i);
        //}
        //out[i] = 0;

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
                //System.out.println("Recieved: " + msg);
                if ( msg != null && !msg.equals("") ) {
                    os.write(servePetition(msg, is, os));
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

public class RaytracerDistributedServer implements Runnable
{
    private int tcpPort;
    public RaytracerDistributedServer(int port)
    {
        tcpPort = port;
        Thread networkThread = new Thread(this);
        networkThread.start();
    }

    public void run()
    {
        ServerSocket ss;
        Socket cs;
        VitralVisualizationServerProtocol listener;

        System.out.println("Waiting for connections on TCP port " + tcpPort);
        try {
            ss = new ServerSocket(tcpPort);
            while ( true ) {
                cs = ss.accept();
                listener = new VitralVisualizationServerProtocol(cs);
                Thread listenerThread;
                listenerThread = new Thread(listener);
                listenerThread.start();
            }
        }
        catch ( Exception e ) {
            System.err.println("Error in VitralVisualizationServer communications!");
            System.err.println(e);
        }
    }

    public static void main(String args[])
    {
        int port = 1234;
        RaytracerDistributedServer instance;

        if ( args.length > 0 ) {
            port = Integer.parseInt(args[0]);
        }

        instance = new RaytracerDistributedServer(port);
    }

}
