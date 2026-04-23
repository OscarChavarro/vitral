package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;

public class Jogl4RendererConfigurationRenderer extends Jogl4Renderer {
    private static int constantProgramId;
    private static int texturedProgramId;

    public static int selectShaderProgram(GL4 gl, RendererConfiguration quality)
    {
        ensurePrograms(gl);
        if ( quality != null && quality.isTextureSet() ) {
            return texturedProgramId;
        }
        return constantProgramId;
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
    }
}
