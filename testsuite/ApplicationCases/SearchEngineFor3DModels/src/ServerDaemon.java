//===========================================================================
//= A 3D Search Engine based on Vitral SDK toolkit platform                 =
//=-------------------------------------------------------------------------=
//= Oscar Chavarro, May 23 2007                                             =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =
//===========================================================================

// Java basic classes
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Collections;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

// Awt / swing classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLAutoDrawable;

// VSDK Classes
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.GeometryMetadata;
import vsdk.toolkit.media.ShapeDescriptor;
import vsdk.toolkit.processing.ImageProcessing;
import vsdk.framework.shapeMatching.JoglShapeMatchingOfflineRenderer;
import vsdk.framework.shapeMatching.JoglShapeMatchingOfflineRenderable;
import vsdk.framework.shapeMatching.JoglProjectedViewRenderer;
import vsdk.toolkit.gui.ProgressMonitorConsole;

/**
Current application provides a server (TCP based) oriented management control
for the testing of the VitralSDK implementation of a search engine for
3d models, as the one defined in [FUNK2003].

The application provides basic operations, useful for testing and benchmarking.

Current class is responsible of handling user requests, and managing graphics
contexts for OpenGL.  This console application has two graphics operation
modes:
  - A true 100% offline renderer that enables hardware accelerated rendering
    using pbuffers.
  - A software renderer that lies upon a valid and visible window.
*/
public class ServerDaemon extends JoglShapeMatchingOfflineRenderable implements GLEventListener
{
    private JoglShapeMatchingOfflineRenderer offlineRenderer = null;
    private int distanceFieldSide = 64;
    private int renderPreviewXSize = 640;
    private int renderPreviewYSize = 480;
    private JoglProjectedViewRenderer projectedViewRenderer = null;
    private GLCanvas canvas;
    private SearchEngine searchEngine;
    private ArrayList<GeometryMetadata> descriptorsArray;
    private HashMap<String, TimeReport> times;
    public boolean terminate = false;
    public String command[];

    private FileOutputStream fos;
    private BufferedOutputStream writer;

    private ServerDaemon()
    {
        projectedViewRenderer = new JoglProjectedViewRenderer(distanceFieldSide, distanceFieldSide, false);
        offlineRenderer = new JoglShapeMatchingOfflineRenderer(renderPreviewXSize, renderPreviewYSize, this);
        searchEngine = new SearchEngine();
        canvas = null;

        times = searchEngine.createTimers();

        descriptorsArray = new ArrayList<GeometryMetadata>();
        searchEngine.readDatabase(descriptorsArray, times);
    }

    private void startWriter()
    {
        try {
            File fd = new File("output.html");

            fos = new FileOutputStream(fd);
            writer = new BufferedOutputStream(fos);
        }
        catch ( Exception e ) {
            ;
        }
    }

    private void stopWriter()
    {
        try {
            writer.close();
            fos.close();
        }
        catch ( Exception e ) {
            ;
        }
    }

    private void exportResults(ArrayList <Result> similarModels, ArrayList<GeometryMetadata> descriptorsArray)
    {
        int i;

        //- Sort report ---------------------------------------------------
        Collections.sort(similarModels);
        //- Print report --------------------------------------------------
        System.out.println("= SIMILAR MODELS REPORT ===================================================");
        System.out.println("Founded " + similarModels.size() +
            " similar objects:");
        for ( i = 0; i < similarModels.size(); i++ ) {
            System.out.println("  - " + similarModels.get(i));
        }

        startWriter();

        searchEngine.writeResultsAsHtml(new PrintWriter(writer), similarModels, descriptorsArray);

        stopWriter();
    }

