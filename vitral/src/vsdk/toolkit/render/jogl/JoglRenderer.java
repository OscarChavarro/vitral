//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - July 25 2006 - Oscar Chavarro: Original base version                  =
//= - December 28 2006 - Oscar Chavarro: Added Nvidia Cg support            =
//===========================================================================

package vsdk.toolkit.render.jogl;

// Java base classes
import java.io.InputStream;

// JOGL clases
import javax.media.opengl.GL;
import com.sun.opengl.cg.CgGL;
import com.sun.opengl.cg.CGcontext;
import com.sun.opengl.cg.CGprogram;
import com.sun.opengl.cg.CGparameter;

// VitralSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.render.RenderingElement;

/**
The JoglRenderer abstract class provides an interface for Jogl*Renderer
style classes. This serves two purposes:
  - To help in design level organization of Jogl renderers (this eases the
    study of the class hierarchy)
  - To provide a place to locate operations common to all Jogl renderers
    classes and Jogl renderers' private utility/supporting classes. In this
    moment, this operations include the global framework for managing
    Nvidia Cg access and general JOGL initialization, as such verifying
    correct availavility of native OpenGL libraries.
*/

public abstract class JoglRenderer extends RenderingElement {
    /**
    Note that this static block is automatically called for the first
    instanciation or the first static call to any of current subclasses.
    This is used to check correct JOGL environment availability, and
    report if not. DISABLED!
    */
/*
    static {
        // What happens when this is executed from an applet?
        verifyOpenGLAvailability();
    }
*/

    private static boolean nvidiaCgErrorReported = false;
    private static boolean nvidiaCgAvailable = false;
    private static CGcontext nvidiaGpuContext = null;
    private static int nvidiaGpuVertexProfile = -1;
    private static int nvidiaGpuPixelProfile = -1;
    private static boolean renderingWithNvidiaGpuFlag = false;
    private static CGprogram currentVertexShader = null;
    private static CGprogram currentPixelShader = null;

    public static boolean renderingWithNvidiaGpu()
    {
        return renderingWithNvidiaGpuFlag;
    }

    public static boolean setRenderingWithNvidiaGpu(boolean requested)
    {
        if ( !getNvidiaCgAvailability() ) {
            renderingWithNvidiaGpuFlag = false;
        }
    else {
            renderingWithNvidiaGpuFlag = requested;
    }
        return renderingWithNvidiaGpuFlag;
    }

    public static void
    bindNvidiaGpuShaders(CGprogram vertexShader, CGprogram pixelShader)
    {
        currentVertexShader = vertexShader;
    currentPixelShader = pixelShader;
    if ( currentVertexShader != null ) {
            CgGL.cgGLBindProgram(currentVertexShader);
    }
    if ( currentPixelShader != null ) {
            CgGL.cgGLBindProgram(currentPixelShader);
    }
    }

    public static CGparameter
    accessNvidiaGpuVertexParameter(String name)
    {
    if ( currentVertexShader != null ) {
            return CgGL.cgGetNamedParameter(currentVertexShader, name);
    }
    return null;
    }

    public static CGparameter
    accessNvidiaGpuPixelParameter(String name)
    {
    if ( currentPixelShader != null ) {
            return CgGL.cgGetNamedParameter(currentPixelShader, name);
    }
    return null;
    }

    public static boolean verifyOpenGLAvailability()
    {
        if ( !PersistenceElement.verifyLibrary("jogl") ) {
            VSDK.reportMessage(null, VSDK.FATAL_ERROR, 
                "JoglRenderer.verifyOpenGLAvailability",
                "JOGL Library not found.  Check your installation.");
            return false;
        }
        if ( !PersistenceElement.verifyLibrary("jogl_awt") ) {
            VSDK.reportMessage(null, VSDK.FATAL_ERROR, 
                "JoglRenderer.verifyOpenGLAvailability",
                "JOGL-AWT Library not found.  Check your installation.");
            return false;
        }
        if ( !PersistenceElement.verifyLibrary("jogl_cg") ) {
            VSDK.reportMessage(null, VSDK.FATAL_ERROR, 
                "JoglRenderer.verifyOpenGLAvailability",
                "JOGL-CG Library not found.  Check your installation.");
            return false;
        }
        return true;
    }

