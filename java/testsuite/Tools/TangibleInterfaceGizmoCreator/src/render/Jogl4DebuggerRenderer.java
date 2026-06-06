package render;

import java.awt.EventQueue;
import java.io.File;
import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLEventListener;

import models.TangibleInterfaceGizmosModel;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4LineRenderer;
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

    private void drawReferenceFrame(GL4 gl, Matrix4x4d mvp)
    {
        if ( !model.isShowCoordinateSystem() ) {
            return;
        }
        float[] positions = new float[] {
            0, 0, 0, 1, 0, 0,
            0, 0, 0, 0, 1, 0,
            0, 0, 0, 0, 0, 1
        };
        float[] colors = new float[] {
            1, 0, 0, 1, 0, 0,
            0, 1, 0, 0, 1, 0,
            0, 0, 1, 0, 0, 1
        };
        Jogl4LineRenderer.drawLines(gl, mvp, positions, colors, 3.0f, -3.0e-4f);
    }

    private void drawObjectsGL(GL4 gl)
    {
        if ( model.getSolid() == null ) {
            return;
        }
        Matrix4x4d modelMatrix = Matrix4x4d.identityMatrix();
        Matrix4x4d mvp = model.getCamera().calculateProjectionMatrix()
            .multiply(modelMatrix);

        Jogl4SimpleMaterialRenderer.activate(gl, model.getMaterial());
        Jogl4LightRenderer.activate(gl, model.getLight1());
        Jogl4LightRenderer.draw(gl, model.getLight1(), model.getCamera());
        Jogl4LightRenderer.activate(gl, model.getLight2());
        Jogl4LightRenderer.draw(gl, model.getLight2(), model.getCamera());
        Jogl4PolyhedralBoundedSolidRenderer.draw(gl, model.getSolid(),
            model.getCamera(), model.getQuality(), modelMatrix);

        drawReferenceFrame(gl, mvp);
        Jogl4PolyhedralBoundedSolidRenderer.drawDebugFaceBoundary(gl,
            model.getSolid(), model.getFaceIndex(), mvp);
        Jogl4PolyhedralBoundedSolidRenderer.drawDebugFace(gl, model.getSolid(),
            model.getFaceIndex(), modelMatrix, mvp, model.getCamera());
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();

        gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL4.GL_DEPTH_TEST);
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
        GL4 gl = drawable.getGL().getGL4();
        gl.glViewport(0, 0, width, height);
        model.getCamera().updateViewportResize(width, height);
        hudRenderer.updateViewportSize(width, height);
    }

    private void exportPendingScreenshot(GL4 gl, int width, int height)
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

    private static RGBImageUncompressed captureRgbImage(GL4 gl, int width, int height)
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
