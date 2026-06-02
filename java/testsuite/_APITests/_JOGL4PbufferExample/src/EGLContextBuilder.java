import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;

public class EGLContextBuilder
{
    private final boolean headless;

    public EGLContextBuilder(boolean headless)
    {
        this.headless = headless;
    }

    public boolean isHeadless()
    {
        return headless;
    }

    public GLOffscreenAutoDrawable createDrawable(int width, int height)
        throws GLException
    {
        GLProfile profile = headless
            ? createHeadlessProfile()
            : GLProfile.get(GLProfile.GL4);
        GLCapabilities pbCaps = new GLCapabilities(profile);
        pbCaps.setDoubleBuffered(false);

        try {
            GLDrawableFactory creator = headless
                ? GLDrawableFactory.getEGLFactory()
                : GLDrawableFactory.getFactory(profile);
            return creator.createOffscreenAutoDrawable(
                null, pbCaps, null, width, height);
        }
        catch ( Exception e ) {
            Logger.reportMessageWithException(
                this,
                VSDK.FATAL_ERROR,
                "EGLContextBuilder.createDrawable",
                "Unable to create an offscreen OpenGL drawable for this host.",
                e);
            throw new GLException(e);
        }
    }

    private static GLProfile createHeadlessProfile()
    {
        System.setProperty("jogl.disable.opengldesktop", "true");
        System.setProperty("jogl.disable.opengles", "false");
        System.setProperty("jogl.disable.openglcore", "false");
        System.setProperty("jogl.disable.openglarbcontext", "false");
        System.setProperty("jogl.disable.surfacelesscontext", "false");
        return GLProfile.get(GLProfile.GL4);
    }

    public boolean isOffscreenRenderingEnbled()
    {
        return true;
    }

    public boolean isGPUEnabled()
    {
        return !headless;
    }

    public String getRendererBackendName()
    {
        if ( headless ) {
            return "JOGL offscreen EGL";
        }
        return "JOGL offscreen Pbuffer";
    }
}
