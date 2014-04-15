//===========================================================================
package vitral.application;

// OpenGL ES 2.0 classes
import java.io.InputStream;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.io.image.ImagePersistence;

/**
Creates an off-screen area to render OpenGL ES 2.0 commands on an asset 
pre loading thread.
*/
public class AndroidGLES20AssetLoader implements Runnable {

    private final AndroidGLES20DrawingArea parent;
    
    public AndroidGLES20AssetLoader(AndroidGLES20DrawingArea parent)
    {
        this.parent = parent;
    }
    
    private void sendLoadedModelAssetsToGPU() {
        //- Set up textures -----------------------------------------------
        InputStream is;

        try {
            is = parent.getAndroidApplicationContext().getResources().
                    openRawResource(R.raw.miniearth);
            parent.setTexture(ImagePersistence.importRGB(is));
        }
        catch ( Exception e ) {
            VSDK.reportMessageWithException(this, VSDK.FATAL_ERROR,
                    "createModel", "Can not load texture!", e);
        }

        // Note that generated OpenGL operations on current thread will be
        // drawn over pbuffer surface, but generated display lists will be
        // shared between contexts...
        parent.drawImage(parent.getTexture(), new Camera(), 0, 0);
    }

    /**
    Current method for creating a PBuffer on Android OpenGL ES 2.0 and attaching
    its context as a shared one to currently existing main application context.
    */
    private void initOpenGLOffscreenDisplayForCurrentThread() {
        // Trying to prepare multithread rendering for OpenGL 2.0 ES
        EGL10 driver;
        EGLDisplay display;
        
        driver = (EGL10) EGLContext.getEGL();
        display = driver.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        EGLContext assetLoadingContext;
        
        assetLoadingContext = driver.eglCreateContext(
                display,
                AndroidGLES20SharedContextFactory.getPrimaryConfig(),
                AndroidGLES20SharedContextFactory.getPrimaryContext(),
                AndroidGLES20SharedContextFactory.getPrimaryAttribList());
        
        int attribList[] = {EGL10.EGL_NONE};
        
        EGLSurface pbuffer = driver.eglCreatePbufferSurface(
                display,
                AndroidGLES20SharedContextFactory.getPrimaryConfig(),
                attribList);
        driver.eglMakeCurrent(display, pbuffer, pbuffer, assetLoadingContext);
    }

    /**
    This thread execution should be controlled in such a way that GPU loading
    of assets makes a pipelined process with respect to model loader threads.
    For each model loading stage ready, this thread performs the GPU loading
    step, without waiting for model loader thread to end reading all model
    elements.
    */
    public void run() {
        initOpenGLOffscreenDisplayForCurrentThread();
        sendLoadedModelAssetsToGPU();
        
        parent.setDisplayListsCompiled(true);
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
