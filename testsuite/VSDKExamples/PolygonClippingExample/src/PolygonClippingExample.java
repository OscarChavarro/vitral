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
import javax.swing.JFrame;

import com.jogamp.opengl.awt.GLCanvas;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.render.jogl.JoglRenderer;

@SuppressWarnings("removal")
public class PolygonClippingExample extends JFrame implements MouseListener,
    MouseMotionListener, MouseWheelListener, KeyListener
{
    private static final String WINDOW_TITLE =
        "VITRAL concept test - Polygon clipping example";
    private static final Dimension DEFAULT_WINDOW_SIZE =
        new Dimension(1280, 800);

    private final PolygonClippingDebuggerModel model;
    private final PolygonClippingKeyboardInteractionTechniques
        keyboardInteractionTechniques;
    private final PolygonClippingMouseInteractionTechniques
        mouseInteractionTechniques;
    private final JoglPolygonClippingRenderer renderer;

    public PolygonClippingExample()
    {
        model = new PolygonClippingDebuggerModel();
        keyboardInteractionTechniques =
            new PolygonClippingKeyboardInteractionTechniques();
        mouseInteractionTechniques =
            new PolygonClippingMouseInteractionTechniques();
        renderer = new JoglPolygonClippingRenderer(model);

        VSDK.setWithSystemExit(false);
        VSDK.setWithFatalExceptions(true);

        rebuildScene();
        focusCameraOnCurrentScene();
    }

    public static void main(String[] args)
    {
        JoglRenderer.verifyOpenGLAvailability();
        PolygonClippingExample instance = new PolygonClippingExample();
        instance.createMainWindow(false);
    }

    private void rebuildScene()
    {
        try {
            model.clearErrorState();
            PolygonClippingModelingTools.rebuildScene(model);
        }
        catch ( Throwable e ) {
            model.setErrorState(formatBuildErrorMessage(e));
        }
    }

    private void focusCameraOnCurrentScene()
    {
        Vector3D center = PolygonClippingModelingTools.calculateSceneCenter(model);
        model.camera.setFocusedPositionMaintainingOrthogonality(center);
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
        model.canvas = new GLCanvas();
        model.canvas.addGLEventListener(renderer);
        model.canvas.addMouseListener(this);
        model.canvas.addMouseMotionListener(this);
        model.canvas.addMouseWheelListener(this);
        model.canvas.addKeyListener(this);
        return model.canvas;
    }

    private void repaintCanvas()
    {
        if ( model.canvas != null ) {
            model.canvas.repaint();
        }
    }

    private void setMainFrame(JFrame frame)
    {
        model.mainFrame = frame;
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
            }
            else if ( device.isFullScreenSupported() ) {
                frame.setUndecorated(true);
                device.setFullScreenWindow(frame);
            }
            else {
                frame.setUndecorated(true);
                frame.setExtendedState(frame.getExtendedState()
                    | JFrame.MAXIMIZED_BOTH);
                frame.setVisible(true);
            }
        }
        else {
            frame.setUndecorated(false);
            if ( model.windowedBounds != null ) {
                frame.setBounds(model.windowedBounds);
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
        if ( device.isFullScreenSupported()
             && device.getFullScreenWindow() == oldFrame ) {
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

        frame.getRootPane().putClientProperty("apple.awt.fullscreenable",
            Boolean.TRUE);
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                frame.toFront();
                frame.requestFocus();
                if ( requestMacOsNativeFullScreen(frame) ) {
                    model.fullScreenMode = !model.fullScreenMode;
                    renderer.refreshCanvasAfterWindowModeChange();
                }
                else {
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
                        Rectangle screenBounds =
                            device.getDefaultConfiguration().getBounds();
                        frame.setBounds(screenBounds);
                    }
                    frame.setVisible(true);
                    model.fullScreenMode = !model.fullScreenMode;
                    renderer.refreshCanvasAfterWindowModeChange();
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
            frame.getRootPane().putClientProperty("apple.awt.fullscreenable",
                Boolean.TRUE);

            try {
                Class<?> fullScreenUtilitiesClass =
                    Class.forName("com.apple.eawt.FullScreenUtilities");
                fullScreenUtilitiesClass.getMethod("setWindowCanFullScreen",
                    Window.class, boolean.class).invoke(null, frame, true);
            }
            catch ( Throwable e ) {
            }

            Class<?> applicationClass =
                Class.forName("com.apple.eawt.Application");
            Object application =
                applicationClass.getMethod("getApplication").invoke(null);
            applicationClass.getMethod("requestToggleFullScreen",
                Window.class).invoke(application, frame);
            return true;
        }
        catch ( Throwable e ) {
            return false;
        }
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
                    model.takeSnapshot = true;
                }
            }) ) {
            repaintCanvas();
        }
    }

    @Override
    public void keyReleased(java.awt.event.KeyEvent e)
    {
        vsdk.toolkit.gui.KeyEvent event =
            vsdk.toolkit.gui.AwtSystem.awt2vsdkEvent(e);
        if ( model.cameraController.processKeyReleasedEvent(event) ) {
            repaintCanvas();
        }
        if ( model.qualityController.processKeyReleasedEvent(event) ) {
            repaintCanvas();
        }
    }

    @Override
    public void keyTyped(java.awt.event.KeyEvent e)
    {
    }
}
