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
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.gui.ProgressMonitor;
import vsdk.toolkit.environment.scene.SimpleScene;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBPixel;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.io.PersistenceElement;

class JobAssigment
{
    public int x0;
    public int y0;
    public int x1;
    public int y1;
    public RGBImage result;
    public String toString()
    {
        String msg = "<" + x0 + ", " + y0 + "> - <" + x1 + ", " + y1 + ">";
        return msg;
    }
    public void merge(RGBImage tile)
    {
        int x, y;
        RGBPixel p = new RGBPixel();

        System.out.println("Merging " + this);

        for ( y = 1; y < tile.getYSize(); y++ ) {
            for ( x = 1; x < tile.getXSize(); x++ ) {
                tile.getPixelRgb(x, y, p);
                result.putPixelRgb(x+x0, y+y0, p);
            }
        }
    }
}

class RaytracerDistributedClient implements Runnable
{
    private LinkedBlockingQueue<JobAssigment> pendingtasks;
    private LinkedBlockingQueue<JobAssigment> pendingends;
    private String ip;
    private int port;
    private Socket socket;
    private boolean running;
    private int id;

    private SimpleScene theScene;
    private int x0;
    private int y0;
    private int x1;
    private int y1;

    public RaytracerDistributedClient(
        String ip, int port,
        SimpleScene theScene,
        int id,
        LinkedBlockingQueue<JobAssigment> pendingtasks,
        LinkedBlockingQueue<JobAssigment> pendingends
    )
    {
        this.pendingtasks = pendingtasks;
        this.pendingends = pendingends;
        this.ip = new String(ip);
        this.port = port;
        x0 = 150;
        y0 = 150;
        x1 = 300;
        y1 = 300;
        socket = null;
        running = true;
        this.theScene = theScene;
        this.id = id;
    }

    public void run()
    {
        System.out.println("Starting VitralVisualizationClient thread.");
        System.out.println("Trying to connect to IP " + ip + " at port " + port + "... ");
        InputStream is;
        OutputStream os;
        ObjectOutputStream serializer;
        byte responseType[] = new byte[1];

        try {
            socket = new Socket(ip, port);
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
            PersistenceElement.writeIntBE(os, id);
            PersistenceElement.readBytes(is, responseType);

            //-----------------------------------------------------------------

            //-----------------------------------------------------------------

            while ( running ) {
                try {
                    JobAssigment task = pendingtasks.take();
                    pendingends.put(task);
                    System.out.println("Sending task " + task);
                    PersistenceElement.writeAsciiString(os, "x0");
                    PersistenceElement.writeIntBE(os, task.x0);
                    PersistenceElement.readBytes(is, responseType);

                    PersistenceElement.writeAsciiString(os, "y0");
                    PersistenceElement.writeIntBE(os, task.y0);
                    PersistenceElement.readBytes(is, responseType);

                    PersistenceElement.writeAsciiString(os, "x1");
                    PersistenceElement.writeIntBE(os, task.x1);
                    PersistenceElement.readBytes(is, responseType);

                    PersistenceElement.writeAsciiString(os, "y1");
                    PersistenceElement.writeIntBE(os, task.y1);
                    PersistenceElement.readBytes(is, responseType);

                    PersistenceElement.writeAsciiString(os, "render");
                    ObjectInputStream ois = new ObjectInputStream(is);
                    RGBImage img = (RGBImage)ois.readObject();
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
    distributedControl(RGBImage theResultingImage,
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
                tile.x0 = x;
                tile.y0 = y;
                tile.x1 = x + dx;
                tile.y1 = y + dy;
                tile.result = theResultingImage;
                if ( tile.x1 >= theResultingImage.getXSize() ) {
                    tile.x1 = theResultingImage.getXSize() - 1;
                }
                if ( tile.y1 >= theResultingImage.getYSize() ) {
                    tile.y1 = theResultingImage.getYSize() - 1;
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
