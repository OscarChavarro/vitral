//===========================================================================

package vitral.application;

// Android classes
import android.content.Context;
import android.opengl.GLSurfaceView;

public class BasicGLSurfaceView extends GLSurfaceView {
    public GLES20TriangleRenderer glExecutor;
    public BasicGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        glExecutor = new GLES20TriangleRenderer(context);
        setRenderer(glExecutor);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
