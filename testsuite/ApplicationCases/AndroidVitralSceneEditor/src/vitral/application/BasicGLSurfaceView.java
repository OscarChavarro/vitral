//===========================================================================

package vitral.application;

// Android classes
import android.content.Context;
import android.opengl.GLSurfaceView;

public class BasicGLSurfaceView extends GLSurfaceView {

    public GLES20TriangleRenderer glExecutor;

    public BasicGLSurfaceView(Context context) {
        super(context);

        // Set OpenGL drawing canvas
        setEGLContextClientVersion(2);
        glExecutor = new GLES20TriangleRenderer(context);
        setEGLConfigChooser(8 , 8, 8, 8, 16, 0);
        setRenderer(glExecutor);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
