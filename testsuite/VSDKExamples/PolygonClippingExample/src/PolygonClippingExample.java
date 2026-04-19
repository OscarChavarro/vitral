// AWT/Swing classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
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
import javax.swing.JFrame;
import javax.swing.WindowConstants;

// JOGL classes
import com.jogamp.opengl.awt.GLCanvas;

// Vitral classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.gui.CameraControllerOrbiter;
import vsdk.toolkit.render.jogl.JoglRenderer;

public class PolygonClippingExample extends JFrame implements MouseListener,
    MouseMotionListener, MouseWheelListener, KeyListener
{
    private static final String WINDOW_TITLE = "VITRAL concept test - Polygon clipping example";
    private static final Dimension DEFAULT_WINDOW_SIZE = new Dimension(1280, 800);

    private final transient PolygonClippingDebuggerModel model;
    private final transient PolygonClippingKeyboardInteractionTechniques keyboardInteractionTechniques;
    private final transient PolygonClippingMouseInteractionTechniques mouseInteractionTechniques;
    private final transient JoglPolygonClippingRenderer renderer;
    private transient GLCanvas canvas;

    public PolygonClippingExample()
    {
        model = new PolygonClippingDebuggerModel();
        keyboardInteractionTechniques = new PolygonClippingKeyboardInteractionTechniques();
        mouseInteractionTechniques = new PolygonClippingMouseInteractionTechniques();
        renderer = new JoglPolygonClippingRenderer(model);
        canvas = null;

        VSDK.setWithSystemExit(false);
        VSDK.setWithFatalExceptions(true);

        rebuildScene();
        focusCameraOnCurrentScene();
    }

    public static void main(String[] args)
    {
        if (!JoglRenderer.verifyOpenGLAvailability()) {
            System.err.println("Can not open OpenGL context. Check graphics configuration");
            System.exit(0);
        }
        PolygonClippingExample instance = new PolygonClippingExample();
        instance.createMainWindow(false);
    }

    private void rebuildScene()
    {
        try {
            model.clearErrorState();
            PolygonClippingModelingTools.rebuildScene(model);
        }
        catch ( Exception e ) {
            model.setErrorState(formatBuildErrorMessage(e));
        }
    }

    private void focusCameraOnCurrentScene()
    {
        Vector3D center = PolygonClippingModelingTools.calculateSceneCenter(model);
        Vector3D eye = model.getCamera().getPosition();
        Vector3D focus = model.getCamera().getFocusedPosition();
        double distance = eye.subtract(focus).length();
        if ( distance < VSDK.EPSILON ) {
            distance = 20.0;
        }
        model.getCamera().setPosition(new Vector3D(center.x(), center.y() - distance, center.z()));
        model.getCamera().setFocusedPositionMaintainingOrthogonality(center);
        model.getCamera().setUpMaintainingOrthogonality(new Vector3D(0, 0, 1));
        if ( model.getCameraController() instanceof CameraControllerOrbiter orbiter ) {
            orbiter.setPointOfInterest(center);
        }
    }

    private String formatBuildErrorMessage(Throwable e)
    {
        return "Build error [" + model.getCurrentTestCase().name() + "]: "
            + e.getClass().getSimpleName()
            + (e.getMessage() != null && !e.getMessage().isEmpty()
                ? " - " + e.getMessage()
                : "");
    }

    private GLCanvas createGUI()
    {
        canvas = new GLCanvas();
        canvas.addGLEventListener(renderer);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addMouseWheelListener(this);
        canvas.addKeyListener(this);
        return canvas;
    }

    private void repaintCanvas()
    {
        if ( canvas != null ) {
            canvas.repaint();
        }
    }

    private void setMainFrame(JFrame frame)
    {
        model.setMainFrame(frame);
    }

    private void createMainWindow(boolean fullScreenMode)
    {
        JFrame frame = new JFrame(WINDOW_TITLE);
        setMainFrame(frame);

        canvas = createGUI();
        frame.add(canvas, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

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
                frame.setExtendedState(frame.getExtendedState()
                    | Frame.MAXIMIZED_BOTH);
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
                frame.setLocationRelativeTo(null);
            }
            frame.setVisible(true);
        }

        canvas.requestFocusInWindow();
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
        if ( device.isFullScreenSupported()
             && device.getFullScreenWindow() == oldFrame ) {
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
        EventQueue.invokeLater(() -> {
            frame.toFront();
            frame.requestFocus();
            if ( !requestMacOsNativeFullScreen(frame) ) {
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
                    Rectangle screenBounds =
                        device.getDefaultConfiguration().getBounds();
                    frame.setBounds(screenBounds);
                }
                frame.setVisible(true);
            }
            model.setFullScreenMode(!model.isFullScreenMode());
            renderer.refreshCanvasAfterWindowModeChange(canvas);
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
            frame.getRootPane().putClientProperty("apple.awt.fullscreenable",
                Boolean.TRUE);

            Class<?> fullScreenUtilitiesClass =
                Class.forName("com.apple.eawt.FullScreenUtilities");
            fullScreenUtilitiesClass.getMethod("setWindowCanFullScreen",
                Window.class, boolean.class).invoke(null, frame, true);

            Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
            Object application = applicationClass.getMethod("getApplication").invoke(null);
            applicationClass.getMethod("requestToggleFullScreen", Window.class).invoke(application, frame);
            return true;
        }
        catch ( Exception e ) {
            return false;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        mouseInteractionTechniques.processMouseEntered(canvas);
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
        vsdk.toolkit.gui.KeyEvent event =
            vsdk.toolkit.gui.AwtSystem.awt2vsdkEvent(e);

        if ( keyboardInteractionTechniques.processPressed(model, event,
            new PolygonClippingKeyboardInteractionTechniques.Actions() {
                @Override
                public void requestExit()
                {
                    System.exit(0);
                }

                @Override
                public void rebuildScene()
                {
                    PolygonClippingExample.this.rebuildScene();
                    PolygonClippingExample.this.focusCameraOnCurrentScene();
                }

                @Override
                public void toggleFullscreen()
                {
                    PolygonClippingExample.this.toggleFullscreenMode();
                }

                @Override
                public void requestSnapshot()
                {
                    model.setTakeSnapshot(true);
                }
            }) ) {
            repaintCanvas();
        }
    }

    @Override
    public void keyReleased(java.awt.event.KeyEvent e)
    {
        vsdk.toolkit.gui.KeyEvent event = vsdk.toolkit.gui.AwtSystem.awt2vsdkEvent(e);
        if ( model.getCameraController().processKeyReleasedEvent(event) ) {
            repaintCanvas();
        }
        if ( model.getQualityController().processKeyReleasedEvent(event) ) {
            repaintCanvas();
        }
    }

    @Override
    public void keyTyped(java.awt.event.KeyEvent e)
    {
        // Intentionally empty: handled via key press/release
    }
}
