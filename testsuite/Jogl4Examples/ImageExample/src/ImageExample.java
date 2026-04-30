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
import java.io.File;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.fixtures.Jogl4SimpleCorridorSample;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.gui.CameraController;
import vsdk.toolkit.gui.CameraControllerAquynza;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBAImageCompressed;
import vsdk.toolkit.media.RGBAImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4CameraRenderer;
import vsdk.toolkit.render.jogl.Jogl4ImageRenderer;
import vsdk.toolkit.render.jogl.Jogl4MatrixRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

public class ImageExample extends JFrame implements
    GLEventListener,
    MouseListener,
    MouseMotionListener,
    MouseWheelListener,
    KeyListener
{
    private static final int DEPTH_BUFFER_BITS = 64;
    private static final float IMAGE_DEPTH_BIAS_FACTOR = -1.0f;
    private static final float IMAGE_DEPTH_BIAS_UNITS = -8.0f;

    private Camera camera;
    private CameraController cameraController;
    private GLCanvas canvas;
    private Jogl4SimpleCorridorSample corridor;
    private Image renderImage;
    private Image earthImage;

    private boolean closing;
    private boolean glResourcesReleased;

    public ImageExample()
    {
        super("VITRAL concept test - JOGL4 Image use example");

        GLProfile profile = GLProfile.get(GLProfile.GL4);
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDepthBits(DEPTH_BUFFER_BITS);
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

        camera = new Camera();
        cameraController = new CameraControllerAquynza(camera);

        corridor = new Jogl4SimpleCorridorSample();

        renderImage = loadImage("../../../etc/images/render.jpg");
        earthImage = loadImage("../../../etc/textures/earth.dds");
    }

    private Image loadImage(String imageFilename)
    {
        try {
            return ImagePersistence.importRGB(new File(imageFilename));
        }
        catch (Exception e) {
            System.err.println("Error: could not read image file \"" + imageFilename + "\".");
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return null;
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(1024, 768);
    }

    public static void main(String[] args)
    {
        if ( !Jogl4Renderer.verifyOpenGLAvailability() ) {
            System.out.println("Can not start OpenGL/JOGL.");
            return;
        }

        ImageExample f = new ImageExample();
        f.pack();
        f.setVisible(true);
        f.canvas.requestFocusInWindow();
    }

    private void drawTexturedPolygon(
        GL4 gl,
        Matrix4x4 projection,
        Image image,
        float x0,
        float y0,
        float width,
        float height)
    {
        int textureId = Jogl4ImageRenderer.activate(gl, image);
        if ( textureId <= 0 ) {
            return;
        }

        float x1 = x0 + width;
        float y1 = y0 + height;

        float[] positions = {
            x0, y0, 0.01f,
            x1, y0, 0.01f,
            x1, y1, 0.01f,
            x0, y0, 0.01f,
            x1, y1, 0.01f,
            x0, y1, 0.01f
        };

        float[] uvCoordinates = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        };

        gl.glDisable(GL4.GL_CULL_FACE);
        gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);

        Jogl4ImageRenderer.drawTexturedQuad(
            gl,
            textureId,
            projection,
            positions,
            uvCoordinates,
            1.0f,
            1.0f,
            1.0f);
    }

    private void drawWorldImages(GL4 gl, Matrix4x4 projection)
    {
        float renderWidth = (float)renderImage.getXSize() / (float)renderImage.getYSize();

        drawTexturedPolygon(gl, projection, renderImage, 0.0f, 0.0f, renderWidth, 1.0f);
        drawTexturedPolygon(gl, projection, earthImage, 0.0f, -1.0f, 1.0f, 1.0f);
    }

    private void drawWorldImagesDepthBiased(GL4 gl, Matrix4x4 projection)
    {
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDepthFunc(GL4.GL_LEQUAL);
        gl.glEnable(GL4.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(IMAGE_DEPTH_BIAS_FACTOR, IMAGE_DEPTH_BIAS_UNITS);

        drawWorldImages(gl, projection);

        gl.glDisable(GL4.GL_POLYGON_OFFSET_FILL);
        gl.glDepthFunc(GL4.GL_LESS);
    }

    private void drawHudImage(GL4 gl, Image image, boolean upperLeft)
    {
        int textureId = Jogl4ImageRenderer.activate(gl, image);
        if ( textureId <= 0 ) {
            return;
        }

        int[] viewport = new int[4];
        gl.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);
        int viewportWidth = Math.max(viewport[2], 1);
        int viewportHeight = Math.max(viewport[3], 1);

        float width = 2.0f * ((float)image.getXSize() / (float)viewportWidth);
        float height = 2.0f * ((float)image.getYSize() / (float)viewportHeight);

        float x0 = -1.0f;
        float y0 = upperLeft ? 1.0f - height : -1.0f;
        float x1 = x0 + width;
        float y1 = y0 + height;

        float[] positions = {
            x0, y0, 0.0f,
            x1, y0, 0.0f,
            x1, y1, 0.0f,
            x0, y0, 0.0f,
            x1, y1, 0.0f,
            x0, y1, 0.0f
        };

        float[] uvCoordinates = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        };

        gl.glDisable(GL4.GL_CULL_FACE);
        if ( image instanceof RGBAImageUncompressed ||
             image instanceof RGBAImageCompressed ) {
            gl.glEnable(GL4.GL_BLEND);
            gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
        }

        Jogl4ImageRenderer.drawTexturedQuad(
            gl,
            textureId,
            Matrix4x4.identityMatrix(),
            positions,
            uvCoordinates,
            1.0f,
            1.0f,
            1.0f);

        if ( image instanceof RGBAImageUncompressed ||
             image instanceof RGBAImageCompressed ) {
            gl.glDisable(GL4.GL_BLEND);
        }
    }

    private void drawHud(GL4 gl)
    {
        gl.glDisable(GL4.GL_DEPTH_TEST);
        drawHudImage(gl, renderImage, false);
        drawHudImage(gl, earthImage, true);
        gl.glEnable(GL4.GL_DEPTH_TEST);
    }

    private void drawObjectsGL(GL4 gl)
    {
        Matrix4x4 projection = Jogl4CameraRenderer.activate(gl, camera);

        corridor.drawGL(gl, projection);
        Jogl4MatrixRenderer.draw(gl, projection, Matrix4x4.identityMatrix());
        drawWorldImagesDepthBiased(gl, projection);
        drawHud(gl);
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();

        gl.glDisable(GL4.GL_BLEND);
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

        drawObjectsGL(gl);
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
                "ImageExample requires OpenGL 4.1+. Current context is "
                    + major[0] + "." + minor[0]);
        }
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();

        if ( !glResourcesReleased ) {
            Jogl4ImageRenderer.unload(gl, renderImage);
            Jogl4ImageRenderer.unload(gl, earthImage);
            corridor.dispose(gl);
            Jogl4CameraRenderer.dispose(gl);
            Jogl4ImageRenderer.dispose(gl);
            glResourcesReleased = true;
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
        GL4 gl = drawable.getGL().getGL4();
        gl.glViewport(0, 0, width, height);
        camera.updateViewportResize(width, height);
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        canvas.requestFocusInWindow();
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if ( cameraController.processMousePressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if ( cameraController.processMouseReleasedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if ( cameraController.processMouseClickedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        if ( cameraController.processMouseMovedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        if ( cameraController.processMouseDraggedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        if ( cameraController.processMouseWheelEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) {
            requestClose();
            return;
        }

        if ( cameraController.processKeyPressedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
            canvas.repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        if ( cameraController.processKeyReleasedEvent(AwtSystem.awt2vsdkEvent(e)) ) {
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
            setVisible(false);

            if ( canvas != null ) {
                try {
                    canvas.destroy();
                }
                catch (Throwable t) {
                    // If the AWT peer was already removed, continue shutdown.
                }
            }

            disposeWindow();
            System.exit(0);
        };

        if ( SwingUtilities.isEventDispatchThread() ) {
            shutdown.run();
        }
        else {
            SwingUtilities.invokeLater(shutdown);
        }
    }

    private void disposeWindow()
    {
        if ( isDisplayable() ) {
            super.dispose();
        }
    }
}
