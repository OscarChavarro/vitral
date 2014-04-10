//===========================================================================

package vitral.application;

// Android classes
import android.content.Context;
import android.opengl.GLSurfaceView;
//import javax.microedition.khronos.egl.EGL11;
//import javax.microedition.khronos.egl.EGLContext;
//import javax.microedition.khronos.egl.EGLDisplay;

public class BasicGLSurfaceView extends GLSurfaceView {

    public AndroidGLES20DrawingArea glExecutor;

    public BasicGLSurfaceView(Context context) {
        super(context);

        // Set OpenGL drawing canvas
        setEGLContextClientVersion(2);
        glExecutor = new AndroidGLES20DrawingArea(context);
        setEGLConfigChooser(8 , 8, 8, 8, 16, 0);
        setRenderer(glExecutor);

        // Trying to prepare multithread rendering for OpenGL 2.0 ES
        //EGL11 driver = (EGL11)EGLContext.getEGL();
        /*
        EGLDisplay display;
        display = driver.eglGetDisplay(EGL11.EGL_DEFAULT_DISPLAY);
        System.out.println("XXXX: display: " + display);
        */
        //System.out.println("XXXX: Driver: " + driver);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
