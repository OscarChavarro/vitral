// Java Swing / Awt classes
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;
import javax.swing.JFrame;

// JOGL classes
import com.jogamp.opengl.awt.GLCanvas;

// VitralSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.gui.CameraControllerOrbiter;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.render.jogl.JoglRenderer;

@SuppressWarnings("removal")
public class PolyhedralBoundedSolidExample extends JFrame implements
    MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

    private static final String WINDOW_TITLE =
        "VITRAL concept test - Polyhedral bounded solid example";
    private static final Dimension DEFAULT_WINDOW_SIZE = new Dimension(1024, 768);

    private final DebuggerModel model;
    private final DebuggerKeyboardInteractionTechniques keyboardInteractionTechniques;
    private final DebuggerMouseInteractionTechniques mouseInteractionTechniques;
    private final JoglDebuggerRenderer joglDebuggerRenderer;

    public PolyhedralBoundedSolidExample() {
        model = new DebuggerModel();
        keyboardInteractionTechniques = new DebuggerKeyboardInteractionTechniques();
        mouseInteractionTechniques = new DebuggerMouseInteractionTechniques();
        joglDebuggerRenderer = new JoglDebuggerRenderer(model);

        // Initial solid
        buildSolidWithRecovery();
        recenterOrbiterAfterModelChange(null, new Vector3D(0, 0, 0));
    }

    private GLCanvas createGUI()
    {
        model.canvas = new GLCanvas();
        model.canvas.addGLEventListener(joglDebuggerRenderer);
        model.canvas.addMouseListener(this);
        model.canvas.addMouseMotionListener(this);
        model.canvas.addKeyListener(this);

        return model.canvas;
    }

    public static void main (String[] args) {
        JoglRenderer.verifyOpenGLAvailability();
        PolyhedralBoundedSolidExample instance = new PolyhedralBoundedSolidExample();
        instance.createMainWindow(false);
    }

    private void setMainFrame(JFrame frame)
    {
        model.mainFrame = frame;
    }

    private void rebuildSolid()
    {
        buildSolidWithRecovery();
    }

    private void buildSolidWithRecovery()
    {
        try {
            model.clearErrorState();
            model.solid = PolyhedralBoundedSolidModelingTools.buildSolid(model);
            if ( model.solid == null ) {
                throw new IllegalStateException("Solid builder returned null");
            }
        }
        catch ( Throwable e ) {
            model.setErrorState(formatBuildErrorMessage(e));
        }
    }

    private String formatBuildErrorMessage(Throwable e)
    {
        StringBuilder msg = new StringBuilder();
        msg.append("Build error");
        if ( model.solidModelName != null ) {
            msg.append(" [").append(model.solidModelName.name()).append("]");
        }
        msg.append(": ").append(e.getClass().getSimpleName());
        if ( e.getMessage() != null && !e.getMessage().isEmpty() ) {
            msg.append(" - ").append(e.getMessage());
        }
        return msg.toString();
    }

    private void repaintCanvas()
    {
        if ( model.canvas != null ) {
            model.canvas.repaint();
        }
    }

    private Vector3D calculateSolidCenter()
    {
        if ( model.solid == null ) {
            return new Vector3D(0, 0, 0);
        }

        double[] minMax = model.solid.getMinMax();
        if ( minMax == null || minMax.length < 6 ) {
            return new Vector3D(0, 0, 0);
        }

        return new Vector3D(
            (minMax[0] + minMax[3]) / 2.0,
            (minMax[1] + minMax[4]) / 2.0,
            (minMax[2] + minMax[5]) / 2.0);
    }

    private void recenterOrbiterAfterModelChange(SolidModelNames previousModelName,
                                                 Vector3D previousPointOfInterest)
    {
        if ( previousModelName == model.solidModelName ) {
            return;
        }
        if ( model.solid == null ) {
            return;
        }
        if ( !(model.cameraController instanceof CameraControllerOrbiter) ) {
            return;
        }

        CameraControllerOrbiter orbiterController =
            (CameraControllerOrbiter)model.cameraController;

        Vector3D previousEye = model.camera.getPosition();
        Vector3D relativeVector = previousEye.substract(previousPointOfInterest);
        Vector3D newPointOfInterest = calculateSolidCenter();
        Vector3D newEye = newPointOfInterest.add(relativeVector);

        orbiterController.setPointOfInterest(newPointOfInterest);
        model.camera.setPosition(newEye);
        model.camera.setFocusedPositionMaintainingOrthogonality(newPointOfInterest);
    }

    private void toggleFullscreenMode()
    {
        if ( model.mainFrame == null ) {
            return;
        }

        if ( isMacOs() ) {
            toggleFullscreenModeMacOs();
            return;
        }

        GraphicsDevice device = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getDefaultScreenDevice();
        JFrame oldFrame = model.mainFrame;

        if ( !model.fullScreenMode ) {
            model.windowedBounds = oldFrame.getBounds();
        }
        if ( device.isFullScreenSupported() &&
             device.getFullScreenWindow() == oldFrame ) {
            device.setFullScreenWindow(null);
        }

        oldFrame.setVisible(false);
        oldFrame.dispose();

        model.fullScreenMode = !model.fullScreenMode;
        createMainWindow(model.fullScreenMode);
    }

    private void toggleFullscreenModeMacOs()
    {
        final JFrame frame = model.mainFrame;
        if ( frame == null ) {
            return;
        }

        if ( !model.fullScreenMode ) {
            model.windowedBounds = frame.getBounds();
        }

        frame.getRootPane().putClientProperty("apple.awt.fullscreenable", Boolean.TRUE);
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                frame.toFront();
                frame.requestFocus();
                if ( requestMacOsNativeFullScreen(frame) ) {
                    model.fullScreenMode = !model.fullScreenMode;
                    joglDebuggerRenderer.refreshCanvasAfterWindowModeChange();
                }
                else {
                    // Last-resort fallback on macOS when native full screen API is unavailable.
                    frame.dispose();
                    frame.setUndecorated(!model.fullScreenMode);
                    if ( model.fullScreenMode ) {
                        if ( model.windowedBounds != null ) {
                            frame.setBounds(model.windowedBounds);
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
                    model.fullScreenMode = !model.fullScreenMode;
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
                // Optional macOS helper; keep going with Application API path.
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
        setMainFrame(frame);

        GLCanvas canvas = createGUI();
        frame.add(canvas, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        if ( fullScreenMode ) {
            GraphicsDevice device = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();

            if ( isMacOs() ) {
                frame.setUndecorated(false);
                if ( model.windowedBounds != null ) {
                    frame.setBounds(model.windowedBounds);
                }
                else {
                    frame.setSize(DEFAULT_WINDOW_SIZE);
                    frame.setLocationRelativeTo(null);
                }
                frame.setVisible(true);
                // macOS full screen should be toggled over the same visible frame.
                // See `toggleFullscreenModeMacOs`.
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
            if ( model.windowedBounds != null ) {
                frame.setBounds(model.windowedBounds);
            }
            else {
                //Dimension size = new Dimension(1366, 768);
                //frame.setMinimumSize(size);
                frame.setSize(DEFAULT_WINDOW_SIZE);
            }
            frame.setVisible(true);
        }

        canvas.requestFocusInWindow();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mouseInteractionTechniques.processMouseEntered(model);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseInteractionTechniques.processMouseExited(model);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if ( mouseInteractionTechniques.processMousePressed(model, e) ) {
            repaintCanvas();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if ( mouseInteractionTechniques.processMouseReleased(model, e) ) {
            repaintCanvas();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if ( mouseInteractionTechniques.processMouseClicked(model, e) ) {
            repaintCanvas();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if ( mouseInteractionTechniques.processMouseMoved(model, e) ) {
            repaintCanvas();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if ( mouseInteractionTechniques.processMouseDragged(model, e) ) {
            repaintCanvas();
        }
    }

    /**
    @param e
    */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if ( mouseInteractionTechniques.processMouseWheelMoved(model, e) ) {
            repaintCanvas();
        }
    }

    @Override
    public void keyPressed(java.awt.event.KeyEvent e) {
        KeyEvent event = AwtSystem.awt2vsdkEvent(e);
        SolidModelNames previousModelName = model.solidModelName;
        Vector3D previousPointOfInterest = new Vector3D(0, 0, 0);
        if ( model.cameraController instanceof CameraControllerOrbiter ) {
            CameraControllerOrbiter orbiterController =
                (CameraControllerOrbiter)model.cameraController;
            previousPointOfInterest = orbiterController.getPointOfInterest();
        }

        if ( keyboardInteractionTechniques.processPressed(
                 model, event, new DebuggerKeyboardInteractionTechniques.Actions() {
                     @Override
                     public void requestExit() {
                         System.exit(0);
                     }

                     @Override
                     public void rebuildSolid() {
                         PolyhedralBoundedSolidExample.this.rebuildSolid();
                     }

                     @Override
                     public void toggleFullscreen() {
                         PolyhedralBoundedSolidExample.this.toggleFullscreenMode();
                     }
                 }) ) {
            recenterOrbiterAfterModelChange(
                previousModelName, previousPointOfInterest);
            repaintCanvas();
        }
    }

    @Override
    public void keyReleased(java.awt.event.KeyEvent e) {
        KeyEvent event = AwtSystem.awt2vsdkEvent(e);
        if ( model.cameraController.processKeyReleasedEvent(event) ) {
            repaintCanvas();
        }
    }

    /**
    Do NOT call your controller from the `keyTyped` method, or the controller
    will be invoked twice for
    @param e each key. Call it only from the `keyPressed` and
    `keyReleased` method
    */
    @Override
    public void keyTyped(java.awt.event.KeyEvent e) {
    }

}
