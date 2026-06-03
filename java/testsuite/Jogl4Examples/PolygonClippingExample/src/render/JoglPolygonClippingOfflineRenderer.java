package render;

import java.io.File;
import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;

import model.PolygonClippingDebuggerModel;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.media.RGBImageUncompressed;

public class JoglPolygonClippingOfflineRenderer implements GLEventListener
{
    private final File outputFile;
    private final int width;
    private final int height;
    private final JoglPolygonClippingRenderer delegate;

    private GLOffscreenAutoDrawable pbuffer;
    private boolean done;

    public JoglPolygonClippingOfflineRenderer(
        PolygonClippingDebuggerModel model,
        File outputFile)
    {
        this(model, outputFile, 1280, 800);
    }

    public JoglPolygonClippingOfflineRenderer(
        PolygonClippingDebuggerModel model,
        File outputFile,
        int width,
        int height)
    {
        if ( model == null ) {
            throw new IllegalArgumentException("model can not be null");
        }
        if ( outputFile == null ) {
            throw new IllegalArgumentException("outputFile can not be null");
        }

        this.outputFile = outputFile;
        this.width = width;
        this.height = height;
        this.delegate = new JoglPolygonClippingRenderer(model);
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

        try {
            pbuffer.display();
        }
        finally {
            if ( pbuffer != null ) {
                pbuffer.destroy();
                pbuffer = null;
            }
        }
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
        GL4 gl = drawable.getGL().getGL4();
        gl.glFinish();

        RGBImageUncompressed image = captureRgbImage(gl, width, height);
        ensureParentFolder(outputFile);
        ImagePersistence.exportPNG(outputFile, image);
        done = true;
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        GL4 gl = drawable.getGL().getGL4();
        delegate.dispose(drawable);
        gl.glFinish();
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
        return GLProfile.get(GLProfile.GL4);
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
