package vsdk.toolkit.render.jogl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.RGBAImage;

public class Jogl4ImageRenderer extends Jogl4Renderer {
    private static final RendererConfiguration TEXTURE_QUALITY = new RendererConfiguration();

    private static int quadVaoId;
    private static int quadPositionVboId;
    private static int quadUvVboId;

    static {
        TEXTURE_QUALITY.setTexture(true);
        TEXTURE_QUALITY.setUseVertexColors(false);
    }

    public static int activate(GL4 gl, Image img)
    {
        if ( img instanceof RGBAImage ) {
            return Jogl4RGBAImageRenderer.activate(gl, (RGBAImage)img);
        }
        if ( img instanceof RGBImage ) {
            return Jogl4RGBImageRenderer.activate(gl, (RGBImage)img);
        }
        return -1;
    }

    public static void deactivate(GL4 gl, Image img)
    {
        if ( img instanceof RGBAImage ) {
            Jogl4RGBAImageRenderer.deactivate(gl, (RGBAImage)img);
        }
        else if ( img instanceof RGBImage ) {
            Jogl4RGBImageRenderer.deactivate(gl, (RGBImage)img);
        }
    }

    public static void unload(GL4 gl, Image img)
    {
        if ( img instanceof RGBAImage ) {
            Jogl4RGBAImageRenderer.unload(gl, (RGBAImage)img);
        }
        else if ( img instanceof RGBImage ) {
            Jogl4RGBImageRenderer.unload(gl, (RGBImage)img);
        }
    }

    public static void draw(GL4 gl, Image img)
    {
        if ( img instanceof RGBAImage ) {
            Jogl4RGBAImageRenderer.draw(gl, (RGBAImage)img);
        }
        else if ( img instanceof RGBImage ) {
            Jogl4RGBImageRenderer.draw(gl, (RGBImage)img);
        }
    }

    public static void drawTexturedQuad(
        GL4 gl,
        int textureId,
        Matrix4x4 modelViewProjection,
        float[] positions,
        float[] uvCoordinates,
        float diffuseR,
        float diffuseG,
        float diffuseB)
    {
        if ( textureId <= 0 || positions == null || uvCoordinates == null ) {
            return;
        }
        if ( positions.length % 3 != 0 || uvCoordinates.length % 2 != 0 ) {
            throw new IllegalArgumentException("Invalid quad data");
        }
        if ( (positions.length / 3) != (uvCoordinates.length / 2) ) {
            throw new IllegalArgumentException("Position/UV vertex count mismatch");
        }

        ensureBuffers(gl);

        gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);

        int programId = Jogl4RendererConfigurationRenderer.selectShaderProgram(gl, TEXTURE_QUALITY);
        Jogl4RendererConfigurationRenderer.activateShader(
            gl,
            programId,
            modelViewProjection,
            TEXTURE_QUALITY,
            diffuseR,
            diffuseG,
            diffuseB);

        gl.glActiveTexture(GL4.GL_TEXTURE0);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, textureId);

        gl.glBindVertexArray(quadVaoId);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, quadPositionVboId);
        gl.glBufferData(
            GL4.GL_ARRAY_BUFFER,
            (long)positions.length * Float.BYTES,
            Buffers.newDirectFloatBuffer(positions),
            GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, quadUvVboId);
        gl.glBufferData(
            GL4.GL_ARRAY_BUFFER,
            (long)uvCoordinates.length * Float.BYTES,
            Buffers.newDirectFloatBuffer(uvCoordinates),
            GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(2);
        gl.glVertexAttribPointer(2, 2, GL4.GL_FLOAT, false, 0, 0L);

        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, positions.length / 3);

        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(2);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);

        Jogl4RendererConfigurationRenderer.deactivateShader(gl);
    }

    static void drawLowerLeftOverlay(GL4 gl, int textureId, int width, int height)
    {
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);
        int viewportWidth = Math.max(viewport[2], 1);
        int viewportHeight = Math.max(viewport[3], 1);

        float w = 2.0f * ((float)width / (float)viewportWidth);
        float h = 2.0f * ((float)height / (float)viewportHeight);

        float x0 = -1.0f;
        float y0 = -1.0f;
        float x1 = x0 + w;
        float y1 = y0 + h;

        float[] positions = {
            x0, y0, 0.0f,
            x1, y0, 0.0f,
            x1, y1, 0.0f,
            x0, y0, 0.0f,
            x1, y1, 0.0f,
            x0, y1, 0.0f
        };

        float[] uvCoordinates = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        };

        drawTexturedQuad(
            gl,
            textureId,
            Matrix4x4.identityMatrix(),
            positions,
            uvCoordinates,
            1.0f,
            1.0f,
            1.0f);
    }

    public static void dispose(GL4 gl)
    {
        Jogl4RendererConfigurationRenderer.dispose(gl);

        int[] tmp = new int[1];

        if ( quadPositionVboId != 0 ) {
            tmp[0] = quadPositionVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            quadPositionVboId = 0;
        }

        if ( quadUvVboId != 0 ) {
            tmp[0] = quadUvVboId;
            gl.glDeleteBuffers(1, tmp, 0);
            quadUvVboId = 0;
        }

        if ( quadVaoId != 0 ) {
            tmp[0] = quadVaoId;
            gl.glDeleteVertexArrays(1, tmp, 0);
            quadVaoId = 0;
        }
    }

    private static void ensureBuffers(GL4 gl)
    {
        if ( quadVaoId != 0 ) {
            return;
        }

        int[] tmp = new int[1];

        gl.glGenVertexArrays(1, tmp, 0);
        quadVaoId = tmp[0];

        gl.glGenBuffers(1, tmp, 0);
        quadPositionVboId = tmp[0];

        gl.glGenBuffers(1, tmp, 0);
        quadUvVboId = tmp[0];
    }
}
