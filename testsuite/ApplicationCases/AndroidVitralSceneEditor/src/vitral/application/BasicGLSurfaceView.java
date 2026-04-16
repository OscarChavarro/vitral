package vitral.application;

// Android classes
import android.content.Context;
import android.opengl.GLSurfaceView;

public class BasicGLSurfaceView extends GLSurfaceView {

    private final AndroidGLES20DrawingArea glExecutor;

    public BasicGLSurfaceView(Context context) {
        super(context);

        // Set OpenGL drawing canvas
        setEGLContextClientVersion(2); // Using OpenGL ES 2.0
        glExecutor = new AndroidGLES20DrawingArea(context);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0); // RGBA depth stencil profile
        setEGLContextFactory(new AndroidGLES20SharedContextFactory()); // Experimental!
        setRenderer(glExecutor);
        
        setOnTouchListener(glExecutor);
    }

    /**
    @return the glExecutor
    */
    public AndroidGLES20DrawingArea getGlExecutor() {
        return glExecutor;
    }
    
}
