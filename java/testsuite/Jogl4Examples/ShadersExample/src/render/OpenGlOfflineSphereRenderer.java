package render;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;

import model.ShadersModel;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.render.jogl.Jogl4SphereRenderer;

public class OpenGlOfflineSphereRenderer
{
    public RGBImageUncompressed render(
        ShadersModel model,
        Matrix4x4d modelRotation,
        int width,
        int height)
    {
        GLProfile profile = GLProfile.get(GLProfile.GL4);
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDoubleBuffered(false);
        GLOffscreenAutoDrawable pbuffer = GLDrawableFactory
            .getFactory(profile)
            .createOffscreenAutoDrawable(null, caps, null, width, height);

        OfflineRendererListener listener = new OfflineRendererListener(
            model,
            modelRotation,
            width,
            height);
        pbuffer.addGLEventListener(listener);
        pbuffer.display();
        pbuffer.destroy();
        return listener.getCapturedImage();
    }

    private static final class OfflineRendererListener implements GLEventListener
    {
        private final ShadersModel model;
        private final Matrix4x4d modelRotation;
        private final int width;
        private final int height;
        private RGBImageUncompressed capturedImage;

        private OfflineRendererListener(
            ShadersModel model,
            Matrix4x4d modelRotation,
            int width,
            int height)
        {
            this.model = model;
            this.modelRotation = modelRotation;
            this.width = width;
            this.height = height;
        }

        @Override
        public void init(GLAutoDrawable drawable)
        {
        }

        @Override
        public void display(GLAutoDrawable drawable)
        {
            GL4 gl = drawable.getGL().getGL4();
            gl.glViewport(0, 0, width, height);
            gl.glEnable(GL4.GL_DEPTH_TEST);
            gl.glClearColor(0, 0, 0, 1);
            gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

            Jogl4SphereRenderer.draw(
                gl,
                model.getSphere(),
                model.getCamera(),
                model.getLight(),
                model.getActiveMaterialForCurrentShading(),
                model.getQuality(),
                model.getTextureMap(),
                model.getBumpMapHeightRgb(),
                modelRotation,
                model.getSphereMeridians(),
                model.getSphereParallels());

            gl.glFinish();
            capturedImage = captureRgbImage(gl, width, height);
        }

        @Override
        public void dispose(GLAutoDrawable drawable)
        {
            GL4 gl = drawable.getGL().getGL4();
            Jogl4SphereRenderer.dispose(gl);
        }

        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
        {
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

        private RGBImageUncompressed getCapturedImage()
        {
            return capturedImage;
        }
    }
}
