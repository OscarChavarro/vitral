// Java basic classes
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.gui.ProgressMonitor;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.render.Raytracer;
import vsdk.toolkit.io.PersistenceElement;

class RaytracerDistributedClient implements Runnable
{
    private Raytracer visualizationEngine;

    private String ip;
    private int port;
    private Socket socket;
    private boolean running;

    private SimpleScene theScene;
    private int currentXStart;
    private int currentYStart;
    private int currentXEnd;
    private int currentYEnd;

    public RaytracerDistributedClient(
        String ip, int port,
        SimpleScene theScene,
        int x0, int y0, int x1, int y1)
    {
        this.ip = new String(ip);
        this.port = port;
        visualizationEngine = null;
        currentXStart = x0;
        currentYStart = y0;
        currentXEnd = x0;
        currentYEnd = y0;
        socket = null;
        running = true;
        this.theScene = theScene;
    }

    public void run()
    {
        System.out.println("Starting VitralVisualizationClient thread.");
        System.out.println("Trying to connect to IP " + ip + " at port " + port + "... ");
        InputStream is;
        OutputStream os;
        ObjectOutputStream serializer;
        byte responseType[] = new byte[1];

        visualizationEngine = new Raytracer();

        try {
            socket = new Socket(ip, port);
        }
        catch ( IOException e ) {
            System.err.println("Cannot connect to server!");
            System.err.println("Check server is running on correct IP/port, and that all network \nconfigurations (firewalls, gateways, etc.) are right.");
            System.exit(0);
        }

        try{
            is = socket.getInputStream();
            os = socket.getOutputStream();

            PersistenceElement.writeAsciiString(os, "scene");
            serializer = new ObjectOutputStream(os);
            serializer.writeObject(theScene);
            serializer.flush();
            PersistenceElement.readBytes(is, responseType);

            while ( running ) {
                System.out.print(".");
                PersistenceElement.writeAsciiString(os, "ping");
                PersistenceElement.readBytes(is, responseType);
                if ( responseType[0] == 1 ) {
                    ; // Empty answer
                    System.out.println("Empty answer!");
                }
                Thread.sleep(1000);
            }
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}

public class DistributerByArea
{

    public void
    distributedControl(RGBImage theResultingImage,
                       RendererConfiguration rendererConfiguration,
                       SimpleScene theScene,
                       ProgressMonitor reporter)
    {
        //- Create N raytracer clients ------------------------------------
        RaytracerDistributedClient client;
        client = new RaytracerDistributedClient("127.0.0.1", 1235, 
            theScene,
            100, 100, 200, 200);
        Thread t;
        t = new Thread(client);
        t.start();
/*
        if ( visualizationEngine == null ) {
            Thread t = new Thread(this);
            t.start();
        }
        while ( visualizationEngine == null );

        visualizationEngine.execute(theResultingImage,
                                    rendererConfiguration,
                                    theScene.getSimpleBodies(),
                                    theScene.getLights(),
                                    theScene.getActiveBackground(),
                                    theScene.getActiveCamera(),
                                    reporter, null,
                                    100, 100, 200, 200);
*/
    }
}
