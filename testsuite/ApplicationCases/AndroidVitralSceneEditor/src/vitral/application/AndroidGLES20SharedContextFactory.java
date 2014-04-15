//===========================================================================
package vitral.application;

import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
This class implements a dirty trick to obtain access to configuration values
used for creating EGLContext inside the method GLSurfaceView.setRenderer() in
order to ease the creation of secondary shared contexts for multi threaded
OpenGL ES 2.0.

Standard Android OpenGL ES 2.0 based applications must create a class that
extends interface GLSurfaceView. From such a class, application configures and
creates an OpenGL rendering context, that is hidden from application. Those
context is created using a "DefaultContextFactory", which is an inner class
from GLSurface. This class is based upon DefaultContextFactory, and follows
the same creation steps but also exposes internal parameters to ease the later
creation of secondary shared OpenGL contexts based on this firstly created
one.
*/
public class AndroidGLES20SharedContextFactory
    implements GLSurfaceView.EGLContextFactory
{
    /**
    @return the primaryConfig
    */
    public static EGLConfig getPrimaryConfig() {
        return primaryConfig;
    }

    /**
    @return the primaryAttribList
    */
    public static int[] getPrimaryAttribList() {
        return primaryAttribList;
    }
    private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private int mEGLContextClientVersion = 2;
    private static EGLContext primaryContext = null;
    private static EGLConfig primaryConfig = null;
    private static int primaryAttribList[] = null;
    
    public EGLContext createContext(
        EGL10 egl, 
        EGLDisplay display, 
        EGLConfig eglConfig) {
        
        primaryConfig = eglConfig;
        primaryAttribList = new int[]{
            EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion,
            EGL10.EGL_NONE};

        primaryContext = egl.eglCreateContext(
            display, eglConfig, 
            EGL10.EGL_NO_CONTEXT,
            mEGLContextClientVersion != 0 ? getPrimaryAttribList() : null);
        return primaryContext;
    }

    public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
        if (!egl.eglDestroyContext(display, context)) {
            // Should log an error
        }
    }
    
    public static EGLContext getPrimaryContext()
    {
        return primaryContext;
    }
    
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
