import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.render.jogl.Jogl4CameraRenderer;
import vsdk.toolkit.render.jogl.Jogl4ImageRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;
import vsdk.toolkit.render.jogl.Jogl4SphereRenderer;

public class ShadersExample extends JFrame implements
    GLEventListener,
    MouseListener,
    MouseMotionListener,
    MouseWheelListener,
    KeyListener
{
    private ShadersModel model;
    private ShadersKeyboardInteractionTechniques keyboardInteractionTechniques;
    private ShadersMouseInteractionTechniques mouseInteractionTechniques;
    private Animation animation;
    private Timer animationTimer;

    private GLCanvas canvas;

    private boolean closing;
    private boolean glResourcesReleased;

    public ShadersExample()
    {
        super("VITRAL JOGL4 Shaders Example");

        GLProfile profile = GLProfile.get(GLProfile.GL4);
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDepthBits(32);
        canvas = new GLCanvas(caps);

        canvas.addGLEventListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addMouseWheelListener(this);
        canvas.addKeyListener(this);
        canvas.setFocusable(true);

        add(canvas, BorderLayout.CENTER);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e)
            {
                requestClose();
            }
        });

        createModel();
    }

    private void createModel()
    {
        model = ShadersModel.createDefault();
        keyboardInteractionTechniques = new ShadersKeyboardInteractionTechniques();
        mouseInteractionTechniques = new ShadersMouseInteractionTechniques();
        animation = new Animation();
        animationTimer = new Timer(Animation.FRAME_DELAY_MILLIS, e -> {
            animation.tick(model);
            if ( canvas != null ) {
                canvas.repaint();
            }
        });
        animationTimer.setCoalesce(true);
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(1100, 900);
    }

    public static void main(String[] args)
    {
        if ( !Jogl4Renderer.verifyOpenGLAvailability() ) {
            System.out.println("Can not start OpenGL/JOGL.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            ShadersExample app = new ShadersExample();
            app.pack();
            app.setVisible(true);
            app.canvas.requestFocusInWindow();
        });
    }

    @Override
    public void init(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();
        int[] major = new int[1];
        int[] minor = new int[1];
        gl.glGetIntegerv(GL4.GL_MAJOR_VERSION, major, 0);
        gl.glGetIntegerv(GL4.GL_MINOR_VERSION, minor, 0);

        if ( major[0] < 4 || (major[0] == 4 && minor[0] < 1) ) {
            throw new IllegalStateException(
                "ShadersExample requires OpenGL 4.1+. Current context is "
                    + major[0] + "." + minor[0]);
        }
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();

        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

        Matrix4x4 modelRotation = new Matrix4x4().axisRotation(
            model.getSphereRotationAngleRadians(),
            0.0,
            0.0,
            1.0);

        Jogl4SphereRenderer.draw(
            gl,
            model.getSphere(),
            model.getCamera(),
            model.getLight(),
            model.getMaterial(),
            model.getQuality(),
            model.getTextureMap(),
            model.getNormalMapRgb(),
            modelRotation,
            model.getSphereMeridians(),
            model.getSphereParallels());
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();
        if ( !glResourcesReleased ) {
            if ( animationTimer != null ) {
                animationTimer.stop();
            }
            Jogl4SphereRenderer.dispose(gl);
            Jogl4ImageRenderer.unload(gl, model.getTextureMap());
            Jogl4ImageRenderer.unload(gl, model.getNormalMapRgb());
            Jogl4ImageRenderer.dispose(gl);
            Jogl4CameraRenderer.dispose(gl);
            glResourcesReleased = true;
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
        GL4 gl = drawable.getGL().getGL4();
        int surfaceWidth = Math.max(1, drawable.getSurfaceWidth());
        int surfaceHeight = Math.max(1, drawable.getSurfaceHeight());
        gl.glViewport(0, 0, surfaceWidth, surfaceHeight);
        model.getCamera().updateViewportResize(surfaceWidth, surfaceHeight);
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        mouseInteractionTechniques.processMouseEntered(canvas);
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        mouseInteractionTechniques.processMouseExited();
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if ( mouseInteractionTechniques.processMousePressed(model, e) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if ( mouseInteractionTechniques.processMouseReleased(model, e) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if ( mouseInteractionTechniques.processMouseClicked(model, e) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        if ( mouseInteractionTechniques.processMouseMoved(model, e) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        if ( mouseInteractionTechniques.processMouseDragged(model, e) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        if ( mouseInteractionTechniques.processMouseWheelMoved(model, e) ) {
            canvas.repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        vsdk.toolkit.gui.KeyEvent event = AwtSystem.awt2vsdkEvent(e);
        if ( keyboardInteractionTechniques.processPressed(
            event,
            model,
            new ShadersKeyboardInteractionTechniques.Actions() {
                @Override
                public void requestExit()
                {
                    requestClose();
                }

                @Override
                public void reportQuality(RendererConfiguration currentQuality)
                {
                    System.out.println(currentQuality);
                }

                @Override
                public void animationToggled(boolean enabled)
                {
                    if ( enabled ) {
                        animation.reset();
                        animationTimer.start();
                    }
                    else {
                        animationTimer.stop();
                        animation.reset();
                    }
                }
            }) ) {
            canvas.repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        if ( keyboardInteractionTechniques.processReleased(
            AwtSystem.awt2vsdkEvent(e),
            model) ) {
            canvas.repaint();
        }
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
    }

    private void requestClose()
    {
        if ( closing ) {
            return;
        }
        closing = true;

        Runnable shutdown = () -> {
            if ( animationTimer != null ) {
                animationTimer.stop();
            }
            setVisible(false);
            if ( canvas != null ) {
                try {
                    canvas.destroy();
                }
                catch (Throwable ignored) {
                }
            }
            if ( isDisplayable() ) {
                super.dispose();
            }
            System.exit(0);
        };

        if ( SwingUtilities.isEventDispatchThread() ) {
            shutdown.run();
        }
        else {
            SwingUtilities.invokeLater(shutdown);
        }
    }
}
