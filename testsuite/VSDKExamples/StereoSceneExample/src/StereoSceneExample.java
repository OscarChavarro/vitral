//===========================================================================

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
import javax.media.opengl.GL2;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.GLDrawable;
import com.jogamp.opengl.util.Animator;

// VSDK classes
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;
import vsdk.toolkit.media.RGBColorPalette;
import vsdk.toolkit.io.image.RGBColorPalettePersistence;
import vsdk.toolkit.io.image.ImagePersistence;

import vsdk.toolkit.render.jogl.JoglStereoStrategyRenderer;
import vsdk.toolkit.render.jogl.JoglStereoStrategyAnaglyphRenderer;
import vsdk.toolkit.render.jogl.JoglStereoStrategyInterlaceRenderer;
import vsdk.toolkit.render.jogl.JoglStereoStrategyMultiviewRenderer;
import vsdk.toolkit.render.jogl.JoglStereoStrategyPBufferRenderer;
import vsdk.toolkit.render.jogl.JoglStereoStrategyQuadBufferRenderer;
import vsdk.toolkit.render.jogl.JoglStereoStrategyCyclopeanZBufferRenderer;
import vsdk.toolkit.render.jogl.JoglStereoStrategyAutostereogramRenderer;
import vsdk.toolkit.render.jogl.JoglStereoStrategyWiggleRenderer;

//===========================================================================

public class StereoSceneExample {

    public JoglStereoStrategyRenderer stereoStrategy;
    private JFrame mainWindowWidget;
    private JoglDrawingArea drawingArea;
    private EventDispatcher eventDispatcher;
    private RGBColorPalette palette;
    private RGBImage stereogramTilePattern;

    private boolean fullScreenGuiMode;
    private int stereoStrategyId;

    public GLJPanel canvas;
    public Scene scene;
    public double angle;
    public Animator animator;
    public boolean isRotating;

    public StereoSceneExample(int stereoStrategyId)
    {
        angle = -90;
        isRotating = false;
        fullScreenGuiMode = true;
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
        drawingArea = new JoglDrawingArea(this);
        eventDispatcher = new EventDispatcher(this);
        animator = null;

        //-----------------------------------------------------------------
        switch ( stereoStrategyId ) {
          case 1:  default:
            stereoStrategy = null;
            break;

          case 2:
            stereoStrategy = new JoglStereoStrategyInterlaceRenderer();
            break;

          case 3:
            stereoStrategy = new JoglStereoStrategyQuadBufferRenderer();
            break;

          case 4:
            stereoStrategy = new JoglStereoStrategyMultiviewRenderer();
            break;

          case 5:
            RGBImage rightImage = new RGBImage();
            RGBImage leftImage = new RGBImage();
            stereoStrategy = new JoglStereoStrategyPBufferRenderer(
                new LeftRenderer(drawingArea, leftImage),
                new RightRenderer(drawingArea, rightImage),
                leftImage, rightImage, xSize/2, ySize);
            break;

          case 6:
            stereoStrategy = new JoglStereoStrategyAnaglyphRenderer();
            ((JoglStereoStrategyAnaglyphRenderer)stereoStrategy).setBlendingMethod(
                true, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0);
            break;

          case 7:
            stereoStrategy = new JoglStereoStrategyCyclopeanZBufferRenderer(
                palette);
            break;

          case 8:
            stereoStrategy = new JoglStereoStrategyAutostereogramRenderer(
                stereogramTilePattern, 14.0, 2.3228, 12.0, 6.0, 112, 112);
            break;

          case 9:
            stereoStrategy = new JoglStereoStrategyWiggleRenderer(0.2);
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

            if ( stereoStrategy instanceof JoglStereoStrategyWiggleRenderer ) {
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
        animator = null;
        mainWindowWidget.setVisible(false);
        mainWindowWidget.dispose();
        System.gc();
        mainWindowWidget = null;
        System.gc();
    }

    public void createModel()
    {
        scene = new Scene();
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
        System.out.println("  - 0 numbers: switch animation rotation");
        System.out.println("  - d/D: control eye distance");
        System.out.println("  - t/T: control eye torsion angle");
        System.out.println("  - <Esc>: Quit program");
    }

    public static void main(String[] args) {
        StereoSceneExample instance;
        int id = 6;

        if ( args.length > 0 ) {
            id = Integer.parseInt(args[0]);
        }

        instance = new StereoSceneExample(id);
        instance.usage();
        instance.createModel();
        instance.createGUI();
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