    public static boolean verifyNvidiaCgAvailability()
    {
        if ( !PersistenceElement.verifyLibrary("Cg") ) {
            if ( nvidiaCgErrorReported ) return false;
            nvidiaCgErrorReported = true;
            VSDK.reportMessage(null, VSDK.WARNING, 
                "JoglRenderer.verifyNvidiaCgAvailability",
                "CG Library not found.  Check your installation.\n" + 
                "Check it is installed from http://developers.nvidia.com\n" +
                "Furter error reporting on this issue disabled");
            return false;
        }
        return true;
    }

    public static boolean getNvidiaCgAvailability()
    {
        return nvidiaCgAvailable;
    }

    public static boolean tryToEnableNvidiaCg()
    {
        nvidiaCgAvailable = false;

        //-----------------------------------------------------------------
        if ( !verifyOpenGLAvailability() ||
             !verifyNvidiaCgAvailability() ) {
            return false;
        }

        //-----------------------------------------------------------------
        nvidiaGpuContext = CgGL.cgCreateContext();

        if ( CgGL.cgGLIsProfileSupported(CgGL.CG_PROFILE_ARBVP1) ) {
            nvidiaGpuVertexProfile = CgGL.CG_PROFILE_ARBVP1;
        }
        else {
            if (CgGL.cgGLIsProfileSupported(CgGL.CG_PROFILE_VP30)) {
                nvidiaGpuVertexProfile = CgGL.CG_PROFILE_VP30;
            }
            else {
                if ( nvidiaCgErrorReported ) return false;
                nvidiaCgErrorReported = true;
                VSDK.reportMessage(null, VSDK.WARNING, 
                    "JoglRenderer.tryToEnableNvidiaCg",
                    "Neither arbvp1 or vp30 vertex profiles supported on " + 
                    "this system.\n" +
                    "CG vertex shader functionality disabled.\n" + 
                    "Furter error reporting on this issue disabled");
                nvidiaCgAvailable = false;
                return false;
            }
        }

        if ( CgGL.cgGLIsProfileSupported(CgGL.CG_PROFILE_ARBFP1) ) {
            nvidiaGpuPixelProfile = CgGL.CG_PROFILE_ARBFP1;
        }
        else {
            // try FP30
            if ( CgGL.cgGLIsProfileSupported(CgGL.CG_PROFILE_FP30) ) {
                nvidiaGpuPixelProfile = CgGL.CG_PROFILE_FP30;
            }
            else {
                if ( nvidiaCgErrorReported ) return false;
                nvidiaCgErrorReported = true;
                VSDK.reportMessage(null, VSDK.WARNING, 
                    "JoglRenderer.tryToEnableNvidiaCg",
                    "Neither arbfp1 or fp30 pixel profiles supported on " +
                    "this system.\n" + 
                    "CG pixel shader functionality disabled.\n" + 
                    "Furter error reporting on this issue disabled");
                nvidiaCgAvailable = false;
                return false;
            }
        }

        //-----------------------------------------------------------------
        // Warning: do not use this!
        //CgGL.cgGLSetManageTextureParameters(nvidiaGpuContext, true);
        nvidiaCgAvailable = true;
        return true;
    }

    public static void enableNvidiaCgProfiles()
    {
        if ( getNvidiaCgAvailability() ) {
            CgGL.cgGLEnableProfile(nvidiaGpuVertexProfile);
            CgGL.cgGLEnableProfile(nvidiaGpuPixelProfile);
        }
    }