    public ArrayList <Result> runCommand(GL gl, String command[], HashMap<String, TimeReport> times)
    {
        ArrayList <Result> similarModels = null;
        int i, j, k;

        times.get("TOTAL").start();
        if ( command[0].equals("add") ) {
            System.out.println("Processing command add...");
            searchEngine.indexFiles(gl, command, descriptorsArray, distanceFieldSide, canvas, offlineRenderer, projectedViewRenderer, times);
            searchEngine.saveDatabase(descriptorsArray, times);
        }
        else if ( command[0].equals("addList") ) {
            String list[] = null;
            try {
                File fd;
                FileReader fr;
                BufferedInputStream bis;
                BufferedReader reader;
                int count = 0;
                InputStreamReader isr;
                StreamTokenizer parser;
                int tokenType;
                i = 0;

                //- Count filenames ------------------------------------------
                fd = new File(command[1]);
                fr = new FileReader(fd);
                parser = new StreamTokenizer(fr);

                parser.resetSyntax();
                parser.eolIsSignificant(true);
                parser.wordChars(0x00, 0xFF);
                parser.whitespaceChars('\n', '\n');

                do {
                    try {
                        tokenType = parser.nextToken();
                      }
                      catch (Exception e) {
                        break;
                    }

                    switch ( tokenType ) {
                      case StreamTokenizer.TT_WORD:
                        count++;
                        break;
                      case StreamTokenizer.TT_EOL: 
                        break;
                      case StreamTokenizer.TT_EOF: break;
                      default: break;
                    }

                } while ( tokenType != StreamTokenizer.TT_EOF );
                fr.close();

                //- Load filenames -------------------------------------------
                System.out.println("Processing " + count + " files.");
                list = new String[count];

                fd = new File(command[1]);
                fr = new FileReader(fd);
                parser = new StreamTokenizer(fr);

                parser.resetSyntax();
                parser.eolIsSignificant(true);
                parser.wordChars(0x00, 0xFF);
                parser.whitespaceChars('\n', '\n');

                i = 0;
                do {
                    try {
                        tokenType = parser.nextToken();
                      }
                      catch (Exception e) {
                        break;
                    }

                    switch ( tokenType ) {
                      case StreamTokenizer.TT_WORD:
                        list[i] = new String(parser.sval);
                        i++;
                        break;
                      case StreamTokenizer.TT_EOL: 
                        break;
                      case StreamTokenizer.TT_EOF: break;
                      default: break;
                    }

                } while ( tokenType != StreamTokenizer.TT_EOF );
                fr.close();
            }
            catch ( Exception e ) {
                System.err.println("Error building file list.");
                System.exit(1);
            }
            searchEngine.indexFiles(gl, list, descriptorsArray, distanceFieldSide, canvas, offlineRenderer, projectedViewRenderer, times);
            searchEngine.saveDatabase(descriptorsArray, times);
        }
        else if ( command[0].equals("searchModel") ) {
            similarModels = searchEngine.matchModel(gl, command[1], descriptorsArray, 1, distanceFieldSide, canvas, offlineRenderer, projectedViewRenderer, times);
        }
        else if ( command[0].equals("searchDistanceField") ) {
            IndexedColorImage distanceField = searchEngine.readIndexedColorImage(command[1], distanceFieldSide);
            if ( distanceField == null ) {
                System.err.println("Error importing distance field. Program aborted.");
                System.exit(1);
            }
            similarModels = searchEngine.matchSketch(gl, distanceField, descriptorsArray, 5, times);
        }
        else if ( command[0].equals("searchSketch") ) {
            IndexedColorImage outline = searchEngine.readIndexedColorImage(command[1], distanceFieldSide);
            if ( outline == null ) {
                System.err.println("Error importing distance field. Program aborted.");
                System.exit(1);
            }
            IndexedColorImage distanceField;
            distanceField = new IndexedColorImage();
            distanceField.init(distanceFieldSide, distanceFieldSide);
            ImageProcessing.processDistanceFieldWithArray(outline, distanceField, 1);
            similarModels = searchEngine.matchSketch(gl, distanceField, descriptorsArray, 5, times);
        }
        else if ( command[0].equals("exportDatabase") ) {
            if ( command.length != 2 ) {
                System.err.println("ERROR: exportDatabase command must specified a text filename to export.");
                System.err.println("Database exporting aborted.");
            }
            else {
                System.out.println("Processing command exportDatabase ...");
                times.get("WRITE_DATABASE").start();
                System.out.println("Exporting database with " + descriptorsArray.size() + " fields!");
                System.out.println("Depending on database size, this could take some time...");
                GeometryMetadata m;
                ArrayList<ShapeDescriptor> d;
                ShapeDescriptor s;
                double arr[];
                String stringSegment;

                try {
                    //---------------------------------------------------------
                    File fd = new File(command[1]);
                    FileOutputStream fos;
                    BufferedOutputStream writer;

                    fd = new File(command[1]);
                    fos = new FileOutputStream(fd);
                    writer = new BufferedOutputStream(fos);

                    //---------------------------------------------------------
                    byte barr[];
                    ProgressMonitorConsole reporter;
                    reporter = new ProgressMonitorConsole();

                    reporter.begin();
                    for ( i = 0; i < descriptorsArray.size(); i++ ) {
                        m = descriptorsArray.get(i);
                        d = m.getDescriptors();
                        stringSegment = "[" + m.getId() + "]\t" + m.getFilename() + "\t";
                        for ( j = 0; j < d.size(); j++ ) {
                            s = d.get(j);
                            stringSegment += s.getLabel() + "\t";
                            barr = stringSegment.getBytes();
                            writer.write(barr, 0, barr.length);

                            arr = s.getFeatureVector();
                            for ( k = 0; k < arr.length; k++ ) {
                                stringSegment = arr[k] + "\t";
                                barr = stringSegment.getBytes();
                                writer.write(barr, 0, barr.length);
                            }
                        }
                        stringSegment = "EOL\n";
                        barr = stringSegment.getBytes();
                        writer.write(barr, 0, barr.length);

                        reporter.update(0, descriptorsArray.size(), i);

                    }
                    reporter.end();

                    writer.close();
                    fos.close();
                }
                catch ( Exception e ) {
                    System.out.println("Error exporting database!");
                }
                times.get("WRITE_DATABASE").stop();
                System.out.println("Done exporting database.");
            }
        }
        else {
            System.err.println("Invalid command [" + command[0] + "]");
        }

        //-----------------------------------------------------------------
        times.get("TOTAL").stop();
        return similarModels;
    }

