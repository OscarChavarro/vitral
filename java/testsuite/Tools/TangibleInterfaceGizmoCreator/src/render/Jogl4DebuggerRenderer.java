package render;

import java.awt.EventQueue;
import java.io.File;
import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import models.TangibleInterfaceGizmosModel;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl2CameraRenderer;
import vsdk.toolkit.render.jogl.Jogl4LightRenderer;
import vsdk.toolkit.render.jogl.Jogl4PolyhedralBoundedSolidRenderer;
import vsdk.toolkit.render.jogl.Jogl4SimpleMaterialRenderer;

public class Jogl4DebuggerRenderer implements GLEventListener
{
    private final TangibleInterfaceGizmosModel model;
    private final Jogl4DebuggerHudRenderer hudRenderer;
    private File pendingScreenshotFile;

    public Jogl4DebuggerRenderer(TangibleInterfaceGizmosModel model)
    {
        this.model = model;
        this.hudRenderer = new Jogl4DebuggerHudRenderer(model);
        this.pendingScreenshotFile = null;
    }

    public void requestScreenshot(File outputFile)
    {
        pendingScreenshotFile = outputFile;
    }

    public void refreshCanvasAfterWindowModeChange()
    {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                if ( model.getCanvas() == null ) {
                    return;
                }
                if ( model.getMainFrame() != null ) {
                    model.getMainFrame().validate();
                    model.getMainFrame().repaint();
                }
                model.getCanvas().revalidate();
                model.getCanvas().repaint();
                model.getCanvas().display();
                model.getCanvas().requestFocusInWindow();
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run()
                    {
                        if ( model.getCanvas() != null ) {
                            model.getCanvas().display();
                        }
                    }
                });
            }
        });
    }

    private void drawReferenceFrame(GL2 gl)
    {
        if ( !model.isShowCoordinateSystem() ) {
            return;
        }
        gl.glBegin(GL2.GL_LINES);
            gl.glColor3d(1, 0, 0);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(1, 0, 0);

            gl.glColor3d(0, 1, 0);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0, 1, 0);

            gl.glColor3d(0, 0, 1);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0, 0, 1);
        gl.glEnd();
    }

    private void drawObjectsGL(GL2 gl)
    {
        gl.glLoadIdentity();
        if ( model.getSolid() == null ) {
            return;
        }

        gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(4.0f, 4.0f);
        Jogl4SimpleMaterialRenderer.activate(gl, model.getMaterial());
        Jogl4LightRenderer.activate(gl, model.getLight1());
        Jogl4LightRenderer.draw(gl, model.getLight1(), model.getCamera());
        Jogl4LightRenderer.activate(gl, model.getLight2());
        Jogl4LightRenderer.draw(gl, model.getLight2(), model.getCamera());
        gl.glEnable(GL2.GL_LIGHTING);
        Jogl4PolyhedralBoundedSolidRenderer.draw(gl, model.getSolid(),
            model.getCamera(), model.getQuality());
        gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glLineWidth(3.0f);
        gl.glEnable(GL2.GL_POLYGON_OFFSET_LINE);
        gl.glPolygonOffset(2.0f, 2.0f);
        drawReferenceFrame(gl);
        Jogl4PolyhedralBoundedSolidRenderer.drawDebugFaceBoundary(gl,
            model.getSolid(), -2);
        gl.glDisable(GL2.GL_POLYGON_OFFSET_LINE);
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);
        gl.glEnable(GL2.GL_DEPTH_TEST);

        Jogl2CameraRenderer.activate(gl, model.getCamera());
        drawObjectsGL(gl);
        hudRenderer.draw(drawable);
        exportPendingScreenshot(gl, drawable.getSurfaceWidth(),
            drawable.getSurfaceHeight());
    }

    @Override
    public void init(GLAutoDrawable drawable)
    {
        hudRenderer.init(drawable);
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        hudRenderer.dispose(drawable);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
        model.getCamera().updateViewportResize(width, height);
        hudRenderer.updateViewportSize(width, height);
    }

    private void exportPendingScreenshot(GL2 gl, int width, int height)
    {
        File outputFile = pendingScreenshotFile;
        if ( outputFile == null || width <= 0 || height <= 0 ) {
            return;
        }

        pendingScreenshotFile = null;
        gl.glFinish();
        RGBImageUncompressed image = captureRgbImage(gl, width, height);
        ensureParentFolder(outputFile);
        ImagePersistence.exportPNG(outputFile, image);
        System.out.println("[TangibleInterfaceGizmoCreator] Exported " +
            outputFile.getPath());
    }

    private static RGBImageUncompressed captureRgbImage(GL2 gl, int width, int height)
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(3 * width * height);
        gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels(0, 0, width, height, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, bb);

        RGBImageUncompressed image = new RGBImageUncompressed();
        image.init(width, height);

        int pos = 0;
        for ( int y = image.getYSize() - 1; y >= 0; y-- ) {
            for ( int x = 0; x < image.getXSize(); x++ ) {
                image.putPixel(x, y, bb.get(pos), bb.get(pos + 1), bb.get(pos + 2));
                pos += 3;
            }
        }
        return image;
    }

    private static void ensureParentFolder(File outputFile)
    {
        File parent = outputFile.getParentFile();
        if ( parent != null && !parent.exists() ) {
            parent.mkdirs();
        }
    }
}
