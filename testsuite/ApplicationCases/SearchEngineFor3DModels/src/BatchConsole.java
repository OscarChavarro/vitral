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
import java.util.ArrayList;
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
import vsdk.toolkit.media.GeometryMetadata;
import vsdk.framework.shapeMatching.JoglShapeMatchingOfflineRenderer;
import vsdk.framework.shapeMatching.JoglShapeMatchingOfflineRenderable;
import vsdk.framework.shapeMatching.JoglProjectedViewRenderer;

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
    // Graphical contexts for indexing
    private GLCanvas canvas;
    private JoglShapeMatchingOfflineRenderer offlineRenderer = null;
    private JoglProjectedViewRenderer projectedViewRenderer = null;
    private int renderPreviewXSize = 640;
    private int renderPreviewYSize = 480;

    // Search engine and its configuration
    private SearchEngine searchEngine;
    private ShapeDatabaseFile shapeDatabase;
    private String command[];
    private int distanceFieldSide = 64;

    private BatchConsole(String command[])
    {
        this.command = command;
        projectedViewRenderer = new JoglProjectedViewRenderer(distanceFieldSide, distanceFieldSide, false);
        offlineRenderer = new JoglShapeMatchingOfflineRenderer(renderPreviewXSize, renderPreviewYSize, this);
        searchEngine = new SearchEngine();
        canvas = null;
        shapeDatabase = new ShapeDatabaseFile("etc/metadata.bin");
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
        //-----------------------------------------------------------------
        ArrayList <Result> similarModels;

        searchEngine.readDatabase(shapeDatabase);
        similarModels = searchEngine.runCommand(gl, offlineRenderer, canvas, projectedViewRenderer, command, shapeDatabase, distanceFieldSide);
        if ( similarModels != null ) {
            reportResults(similarModels);
        }
        searchEngine.reportTimers();

        //- Free unused references for garbage collection -----------------
        shapeDatabase = null;
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
        else if ( args.length > 0 && 
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

//===========================================================================
//= EOF                                                                     =
//===========================================================================
