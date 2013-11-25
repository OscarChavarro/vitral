//===========================================================================

package vitral.application;

// Android classes
import android.content.Context;
import android.opengl.GLSurfaceView;

public class BasicGLSurfaceView extends GLSurfaceView {

    public AndroidGLES20DrawingArea glExecutor;

    public BasicGLSurfaceView(Context context) {
        super(context);

        // Set OpenGL drawing canvas
        setEGLContextClientVersion(2);
        glExecutor = new AndroidGLES20DrawingArea(context);
        setEGLConfigChooser(8 , 8, 8, 8, 16, 0);
        setRenderer(glExecutor);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
