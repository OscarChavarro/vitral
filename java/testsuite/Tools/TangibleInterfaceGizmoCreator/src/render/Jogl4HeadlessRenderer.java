package render;

import java.io.File;
import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;

import models.TangibleInterfaceGizmosModel;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.RGBImageUncompressed;

public class Jogl4HeadlessRenderer implements GLEventListener
{
    private final TangibleInterfaceGizmosModel model;
    private final File outputFile;
    private final int width;
    private final int height;
    private final Jogl4DebuggerRenderer delegate;
    private GLOffscreenAutoDrawable pbuffer;
    private boolean done;

    public Jogl4HeadlessRenderer(TangibleInterfaceGizmosModel model, File outputFile)
    {
        this(model, outputFile, 1024, 768);
    }

    public Jogl4HeadlessRenderer(
        TangibleInterfaceGizmosModel model,
        File outputFile,
        int width,
        int height)
    {
        this.model = model;
        this.outputFile = outputFile;
        this.width = width;
        this.height = height;
        this.delegate = new Jogl4DebuggerRenderer(model);
        this.done = false;
    }

    public void render()
    {
        GLProfile profile = pickCompatibleProfile();
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDoubleBuffered(false);
        caps.setDepthBits(64);

        GLDrawableFactory creator = GLDrawableFactory.getFactory(profile);
        pbuffer = creator.createOffscreenAutoDrawable(
            null, caps, null, width, height);
        pbuffer.addGLEventListener(this);
        pbuffer.display();
    }

    @Override
    public void init(GLAutoDrawable drawable)
    {
        delegate.init(drawable);
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        if ( done ) {
            return;
        }
        delegate.display(drawable);
        GL2 gl = drawable.getGL().getGL2();
        gl.glFinish();

        RGBImageUncompressed image = captureRgbImage(gl, width, height);
        ensureParentFolder(outputFile);
        ImagePersistence.exportPNG(outputFile, image);
        System.out.println("[TangibleInterfaceGizmoCreator] Exported " +
            outputFile.getPath());

        done = true;
        pbuffer.destroy();
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        delegate.dispose(drawable);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
        delegate.reshape(drawable, x, y, width, height);
    }

    private static GLProfile pickCompatibleProfile()
    {
        if ( GLProfile.isAvailable(GLProfile.GL4bc) ) {
            return GLProfile.get(GLProfile.GL4bc);
        }
        if ( GLProfile.isAvailable(GLProfile.GL2) ) {
            return GLProfile.get(GLProfile.GL2);
        }
        return GLProfile.get(GLProfile.GL4);
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
