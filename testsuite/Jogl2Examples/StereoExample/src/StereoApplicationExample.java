// VITRAL recomendation: Use explicit class imports (not .*) in hello world 
// type programs so the user/programmer can be exposed to all the complexity 
// involved. This will help him to know better the involved libraries.

// Basic Java classes
import java.io.File;
import java.io.FileReader;

// AWT/Swing classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.JFrame;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.util.Animator;

// VSDK classes
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl2RGBImageUncompressedRenderer;
import vsdk.toolkit.media.RGBColorPalette;
import vsdk.toolkit.io.image.RGBColorPalettePersistence;
import vsdk.toolkit.io.image.ImagePersistence;

import vsdk.toolkit.render.jogl.Jogl2StereoStrategyRenderer;
import vsdk.toolkit.render.jogl.Jogl2StereoStrategyAnaglyphRenderer;
import vsdk.toolkit.render.jogl.Jogl2StereoStrategyInterlaceRenderer;
import vsdk.toolkit.render.jogl.Jogl2StereoStrategyMultiviewRenderer;
import vsdk.toolkit.render.jogl.Jogl2StereoStrategyPBufferRenderer;
import vsdk.toolkit.render.jogl.Jogl2StereoStrategyQuadBufferRenderer;
import vsdk.toolkit.render.jogl.Jogl2StereoStrategyCyclopeanZBufferRenderer;
import vsdk.toolkit.render.jogl.Jogl2StereoStrategyAutostereogramRenderer;
import vsdk.toolkit.render.jogl.Jogl2StereoStrategyWiggleRenderer;

//===========================================================================

public class StereoApplicationExample {

    public Jogl2StereoStrategyRenderer stereoStrategy;
    private JFrame mainWindowWidget;
    private GLJPanel canvas;
    private JoglDrawingArea drawingArea;
    private Animator animator;
    private EventDispatcher eventDispatcher;
    private RGBColorPalette palette;
    private RGBImageUncompressed stereogramTilePattern;

    private boolean fullScreenGuiMode;
    private int stereoStrategyId;

    public StereoApplicationExample(int stereoStrategyId)
    {
        fullScreenGuiMode = false;
        this.stereoStrategyId = stereoStrategyId;

        try {
            palette = RGBColorPalettePersistence.importGimpPalette(new FileReader("../../../etc/palettes/ColdfireReversed.gpl"));
            stereogramTilePattern = ImagePersistence.importRGB(new File("../../../etc/textureTiles/zelenaGreenPattern.jpg"));
        }
        catch ( Exception ex ) {
            System.err.println("Failed to read file.");
            ex.printStackTrace();
            System.exit(0);
        }

    }

    public void setStereoStrategyId(int stereoStrategyId)
    {
        this.stereoStrategyId = stereoStrategyId;
    }

    public void swithFullScreenMode()
    {
        if ( fullScreenGuiMode ) {
            fullScreenGuiMode = false;
        }
        else {
            fullScreenGuiMode = true;
        }
    }

