//===========================================================================

package vsdk.toolkit.render.joglcg;

// Java base classes
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.ArrayList;

// JOGL/CG classes
import javax.media.opengl.GL2;
import com.jogamp.opengl.cg.CgGL;
import com.jogamp.opengl.cg.CGcontext;
import com.jogamp.opengl.cg.CGprogram;
import com.jogamp.opengl.cg.CGparameter;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.render.jogl.JoglRenderer;

/**
The JoglCgRenderer abstract class provides an interface for JoglCg*Renderer
style classes. This serves two purposes:
  - To help in design level organization of JoglCg renderers (this eases the
    study of the class hierarchy)
  - To provide a place to locate operations common to all JoglCg renderers
    classes and JoglCg renderers' private utility/supporting classes. In this
    moment, this operations include the global framework for managing
    Nvidia Cg access and general JOGL initialization, as such verifying
    correct availavility of native Nvidia Cg libraries.
*/
public abstract class JoglCgRenderer extends JoglRenderer
{
    // Nvidia Cg general management
    protected static boolean nvidiaCgErrorReported = false;
    private static boolean nvidiaCgAvailable = false;
    private static CGcontext nvidiaGpuContext = null;
    private static int nvidiaGpuVertexProfile = -1;
    private static int nvidiaGpuPixelProfile = -1;
    private static boolean renderingWithNvidiaGpuFlag = false;
    public static CGprogram currentVertexShader = null;
    public static CGprogram currentPixelShader = null;

    // Nvidia Cg automatic shader management
    public static boolean nvidiaCgAutomaticMode = false;
    public static CGprogram NvidiaGpuVertexProgramTexture;
    public static CGprogram NvidiaGpuPixelProgramTexture;
    public static CGprogram NvidiaGpuVertexProgramTextureBump;
    public static CGprogram NvidiaGpuPixelProgramTextureBump;
    private static ArrayList<CGprogram> NvidiaGuiAllVertexShaders = null;
    private static ArrayList<CGprogram> NvidiaGuiAllPixelShaders = null;

    public static CGprogram getCurrentVertexShader()
    {
        return currentVertexShader;
    }

    public static CGprogram getCurrentPixelShader()
    {
        return currentPixelShader;
    }

    protected static ArrayList<CGprogram>
    getAllVertexShaders()
    {
        if ( NvidiaGuiAllVertexShaders == null ) {
            NvidiaGuiAllVertexShaders = new ArrayList<CGprogram>();
        }

        NvidiaGuiAllVertexShaders.add(NvidiaGpuVertexProgramTexture);
        NvidiaGuiAllVertexShaders.add(NvidiaGpuVertexProgramTextureBump);
        return NvidiaGuiAllVertexShaders;
    }

    protected static ArrayList<CGprogram>
    getAllPixelShaders()
    {
        if ( NvidiaGuiAllPixelShaders == null ) {
            NvidiaGuiAllPixelShaders = new ArrayList<CGprogram>();
        }

        NvidiaGuiAllPixelShaders.add(NvidiaGpuPixelProgramTexture);
        NvidiaGuiAllPixelShaders.add(NvidiaGpuPixelProgramTextureBump);
        return NvidiaGuiAllPixelShaders;
    }

    public static void createDefaultAutomaticNvidiaCgShaders(String path)
    {
        nvidiaCgAutomaticMode = true;

        FileInputStream fis;

        try {
            //-----------------------------------------------------------------
            if ( !tryToEnableNvidiaCg() ) {
                nvidiaCgAutomaticMode = false;
                return;
            }
            fis = new FileInputStream(path + "/PhongTextureVertexShader.cg");
            NvidiaGpuVertexProgramTexture =
                JoglCgRenderer.loadNvidiaGpuVertexShader(fis);
            fis.close();
            fis = new FileInputStream(path + "/PhongTexturePixelShader.cg");
            NvidiaGpuPixelProgramTexture =
              JoglCgRenderer.loadNvidiaGpuPixelShader(fis);
            fis.close();
            fis = new FileInputStream(path + "/PhongTextureBumpVertexShader.cg");
            NvidiaGpuVertexProgramTextureBump =
              JoglCgRenderer.loadNvidiaGpuVertexShader(fis);
            fis.close();
            fis = new FileInputStream(path + "/PhongTextureBumpPixelShader.cg");
            NvidiaGpuPixelProgramTextureBump =
              JoglCgRenderer.loadNvidiaGpuPixelShader(fis);
            fis.close();
        }
        catch ( Exception e ) {
            VSDK.reportMessage(null, VSDK.WARNING, 
                "JoglCgRenderer.createDefaultAutomaticNvidiaCgShaders",
                "Cannot access Cg shaders (.cg files). Nvidia Cg shader deactivated.");
            nvidiaCgAutomaticMode = false;
            return;
        }
    }

    public static void setNvidiaCgAutomaticMode(boolean flag)
    {
        nvidiaCgAutomaticMode = flag;
    }

    public static boolean getNvidiaCgAutomaticMode()
    {
        return nvidiaCgAutomaticMode;
    }

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