    public void executeRendering(GL gl)
    {
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        if ( canvas != null ) {
            canvas.swapBuffers();
        }

        //- Listen for requirements ---------------------------------------
        ServerSocket socket = null;
        Socket clientSocket;

        command = null;
        do {
            try {
                if ( socket == null ) {
                    socket = new ServerSocket(1234);
                    socket.setSoTimeout(1000);
                    System.out.println("Waiting for client connections at port 1234!");
                }
                clientSocket = socket.accept();
                NetworkAttendingThread sonThread;
                sonThread = new NetworkAttendingThread(clientSocket, this);
                Thread t = new Thread(sonThread);
                t.start();
            }
            catch ( SocketTimeoutException e ) {
                System.out.print(".");
                if ( command != null ) {
                    ArrayList <Result> similarModels;
                    similarModels = runCommand(gl, command, times);
                    if ( similarModels != null ) {
                        exportResults(similarModels, descriptorsArray);
                    }
                }
                command = null;
            }
            catch( Exception e ) {
                System.err.println("Network error: " + e);
                e.printStackTrace();
                System.exit(1);
            }
        } while ( !terminate );

        //- Free unused references for garbage collection -----------------
        int i;
        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            descriptorsArray.set(i, null);
        }
        for ( i = 0; i < descriptorsArray.size(); i++ ) {
            descriptorsArray.remove(0);
        }
        descriptorsArray = null;
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void init(GLAutoDrawable drawable) {
        ;
    }

    /** Not used method, but needed to instanciate GLEventListener */
    public void displayChanged(GLAutoDrawable drawable, boolean a, boolean b) {
        ;
    }

    /** Called to indicate the drawing surface has been moved and/or resized */
    public void reshape (GLAutoDrawable drawable,
                         int x,
                         int y,
                         int width,
                         int height) {
        GL gl = drawable.getGL();
        gl.glViewport(0, 0, width, height); 
    }   

    /** Called by drawable to initiate drawing */
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        executeRendering(gl);
        System.exit(1);
    }

    public static void main(String args[])
    {
        ServerDaemon instance = new ServerDaemon();
        if ( instance.offlineRenderer.isPbufferSupported() ) {
            instance.offlineRenderer.execute();
            instance.offlineRenderer.waitForMe();
        }
        else {
            // Create application based GUI
            JFrame frame;
            Dimension size;

            instance.canvas = new GLCanvas();
            instance.canvas.addGLEventListener(instance);
            frame = new JFrame("VITRAL offline renderer window - do not hide");
            frame.add(instance.canvas, BorderLayout.CENTER);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            size = new Dimension(instance.renderPreviewXSize+40, instance.renderPreviewYSize+60);
            frame.setMinimumSize(size);
            frame.setSize(size);
            frame.setVisible(true);
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