    public static void disableNvidiaCgProfiles()
    {
        if ( getNvidiaCgAvailability() ) {
            CgGL.cgGLUnbindProgram(nvidiaGpuVertexProfile);
            CgGL.cgGLUnbindProgram(nvidiaGpuPixelProfile);
            CgGL.cgGLDisableProfile(nvidiaGpuVertexProfile);
            CgGL.cgGLDisableProfile(nvidiaGpuPixelProfile);
        }
    }

    public static CGprogram loadNvidiaGpuVertexShader(InputStream is)
    {
        if ( !getNvidiaCgAvailability() ) {
            return null;
        }

        CGprogram shader;
        try {
            shader = CgGL.cgCreateProgramFromStream(
                nvidiaGpuContext, CgGL.CG_SOURCE, is,
                nvidiaGpuVertexProfile, null, null);
        }
        catch ( Exception e ) {
            if ( nvidiaCgErrorReported ) return null;
            nvidiaCgErrorReported = true;
            VSDK.reportMessage(null, VSDK.WARNING, 
                "JoglRenderer.loadNvidiaGpuVertexShader",
                "Error loading an nvidia vertex shader.\n" + 
                "CG vertex shader functionality disabled.\n" + 
                "Furter error reporting on this issue disabled");
            nvidiaCgAvailable = false;
            return null;
        }
        if ( !CgGL.cgIsProgramCompiled(shader) ) {
            CgGL.cgCompileProgram(shader);
        }
        CgGL.cgGLLoadProgram(shader);

        return shader;
    }

    public static CGprogram loadNvidiaGpuPixelShader(InputStream is)
    {
        if ( !getNvidiaCgAvailability() ) {
            return null;
        }

        CGprogram shader;
        try {
            shader = CgGL.cgCreateProgramFromStream(
                nvidiaGpuContext, CgGL.CG_SOURCE, is,
                nvidiaGpuPixelProfile, null, null);
        }
        catch ( Exception e ) {
            if ( nvidiaCgErrorReported ) return null;
            nvidiaCgErrorReported = true;
            VSDK.reportMessage(null, VSDK.WARNING, 
                "JoglRenderer.loadNvidiaGpuPixelShader",
                "Error loading an nvidia pixel shader.\n" + 
                "CG pixel shader functionality disabled.\n" + 
                "Furter error reporting on this issue disabled");
            nvidiaCgAvailable = false;
            return null;
        }
        if ( !CgGL.cgIsProgramCompiled(shader) ) {
            CgGL.cgCompileProgram(shader);
        }
        CgGL.cgGLLoadProgram(shader);

        return shader;
    }

    public static void activateNvidiaGpuParameters(GL gl,
        RendererConfiguration quality,
        CGprogram vertexShader, CGprogram pixelShader)
    {
        double withTexture = 0.0;
        if ( quality.isTextureSet() ) withTexture = 1.0;
        CgGL.cgGLSetParameter1d(CgGL.cgGetNamedParameter(
            pixelShader, "withTexture"), withTexture);
    }

    /**
    When using Nvidia Gpu fragment shaders with multiple textures (samplers),
    there is a problem drawing objects with default fixed function OpenGL/
    JOGL, and is that the default texture for fixed function is changed.
    This method is not fair, but a desperate measure for returning
    fixed function OpenGL/JOGL to the default texture.
    Current implementation requires an standarized VitralSDK pixel shader
    NOT USING more than one texture.  It has been observed that just
    enabling and disabling such a shader, turns default OpenGL/JOGL default
    texture for fixed function pipeline to that texture associated with
    "textureMap" sampler in current shader.  Use this function in the
    initialization of shaders, and each time Nvidia GPU use is turned off.
    */
    public static void
    setDefaultTextureForFixedFunctionOpenGL(CGprogram pixelShader)
    {
        CGparameter param;

        JoglRenderer.enableNvidiaCgProfiles();
        CgGL.cgGLBindProgram(pixelShader);
        param = CgGL.cgGetNamedParameter(pixelShader, "textureMap");
        CgGL.cgGLEnableTextureParameter(param);
        JoglRenderer.disableNvidiaCgProfiles();
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