    /**
    This method searches for the dinamic link library packed shared objects
    (.dll or .so.*) needed for Nvidia Cg to work. Returns true if libraries
    are found in standard system locations, false otherwise.

    Note that OpenGL doesn't need to be started before calling this method.
    */
    public static boolean verifyNvidiaCgAvailability()
    {
        if ( !PersistenceElement.verifyLibrary("Cg") ) {
            if ( nvidiaCgErrorReported ) return false;
            nvidiaCgErrorReported = true;
            VSDK.reportMessage(null, VSDK.WARNING, 
                "JoglCgRenderer.verifyNvidiaCgAvailability",
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
            nvidiaCgAutomaticMode = false;
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
                    "JoglCgRenderer.tryToEnableNvidiaCg",
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
                    "JoglCgRenderer.tryToEnableNvidiaCg",
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
        CGprogram shader = null;
        try {
            shader = CgGL.cgCreateProgramFromStream(
                nvidiaGpuContext, CgGL.CG_SOURCE, is,
                nvidiaGpuVertexProfile, null, null);
            if ( !CgGL.cgIsProgramCompiled(shader) ) {
                CgGL.cgCompileProgram(shader);
            }
            CgGL.cgGLLoadProgram(shader);
        }
        catch ( Exception e ) {
            if ( nvidiaCgErrorReported ) return null;
            nvidiaCgErrorReported = true;
            VSDK.reportMessage(null, VSDK.WARNING, 
                "JoglCgRenderer.loadNvidiaGpuVertexShader",
                "Error loading an nvidia vertex shader.\n" + 
                "CG vertex shader functionality disabled.\n" + 
                "Furter error reporting on this issue disabled");
            nvidiaCgAvailable = false;
            return null;
        }
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
            if ( !CgGL.cgIsProgramCompiled(shader) ) {
                CgGL.cgCompileProgram(shader);
            }
            CgGL.cgGLLoadProgram(shader);
        }
        catch ( Exception e ) {
            if ( nvidiaCgErrorReported ) return null;
            nvidiaCgErrorReported = true;
            VSDK.reportMessage(null, VSDK.WARNING, 
                "JoglCgRenderer.loadNvidiaGpuPixelShader",
                "Error loading an nvidia pixel shader.\n" + 
                "CG pixel shader functionality disabled.\n" + 
                "Furter error reporting on this issue disabled");
            nvidiaCgAvailable = false;
            return null;
        }

        return shader;
    }

    public static boolean needCg(RendererConfiguration quality)
    {
        if ( quality.getShadingType() != RendererConfiguration.SHADING_TYPE_PHONG ||
             nvidiaCgErrorReported ) {
            return false;
        }
        return true;
    }

    public static void deactivateNvidiaGpuParameters(GL2 gl, RendererConfiguration quality)
    {
        if ( nvidiaCgErrorReported ) {
            return;
        }

        //-----------------------------------------------------------------
        if ( nvidiaCgAutomaticMode && needCg(quality) ) {
            // Disable textures
            CGparameter param = null;
            param = CgGL.cgGetNamedParameter(NvidiaGpuPixelProgramTexture, "textureMap");
            CgGL.cgGLDisableTextureParameter(param);
            param = CgGL.cgGetNamedParameter(NvidiaGpuPixelProgramTextureBump, "textureMap");
            CgGL.cgGLDisableTextureParameter(param);
            param = CgGL.cgGetNamedParameter(NvidiaGpuPixelProgramTexture, "normalMap");
            CgGL.cgGLDisableTextureParameter(param);
            param = CgGL.cgGetNamedParameter(NvidiaGpuPixelProgramTextureBump, "normalMap");
            CgGL.cgGLDisableTextureParameter(param);
            disableNvidiaCgProfiles();
            setRenderingWithNvidiaGpu(false);
            setDefaultTextureForFixedFunctionOpenGL(
                NvidiaGpuPixelProgramTexture);
        }
    }

    public static void activateNvidiaGpuParameters(GL2 gl,
        RendererConfiguration quality,
        CGprogram vertexShader, CGprogram pixelShader)
    {
        if ( nvidiaCgErrorReported ) {
            return;
        }

        //-----------------------------------------------------------------
        if ( nvidiaCgAutomaticMode && needCg(quality) ) {
            //- Global per-frame shader activation ----------------------------
            enableNvidiaCgProfiles();
            setRenderingWithNvidiaGpu(true);

            if ( quality.isBumpMapSet() ) {
                JoglCgRenderer.currentVertexShader = JoglCgRenderer.NvidiaGpuVertexProgramTextureBump;
                JoglCgRenderer.currentPixelShader = JoglCgRenderer.NvidiaGpuPixelProgramTextureBump;
              }
              else {
                JoglCgRenderer.currentVertexShader = JoglCgRenderer.NvidiaGpuVertexProgramTexture;
                JoglCgRenderer.currentPixelShader = JoglCgRenderer.NvidiaGpuPixelProgramTexture;
            }
            JoglCgRenderer.bindNvidiaGpuShaders(
                currentVertexShader, currentPixelShader);
            vertexShader = currentVertexShader;
            pixelShader = currentPixelShader;

            //- Multiple texture management for pixel shaders -----------------
            CGparameter param;
            param = CgGL.cgGetNamedParameter(JoglCgRenderer.currentPixelShader, "textureMap");
            CgGL.cgGLEnableTextureParameter(param);
            if ( quality.isBumpMapSet() ) {
                param = CgGL.cgGetNamedParameter(JoglCgRenderer.currentPixelShader, "normalMap");
                CgGL.cgGLEnableTextureParameter(param);
            }  

        }
        //-----------------------------------------------------------------
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

        JoglCgRenderer.enableNvidiaCgProfiles();
        CgGL.cgGLBindProgram(pixelShader);
        param = CgGL.cgGetNamedParameter(pixelShader, "textureMap");
        CgGL.cgGLEnableTextureParameter(param);
        JoglCgRenderer.disableNvidiaCgProfiles();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
