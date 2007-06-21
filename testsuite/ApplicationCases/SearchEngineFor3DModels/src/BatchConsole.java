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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Collections;

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
Current application provides a batch (command line) oriented management control
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
public class BatchConsole extends JoglShapeMatchingOfflineRenderable implements GLEventListener
{
    private JoglShapeMatchingOfflineRenderer offlineRenderer = null;
    private int distanceFieldSide = 64;
    private int renderPreviewXSize = 640;
    private int renderPreviewYSize = 480;
    private JoglProjectedViewRenderer projectedViewRenderer = null;
    private String args[];
    private GLCanvas canvas;
    private SearchEngine searchEngine;
    private HashMap<String, TimeReport> times;

    private BatchConsole(String args[])
    {
        this.args = args;
        projectedViewRenderer = new JoglProjectedViewRenderer(distanceFieldSide, distanceFieldSide, false);
        offlineRenderer = new JoglShapeMatchingOfflineRenderer(renderPreviewXSize, renderPreviewYSize, this);
        searchEngine = new SearchEngine();
        canvas = null;
        times = new HashMap<String, TimeReport>();
    }

    private void reportTimes()
    {
        int i;
        TimeReport r;
        double totalTime = 0;
        double totalSum = 0;

        System.out.println("= TIMERS REPORT ===========================================================");
        System.out.println("Following a report of different stage timers for program operations:");
        Set<String> s = times.keySet();
        for ( String e : s ) {
            r = times.get(e);
            if ( r.getLabel().equals("TOTAL") ) {
                totalTime = r.getTime();
            }
            else {
                System.out.println("  - " + r);
                totalSum += r.getTime();
            }
        }
        System.out.println("  - " + times.get("TOTAL"));
        System.out.printf("  - Other times (%f total - %f summed): %f\n",
                          totalTime, totalSum, totalTime - totalSum);
        System.out.println("Other times stand for:");
        System.out.println("  - Garbage collection");
        System.out.println("  - Errors in time metrics");
        System.out.println("  - Control operations");
        System.out.println("  - Time used for console to scroll text");
        System.out.println("  - Time used for redirected console to save log files");
        System.out.println("Note that current figures stands for ELLAPSED (no CPU) times (java limitation)");
    }

    private void reportResults(ArrayList <Result> similarModels)
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
    }

    public void executeRendering(GL gl)
    {
        ArrayList<GeometryMetadata> descriptorsArray;
        descriptorsArray = new ArrayList<GeometryMetadata>();
        ArrayList <Result> similarModels;
        int i, j, k;

        times.put("TOTAL", new TimeReport("TOTAL"));
        times.put("VOXELIZE", new TimeReport("VOXELIZE"));
        times.put("READ_MODEL", new TimeReport("READ_MODEL"));
        times.put("READ_DATABASE", new TimeReport("READ_DATABASE"));
        times.put("WRITE_DATABASE", new TimeReport("WRITE_DATABASE"));
        times.put("SEARCH_MODEL", new TimeReport("SEARCH_MODEL"));
        times.put("SPHERICAL_HARMONICS", new TimeReport("SPHERICAL_HARMONICS"));
        times.put("PRIMITIVE_COUNT", new TimeReport("PRIMITIVE_COUNT"));
        times.put("CUBE13_PROJECTIONS", new TimeReport("CUBE13_PROJECTIONS"));
        times.put("PREVIEWS", new TimeReport("PREVIEWS"));
        times.get("TOTAL").start();

        if ( args[0].equals("add") ) {
            System.out.println("Processing command add...");
            searchEngine.readDatabase(descriptorsArray, times);
            searchEngine.indexFiles(gl, args, descriptorsArray, distanceFieldSide, canvas, offlineRenderer, projectedViewRenderer, times);
            searchEngine.saveDatabase(descriptorsArray, times);
        }
        else if ( args[0].equals("addList") ) {
            searchEngine.readDatabase(descriptorsArray, times);
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
                fd = new File(args[1]);
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

                fd = new File(args[1]);
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
        else if ( args[0].equals("searchModel") ) {
            searchEngine.readDatabase(descriptorsArray, times);
            similarModels = searchEngine.matchModel(gl, args[1], descriptorsArray, 1, distanceFieldSide, canvas, offlineRenderer, projectedViewRenderer, times);
            reportResults(similarModels);
        }
        else if ( args[0].equals("searchDistanceField") ) {
            searchEngine.readDatabase(descriptorsArray, times);
            IndexedColorImage distanceField = searchEngine.readIndexedColorImage(args[1], distanceFieldSide);
            if ( distanceField == null ) {
                System.err.println("Error importing distance field. Program aborted.");
                System.exit(1);
            }
            similarModels = searchEngine.matchSketch(gl, distanceField, descriptorsArray, 5, times);
            reportResults(similarModels);
        }
        else if ( args[0].equals("searchSketch") ) {
            searchEngine.readDatabase(descriptorsArray, times);
            IndexedColorImage outline = searchEngine.readIndexedColorImage(args[1], distanceFieldSide);
            if ( outline == null ) {
                System.err.println("Error importing distance field. Program aborted.");
                System.exit(1);
            }
            IndexedColorImage distanceField;
            distanceField = new IndexedColorImage();
            distanceField.init(distanceFieldSide, distanceFieldSide);
            ImageProcessing.processDistanceFieldWithArray(outline, distanceField, 1);
            similarModels = searchEngine.matchSketch(gl, distanceField, descriptorsArray, 5, times);
            reportResults(similarModels);
        }
        else if ( args[0].equals("exportDatabase") ) {
            if ( args.length != 2 ) {
                System.err.println("ERROR: exportDatabase command must specified a text filename to export.");
                System.err.println("Database exporting aborted.");
            }
            else {
                System.out.println("Processing command exportDatabase ...");
                searchEngine.readDatabase(descriptorsArray, times);

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
                    File fd = new File(args[1]);
                    FileOutputStream fos;
                    BufferedOutputStream writer;

                    fd = new File(args[1]);
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
            System.err.println("Invalid command [" + args[0] + "]");
        }

        //-----------------------------------------------------------------
        times.get("TOTAL").stop();
        reportTimes();

        //- Free unused references for garbage collection -----------------
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
        if ( args.length < 1 ) {
            System.err.println("Usage: java BatchConsole command files...");
            System.err.println("Commands are: add, addList, searchModel, searchSketch, searchDistanceField, exportDatabase");
            System.exit(0);
        }

        BatchConsole instance = new BatchConsole(args);
        if ( instance.offlineRenderer.isPbufferSupported() ||
             (args.length > 0 && 
              !( args[0].equals("add") || args[0].equals("addList") )) ) {
            if ( instance.offlineRenderer.isPbufferSupported() ) {
                instance.offlineRenderer.execute();
                instance.offlineRenderer.waitForMe();
            }
	    else {
                instance.executeRendering(null);
	    }
        }
        else {
            if ( args.length > 0 && 
                ( args[0].equals("add") || args[0].equals("addList") ) ) {
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
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