    private void initGLCanvas(int xSize, int ySize) {
        drawingArea = new JoglDrawingArea();
        eventDispatcher = new EventDispatcher(this);
        animator = null;

        //-----------------------------------------------------------------
        switch ( stereoStrategyId ) {
          case 1:  default:
            stereoStrategy = null;
            break;

          case 2:
            stereoStrategy = new Jogl2StereoStrategyInterlaceRenderer();
            break;

          case 3:
            stereoStrategy = new Jogl2StereoStrategyQuadBufferRenderer();
            break;

          case 4:
            stereoStrategy = new Jogl2StereoStrategyMultiviewRenderer();
            break;

          case 5:
            RGBImageUncompressed rightImage = new RGBImageUncompressed();
            RGBImageUncompressed leftImage = new RGBImageUncompressed();
            stereoStrategy = new Jogl2StereoStrategyPBufferRenderer(
                new LeftRenderer(drawingArea, leftImage),
                new RightRenderer(drawingArea, rightImage),
                leftImage, rightImage, xSize/2, ySize);
            break;

          case 6:
            stereoStrategy = new Jogl2StereoStrategyAnaglyphRenderer();
            ((Jogl2StereoStrategyAnaglyphRenderer)stereoStrategy).setBlendingMethod(
                true, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0);
            break;

          case 7:
            stereoStrategy = new Jogl2StereoStrategyCyclopeanZBufferRenderer(
                palette);
            break;

          case 8:
            stereoStrategy = new Jogl2StereoStrategyAutostereogramRenderer(
                stereogramTilePattern, 14.0, 2.3228, 12.0, 6.0, 112, 112);
            break;

          case 9:
            stereoStrategy = new Jogl2StereoStrategyWiggleRenderer(0.2);
            break;

        }
        //stereoStrategy.setSwapChannels(true);

        drawingArea.setStereoStrategy(stereoStrategy);

        try {
            if ( stereoStrategy != null ) {
                GLProfile profile = GLProfile.get(GLProfile.GL2);
                GLCapabilities caps = new GLCapabilities(profile);

                stereoStrategy.requestCapabilities(caps);

                canvas = new GLJPanel(caps);
            }
            else {
                canvas = new GLJPanel();
            }

            if ( stereoStrategy instanceof Jogl2StereoStrategyWiggleRenderer ) {
                animator = new Animator(canvas);
                animator.start();
            }

            canvas.addGLEventListener(drawingArea);

            canvas.addKeyListener(eventDispatcher);
            canvas.addMouseListener(eventDispatcher);
            canvas.addMouseMotionListener(eventDispatcher);
            canvas.addMouseWheelListener(eventDispatcher);

            eventDispatcher.setCanvas(canvas);
            eventDispatcher.setDrawingArea(drawingArea);
        }
        catch ( Exception e ) {
            System.out.println("ERROR: Not enough GL capabilities to support requested stereo 3D rendering mode!");
            System.exit(0);
        }

        //-----------------------------------------------------------------
    }

    public void destroyGUI()
    {
        if ( animator != null ) {
            if ( animator.isAnimating() ) {
                animator.stop();
            }
        }
        mainWindowWidget.setVisible(false);
        mainWindowWidget.dispose();
        System.gc();
        mainWindowWidget = null;
        System.gc();
    }

    public void createGUI()
    {
        if ( fullScreenGuiMode ) {
            createGUIFullScreen();
        }
        else {
            createGUIWindowed();
        }
    }

    private void createGUIFullScreen()
    {
        mainWindowWidget = new JFrame("VITRAL concept test - Stereo strategy combiner");
        Toolkit tk = mainWindowWidget.getToolkit();
        Dimension d = tk.getScreenSize();

        initGLCanvas(d.width, d.height);

        mainWindowWidget.setUndecorated(true);
        mainWindowWidget.add(canvas, BorderLayout.CENTER);
        mainWindowWidget.setPreferredSize(d);
        mainWindowWidget.pack();
        mainWindowWidget.setLocationRelativeTo(null);
        mainWindowWidget.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainWindowWidget.setVisible(true);
        canvas.requestFocusInWindow();
    }

    private void createGUIWindowed()
    {
        int xSize = 640;
        int ySize = 480;
        initGLCanvas(xSize, ySize);
        mainWindowWidget = new JFrame("VITRAL concept test - Stereo strategy combiner");
        mainWindowWidget.add(canvas, BorderLayout.CENTER);

        // Useful for some setups
        //mainWindowWidget.setUndecorated(true);
        //mainWindowWidget.setBounds(100, 0, xSize, ySize);

        mainWindowWidget.pack();
        mainWindowWidget.setSize(xSize, ySize);
        mainWindowWidget.setLocationRelativeTo(null);

        mainWindowWidget.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainWindowWidget.setVisible(true);
        canvas.requestFocusInWindow();
    }

    private void usage()
    {
        System.out.println("Use following keys:");
        System.out.println("  - c: swap stereo channels");
        System.out.println("  - f: toggle fullscreen mode");
        System.out.println("  - 1-9 numbers: switch stereo strategy");
        System.out.println("  - <Esc>: Quit program");
    }

    public static void main(String[] args) {
        StereoApplicationExample instance;
        int id = 5;

        if ( args.length > 0 ) {
            id = Integer.parseInt(args[0]);
        }

        instance = new StereoApplicationExample(id);
        instance.usage();
        instance.createGUI();
    }

}
