package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;

public class Jogl4RendererConfigurationShaderSelector extends Jogl4Renderer {
    private static int constantProgramId;
    private static int texturedProgramId;
    private static int flatProgramId;
    private static int flatTexturedProgramId;
    private static int gouraudProgramId;
    private static int phongProgramId;
    private static int phongBumpProgramId;

    public static int selectShaderProgram(GL4 gl, RendererConfiguration quality)
    {
        ensurePrograms(gl);
        if ( quality != null && quality.isTextureSet() ) {
            return texturedProgramId;
        }
        return constantProgramId;
    }

    public static int selectSurfaceShaderProgram(
        GL4 gl,
        RendererConfiguration quality,
        boolean hasTexture,
        boolean hasNormalMap)
    {
        ensurePrograms(gl);

        if ( quality == null ) {
            return hasTexture ? texturedProgramId : constantProgramId;
        }

        int shadingType = quality.getShadingType();

        if ( shadingType == RendererConfiguration.SHADING_TYPE_NOLIGHT ) {
            return (quality.isTextureSet() && hasTexture) ? texturedProgramId : constantProgramId;
        }

        if ( shadingType == RendererConfiguration.SHADING_TYPE_FLAT ) {
            return (quality.isTextureSet() && hasTexture) ? flatTexturedProgramId : flatProgramId;
        }

        if ( shadingType == RendererConfiguration.SHADING_TYPE_PHONG ) {
            if ( quality.isBumpMapSet() && hasTexture && hasNormalMap ) {
                return phongBumpProgramId;
            }
            return phongProgramId;
        }

        return gouraudProgramId;
    }

    public static void activateShader(
        GL4 gl,
        int programId,
        Matrix4x4 modelViewProjection,
        RendererConfiguration quality,
        float diffuseR,
        float diffuseG,
        float diffuseB)
    {
        gl.glUseProgram(programId);

        int modelViewProjectionLoc = gl.glGetUniformLocation(programId, "modelViewProjectionLocal");
        if ( modelViewProjectionLoc >= 0 ) {
            gl.glUniformMatrix4fv(
                modelViewProjectionLoc,
                1,
                false,
                Jogl4MatrixRenderer.toColumnMajorFloatArray(modelViewProjection),
                0);
        }

        int diffuseColorLoc = gl.glGetUniformLocation(programId, "diffuseColor");
        if ( diffuseColorLoc >= 0 ) {
            gl.glUniform3f(diffuseColorLoc, diffuseR, diffuseG, diffuseB);
        }

        int withTextureLoc = gl.glGetUniformLocation(programId, "withTexture");
        if ( withTextureLoc >= 0 ) {
            int useTexture = (quality != null && quality.isTextureSet()) ? 1 : 0;
            gl.glUniform1i(withTextureLoc, useTexture);
        }

        int withVertexColorsLoc = gl.glGetUniformLocation(programId, "withVertexColors");
        if ( withVertexColorsLoc >= 0 ) {
            int useVertexColors = (quality != null && quality.getUseVertexColors()) ? 1 : 0;
            gl.glUniform1i(withVertexColorsLoc, useVertexColors);
        }

        int textureSamplerLoc = gl.glGetUniformLocation(programId, "sTexture");
        if ( textureSamplerLoc >= 0 ) {
            gl.glUniform1i(textureSamplerLoc, 0);
        }

        int normalSamplerLoc = gl.glGetUniformLocation(programId, "sNormalMap");
        if ( normalSamplerLoc >= 0 ) {
            gl.glUniform1i(normalSamplerLoc, 1);
        }
    }

    public static void deactivateShader(GL4 gl)
    {
        gl.glUseProgram(0);
    }

    public static void dispose(GL4 gl)
    {
        if ( constantProgramId != 0 ) {
            gl.glDeleteProgram(constantProgramId);
            constantProgramId = 0;
        }
        if ( texturedProgramId != 0 ) {
            gl.glDeleteProgram(texturedProgramId);
            texturedProgramId = 0;
        }
        if ( flatProgramId != 0 ) {
            gl.glDeleteProgram(flatProgramId);
            flatProgramId = 0;
        }
        if ( flatTexturedProgramId != 0 ) {
            gl.glDeleteProgram(flatTexturedProgramId);
            flatTexturedProgramId = 0;
        }
        if ( gouraudProgramId != 0 ) {
            gl.glDeleteProgram(gouraudProgramId);
            gouraudProgramId = 0;
        }
        if ( phongProgramId != 0 ) {
            gl.glDeleteProgram(phongProgramId);
            phongProgramId = 0;
        }
        if ( phongBumpProgramId != 0 ) {
            gl.glDeleteProgram(phongBumpProgramId);
            phongBumpProgramId = 0;
        }
    }

    private static void ensurePrograms(GL4 gl)
    {
        if ( constantProgramId == 0 ) {
            constantProgramId = Jogl4ShaderProgramUtil.createProgramFromFiles(
                gl,
                "constantVertexShader.glsl",
                "constantPixelShader.glsl");
        }

        if ( texturedProgramId == 0 ) {
            texturedProgramId = Jogl4ShaderProgramUtil.createProgramFromFiles(
                gl,
                "constantTextureVertexShader.glsl",
                "constantTexturePixelShader.glsl");
        }

        if ( gouraudProgramId == 0 ) {
            gouraudProgramId = Jogl4ShaderProgramUtil.createProgramFromFiles(
                gl,
                "gouraudTextureVertexShader.glsl",
                "gouraudTexturePixelShader.glsl");
        }

        if ( flatProgramId == 0 ) {
            flatProgramId = Jogl4ShaderProgramUtil.createProgramFromFiles(
                gl,
                "flatVertexShader.glsl",
                "flatPixelShader.glsl");
        }

        if ( flatTexturedProgramId == 0 ) {
            flatTexturedProgramId = Jogl4ShaderProgramUtil.createProgramFromFiles(
                gl,
                "flatTexturedVertexShader.glsl",
                "flatTexturedPixelShader.glsl");
        }

        if ( phongProgramId == 0 ) {
            phongProgramId = Jogl4ShaderProgramUtil.createProgramFromFiles(
                gl,
                "phongTextureVertexShader.glsl",
                "phongTexturePixelShader.glsl");
        }

        if ( phongBumpProgramId == 0 ) {
            phongBumpProgramId = Jogl4ShaderProgramUtil.createProgramFromFiles(
                gl,
                "phongTextureBumpVertexShader.glsl",
                "phongTextureBumpPixelShader.glsl");
        }
    }
}
