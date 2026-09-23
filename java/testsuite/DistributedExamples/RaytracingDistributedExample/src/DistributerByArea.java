// Java basic classes

import java.net.Socket;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;

// VitralSDK classes
import vsdk.toolkit.gui.feedback.ProgressMonitor;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.io.PersistenceElement;

class JobAssigment
{
    public int startX;
    public int startY;
    public int endX;
    public int endY;
    public RGBImageUncompressed result;
    public String toString()
    {
        String msg = "<" + startX + ", " + startY + "> - <" + endX + ", " + endY + ">";
        return msg;
    }
    public void merge(RGBImageUncompressed tile)
    {
        int x, y;
        RGBPixel p = new RGBPixel();

        System.out.println("Merging " + this);

        for ( y = 1; y < tile.getYSize(); y++ ) {
            for ( x = 1; x < tile.getXSize(); x++ ) {
                tile.getPixelRgb(x, y, p);
                result.putPixelRgb(x+startX, y+startY, p);
            }
        }
    }
}

class RaytracerDistributedClient implements Runnable
{
    private LinkedBlockingQueue<JobAssigment> pendingtasks;
    private LinkedBlockingQueue<JobAssigment> pendingends;
    private String ipAddress;
    private int port;
    private Socket socket;
    private boolean running;
    private int id;

    private SimpleScene theScene;
    private int startX;
    private int startY;
    private int endX;
    private int endY;

    private void writeInt32BE(OutputStream os, int value) throws Exception
    {
        PersistenceElement.writeLongBE(os, value);
    }

    public RaytracerDistributedClient(
        String ipAddress, int port,
        SimpleScene theScene,
        int id,
        LinkedBlockingQueue<JobAssigment> pendingtasks,
        LinkedBlockingQueue<JobAssigment> pendingends
    )
    {
        this.pendingtasks = pendingtasks;
        this.pendingends = pendingends;
        this.ipAddress = new String(ipAddress);
        this.port = port;
        startX = 150;
        startY = 150;
        endX = 300;
        endY = 300;
        socket = null;
        running = true;
        this.theScene = theScene;
        this.id = id;
    }

    public void run()
    {
        System.out.println("Starting VitralVisualizationClient thread.");
        System.out.println("Trying to connect to IP " + ipAddress + " at port " + port + "... ");
        InputStream is;
        OutputStream os;
        ObjectOutputStream serializer;
        byte responseType[] = new byte[1];

        try {
            socket = new Socket(ipAddress, port);
        }
        catch ( IOException e ) {
            System.err.println("Cannot connect to server!");
            System.err.println("Check server is running on correct IP/port, and that all network \nconfigurations (firewalls, gateways, etc.) are right.");
            System.exit(0);
        }

        try{
            //-----------------------------------------------------------------
            is = socket.getInputStream();
            os = socket.getOutputStream();

            //-----------------------------------------------------------------
            PersistenceElement.writeAsciiString(os, "scene");
            serializer = new ObjectOutputStream(os);
            serializer.writeObject(theScene);
            serializer.flush();
            PersistenceElement.readBytes(is, responseType);

            PersistenceElement.writeAsciiString(os, "id");
            writeInt32BE(os, id);
            PersistenceElement.readBytes(is, responseType);

            //-----------------------------------------------------------------

            //-----------------------------------------------------------------

            while ( running ) {
                try {
                    JobAssigment task = pendingtasks.take();
                    pendingends.put(task);
                    System.out.println("Sending task " + task);
                    PersistenceElement.writeAsciiString(os, "x0");
                    writeInt32BE(os, task.startX);
                    PersistenceElement.readBytes(is, responseType);

                    PersistenceElement.writeAsciiString(os, "y0");
                    writeInt32BE(os, task.startY);
                    PersistenceElement.readBytes(is, responseType);

                    PersistenceElement.writeAsciiString(os, "x1");
                    writeInt32BE(os, task.endX);
                    PersistenceElement.readBytes(is, responseType);

                    PersistenceElement.writeAsciiString(os, "y1");
                    writeInt32BE(os, task.endY);
                    PersistenceElement.readBytes(is, responseType);

                    PersistenceElement.writeAsciiString(os, "render");
                    ObjectInputStream ois = new ObjectInputStream(is);
                    RGBImageUncompressed img = (RGBImageUncompressed)ois.readObject();
                    PersistenceElement.readBytes(is, responseType);

                    task.merge(img);
                    pendingends.take();
                    continue;
                }
                catch ( Exception e ) {
                    e.printStackTrace();
                    //System.out.print("*");
                    //Thread.sleep(1000);
                    //continue;
                }
                PersistenceElement.writeAsciiString(os, "ping");
                PersistenceElement.readBytes(is, responseType);
                if ( responseType[0] == 1 ) {
                    System.out.print(".");
                    //Thread.sleep(1000);
                }
            }
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}

public class DistributerByArea
{
    private LinkedBlockingQueue<JobAssigment> pendingtasks;
    private LinkedBlockingQueue<JobAssigment> pendingends;

    public void
    distributedControl(RGBImageUncompressed theResultingImage,
                       RendererConfiguration rendererConfiguration,
                       SimpleScene theScene,
                       ProgressMonitor reporter)
    {
        pendingtasks = new LinkedBlockingQueue<JobAssigment>();
        pendingends = new LinkedBlockingQueue<JobAssigment>();

        //-----------------------------------------------------------------
        int dx = 640;
        int dy = 480;
        int x;
        int y;

        for ( y = 0; y < theResultingImage.getYSize(); y += dy ) {
            for ( x = 0; x < theResultingImage.getXSize(); x += dx ) {
                JobAssigment tile;
                tile = new JobAssigment();
                tile.startX = x;
                tile.startY = y;
                tile.endX = x + dx;
                tile.endY = y + dy;
                tile.result = theResultingImage;
                if ( tile.endX >= theResultingImage.getXSize() ) {
                    tile.endX = theResultingImage.getXSize() - 1;
                }
                if ( tile.endY >= theResultingImage.getYSize() ) {
                    tile.endY = theResultingImage.getYSize() - 1;
                }
                try { 
                    pendingtasks.put(tile);
                }
                catch ( Exception e ) {
                    e.printStackTrace();
                }
            }
        }

        //- Create N raytracer clients ------------------------------------
        String configFilename = "./etc/localhost4cpus.txt";
        int nextid = 1;

        try {
            File fd = new File(configFilename);
            FileInputStream fis = new FileInputStream(fd);

            while ( fis.available() > 0 ) {
                String line;
                StringTokenizer parser;

                line = PersistenceElement.readAsciiLine(fis);
                parser = new StringTokenizer(line, " \n\t\r");
                String hostname = parser.nextToken();
                int port = Integer.parseInt(parser.nextToken());

                if ( !hostname.startsWith("#") ) {
                    RaytracerDistributedClient client;
                    client = new RaytracerDistributedClient(hostname, port, 
                                                            theScene, nextid,
                                                            pendingtasks,
                                                            pendingends);
                    nextid++;
                    Thread t;
                    t = new Thread(client);
                    t.start();
                }
            }

            fis.close();
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }

        System.out.print("Waiting for visualizers to end... ");
        while ( pendingends.size() > 0 );
        System.out.println("Ended!");
    }
}
