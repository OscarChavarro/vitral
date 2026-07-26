import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;

import javax.swing.JFrame;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import gui.DebuggerKeyboardInteractionTechniques;
import gui.DebuggerMouseInteractionTechniques;
import models.GizmoNames;
import models.TangibleInterfaceGizmosModel;
import render.Jogl4DebuggerRenderer;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.gui.CameraControllerOrbiter;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.io.geometry.stl.StlWriter;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

public class InteractiveModelPreviewer extends JFrame implements
    MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
{
    private static final String WINDOW_TITLE =
        "VITRAL concept test - Tangible Interface Gizmo Creator";
    private static final Dimension DEFAULT_WINDOW_SIZE = new Dimension(1024, 768);
    private static final double STL_EXPORT_SCALE_FACTOR = 100;

    private final TangibleInterfaceGizmosModel model;
    private final DebuggerKeyboardInteractionTechniques keyboardInteractionTechniques;
    private final DebuggerMouseInteractionTechniques mouseInteractionTechniques;
    private final Jogl4DebuggerRenderer joglDebuggerRenderer;
    private boolean shutdownRequested;

    public InteractiveModelPreviewer(TangibleInterfaceGizmosModel model)
    {
        this.model = model;
        this.keyboardInteractionTechniques = new DebuggerKeyboardInteractionTechniques();
        this.mouseInteractionTechniques = new DebuggerMouseInteractionTechniques();
        this.joglDebuggerRenderer = new Jogl4DebuggerRenderer(model);
        this.shutdownRequested = false;
    }

    public static void launch(TangibleInterfaceGizmosModel model)
    {
        Jogl4Renderer.verifyOpenGLAvailability();
        InteractiveModelPreviewer instance = new InteractiveModelPreviewer(model);
        instance.exportCurrentModelStl();
        instance.createMainWindow(false);
    }

    private GLCanvas createGUI()
    {
        GLProfile profile = pickCompatibleProfile();
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDepthBits(64);
        model.setCanvas(new GLCanvas(caps));
        model.getCanvas().addGLEventListener(joglDebuggerRenderer);
        model.getCanvas().addMouseListener(this);
        model.getCanvas().addMouseMotionListener(this);
        model.getCanvas().addMouseWheelListener(this);
        model.getCanvas().addKeyListener(this);
        return model.getCanvas();
    }

    private GLProfile pickCompatibleProfile()
    {
        if ( GLProfile.isAvailable(GLProfile.GL4bc) ) {
            return GLProfile.get(GLProfile.GL4bc);
        }
        return GLProfile.get(GLProfile.GL4);
    }

    private void rebuildSolid()
    {
        TangibleInterfaceGizmoCreator.buildSolidWithRecovery(model);
    }

    private void repaintCanvas()
    {
        if ( model.getCanvas() != null ) {
            model.getCanvas().repaint();
            model.getCanvas().display();
        }
    }

    private void exportCurrentSolidToStl()
    {
        PolyhedralBoundedSolid solid = model.getSolid();
        if ( solid == null ) {
            System.err.println("[TangibleInterfaceGizmoCreator] No selected solid "
                + "available for STL export");
            return;
        }

        File outputFile = new File("output.stl");
        ensureParentFolder(outputFile);
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            StlWriter.exportSolid(solid, outputStream, STL_EXPORT_SCALE_FACTOR);
            System.out.println("[TangibleInterfaceGizmoCreator] Exported " +
                outputFile.getPath());
        }
        catch ( Exception e ) {
            System.err.println("[TangibleInterfaceGizmoCreator] STL export failed: "
                + e.getMessage());
        }
    }

    private void exportCurrentModelStl()
    {
        if ( model.getSolid() == null || model.isErrorState() ) {
            return;
        }

        int modelIndex = model.getSolidModelName().getDisplayIndex();
        File outputFile = new File("output" + modelIndex + ".stl");
        ensureParentFolder(outputFile);
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            StlWriter.exportSolid(model.getSolid(), outputStream,
                STL_EXPORT_SCALE_FACTOR);
            System.out.println("[TangibleInterfaceGizmoCreator] Exported " +
                outputFile.getPath());
        }
        catch ( Exception e ) {
            System.err.println("[TangibleInterfaceGizmoCreator] STL export failed: "
                + e.getMessage());
        }
    }

    private static void ensureParentFolder(File outputFile)
    {
        File parent = outputFile.getParentFile();
        if ( parent != null && !parent.exists() ) {
            parent.mkdirs();
        }
    }

    private Vector3Dd calculateSolidCenter()
    {
        if ( model.getSolid() == null ) {
            return new Vector3Dd(0, 0, 0);
        }
        double[] minMax = model.getSolid().getMinMax();
        if ( minMax == null || minMax.length < 6 ) {
            return new Vector3Dd(0, 0, 0);
        }
        return new Vector3Dd(
            (minMax[0] + minMax[3]) / 2.0,
            (minMax[1] + minMax[4]) / 2.0,
            (minMax[2] + minMax[5]) / 2.0);
    }

    private void recenterOrbiterAfterModelChange(
        GizmoNames previousModelName,
        Vector3Dd previousPointOfInterest)
    {
        if ( previousModelName == model.getSolidModelName() ) {
            return;
        }
        if ( model.getSolid() == null ) {
            return;
        }
        if ( !(model.getCameraController() instanceof CameraControllerOrbiter) ) {
            return;
        }

        CameraControllerOrbiter orbiterController =
            (CameraControllerOrbiter)model.getCameraController();
        Vector3Dd previousEye = model.getCamera().getPosition();
        Vector3Dd relativeVector = previousEye.subtract(previousPointOfInterest);
        Vector3Dd newPointOfInterest = calculateSolidCenter();
        Vector3Dd newEye = newPointOfInterest.add(relativeVector);

        orbiterController.setPointOfInterest(newPointOfInterest);
        model.getCamera().setPosition(newEye);
        model.getCamera().setFocusedPositionMaintainingOrthogonality(newPointOfInterest);
    }

    private void toggleFullscreenMode()
    {
        if ( model.getMainFrame() == null ) {
            return;
        }

        if ( isMacOs() ) {
            toggleFullscreenModeMacOs();
            return;
        }

        GraphicsDevice device = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getDefaultScreenDevice();
        JFrame oldFrame = model.getMainFrame();

        if ( !model.isFullScreenMode() ) {
            model.setWindowedBounds(oldFrame.getBounds());
        }
        if ( device.isFullScreenSupported() &&
             device.getFullScreenWindow() == oldFrame ) {
            device.setFullScreenWindow(null);
        }

        oldFrame.setVisible(false);
        oldFrame.dispose();

        model.setFullScreenMode(!model.isFullScreenMode());
        createMainWindow(model.isFullScreenMode());
    }

    private void toggleFullscreenModeMacOs()
    {
        final JFrame frame = model.getMainFrame();
        if ( frame == null ) {
            return;
        }

        if ( !model.isFullScreenMode() ) {
            model.setWindowedBounds(frame.getBounds());
        }

        frame.getRootPane().putClientProperty("apple.awt.fullscreenable", Boolean.TRUE);
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                frame.toFront();
                frame.requestFocus();
                if ( requestMacOsNativeFullScreen(frame) ) {
                    model.setFullScreenMode(!model.isFullScreenMode());
                    joglDebuggerRenderer.refreshCanvasAfterWindowModeChange();
                }
                else {
                    frame.dispose();
                    frame.setUndecorated(!model.isFullScreenMode());
                    if ( model.isFullScreenMode() ) {
                        if ( model.getWindowedBounds() != null ) {
                            frame.setBounds(model.getWindowedBounds());
                        }
                        else {
                            frame.setSize(DEFAULT_WINDOW_SIZE);
                            frame.setLocationRelativeTo(null);
                        }
                    }
                    else {
                        GraphicsDevice device = GraphicsEnvironment
                            .getLocalGraphicsEnvironment()
                            .getDefaultScreenDevice();
                        Rectangle screenBounds = device.getDefaultConfiguration().getBounds();
                        frame.setBounds(screenBounds);
                    }
                    frame.setVisible(true);
                    model.setFullScreenMode(!model.isFullScreenMode());
                    joglDebuggerRenderer.refreshCanvasAfterWindowModeChange();
                }
            }
        });
    }

    private boolean isMacOs()
    {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("mac");
    }

    private boolean requestMacOsNativeFullScreen(JFrame frame)
    {
        try {
            frame.getRootPane().putClientProperty(
                "apple.awt.fullscreenable", Boolean.TRUE);

            try {
                Class<?> fullScreenUtilitiesClass =
                    Class.forName("com.apple.eawt.FullScreenUtilities");
                fullScreenUtilitiesClass.getMethod(
                    "setWindowCanFullScreen", Window.class, boolean.class).invoke(
                        null, frame, true);
            }
            catch ( Throwable e ) {
            }

            Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
            Object application = applicationClass.getMethod("getApplication").invoke(null);

            applicationClass.getMethod("requestToggleFullScreen", Window.class).invoke(
                application, frame);
            return true;
        }
        catch ( Throwable e ) {
            return false;
        }
    }

    private void createMainWindow(boolean fullScreenMode)
    {
        JFrame frame = new JFrame(WINDOW_TITLE);
        model.setMainFrame(frame);

        GLCanvas canvas = createGUI();
        frame.add(canvas, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e)
            {
                shutdownApplication();
            }
        });

        if ( fullScreenMode ) {
            GraphicsDevice device = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();

            if ( isMacOs() ) {
                frame.setUndecorated(false);
                if ( model.getWindowedBounds() != null ) {
                    frame.setBounds(model.getWindowedBounds());
                }
                else {
                    frame.setSize(DEFAULT_WINDOW_SIZE);
                    frame.setLocationRelativeTo(null);
                }
                frame.setVisible(true);
            }
            else if ( device.isFullScreenSupported() ) {
                frame.setUndecorated(true);
                device.setFullScreenWindow(frame);
            }
            else {
                frame.setUndecorated(true);
                frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
                frame.setVisible(true);
            }
        }
        else {
            frame.setUndecorated(false);
            if ( model.getWindowedBounds() != null ) {
                frame.setBounds(model.getWindowedBounds());
            }
            else {
                frame.setSize(DEFAULT_WINDOW_SIZE);
            }
            frame.setVisible(true);
        }

        canvas.requestFocusInWindow();
    }

    private synchronized void shutdownApplication()
    {
        if ( shutdownRequested ) {
            return;
        }
        shutdownRequested = true;

        try {
            if ( model.getCanvas() != null ) {
                model.getCanvas().destroy();
            }
        }
        catch ( Throwable ignored ) {
        }

        try {
            if ( model.getMainFrame() != null ) {
                model.getMainFrame().dispose();
            }
        }
        catch ( Throwable ignored ) {
        }

        System.exit(0);
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        mouseInteractionTechniques.processMouseEntered(model);
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        mouseInteractionTechniques.processMouseExited(model);
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if ( mouseInteractionTechniques.processMousePressed(model, e) ) {
            repaintCanvas();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if ( mouseInteractionTechniques.processMouseReleased(model, e) ) {
            repaintCanvas();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if ( mouseInteractionTechniques.processMouseClicked(model, e) ) {
            repaintCanvas();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        if ( mouseInteractionTechniques.processMouseMoved(model, e) ) {
            repaintCanvas();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        if ( mouseInteractionTechniques.processMouseDragged(model, e) ) {
            repaintCanvas();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        if ( mouseInteractionTechniques.processMouseWheelMoved(model, e) ) {
            repaintCanvas();
        }
    }

    @Override
    public void keyPressed(java.awt.event.KeyEvent e)
    {
        KeyEvent event = AwtSystem.awt2vsdkEvent(e);
        GizmoNames previousModelName = model.getSolidModelName();
        Vector3Dd previousPointOfInterest = new Vector3Dd(0, 0, 0);

        if ( model.getCameraController() instanceof CameraControllerOrbiter ) {
            CameraControllerOrbiter orbiterController =
                (CameraControllerOrbiter)model.getCameraController();
            previousPointOfInterest = orbiterController.getPointOfInterest();
        }

        if ( keyboardInteractionTechniques.processPressed(
                 model, event, new DebuggerKeyboardInteractionTechniques.Actions() {
                     @Override
                     public void requestExit()
                     {
                         InteractiveModelPreviewer.this.shutdownApplication();
                     }

                     @Override
                     public void rebuildSolid()
                     {
                         InteractiveModelPreviewer.this.rebuildSolid();
                     }

                     @Override
                     public void exportCurrentModelStl()
                     {
                         InteractiveModelPreviewer.this.exportCurrentModelStl();
                     }

                     @Override
                     public void toggleFullscreen()
                     {
                         InteractiveModelPreviewer.this.toggleFullscreenMode();
                     }

                     @Override
                     public void requestScreenshot()
                     {
                         InteractiveModelPreviewer.this.joglDebuggerRenderer
                             .requestScreenshot(new File("screenshot.png"));
                         InteractiveModelPreviewer.this.repaintCanvas();
                     }

                     @Override
                     public void requestStlExport()
                     {
                         InteractiveModelPreviewer.this.exportCurrentSolidToStl();
                     }
                 }) ) {
            recenterOrbiterAfterModelChange(
                previousModelName, previousPointOfInterest);
            repaintCanvas();
        }
    }

    @Override
    public void keyReleased(java.awt.event.KeyEvent e)
    {
        KeyEvent event = AwtSystem.awt2vsdkEvent(e);
        if ( model.getCameraController().processKeyReleasedEvent(event) ) {
            repaintCanvas();
        }
    }

    @Override
    public void keyTyped(java.awt.event.KeyEvent e)
    {
    }
}
