package vsdk.toolkit.render.jogl;

// Java basic classes
import java.nio.FloatBuffer;

// JOGL classes
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.logging.Logger;
import vsdk.toolkit.media.RGBImageUncompressed;
import vsdk.toolkit.media.ZBuffer;

/**
Draws an image computed outside OpenGL (i.e. by
`vsdk.toolkit.render.raytracing.ParallelRaytracer`) together with its depth
buffer: the image goes to the color buffer and the depth values to the
OpenGL depth buffer, over the lower left corner of the current viewport, one
texel per pixel. Geometry rasterized afterwards (grids, gizmos, editor
feedback, or any body not raytraced) is then depth tested against the
image, which allows mixing raytraced and rasterized objects in one view.

The depth buffer must hold window space depth values for the camera used to
rasterize the rest of the view (see
`vsdk.toolkit.render.raytracing.DepthBufferMode.OPENGL_DEPTH`). Its row 0 is
the top row of the image, as the rows of `RGBImageUncompressed.getPixel`.

The textures are reused among frames while the image size does not change.
*/
public final class Jogl4ColorDepthImageRenderer
{
    private static final String VERTEX_SHADER_FILE = "colorDepthImageVertexShader.glsl";
    private static final String FRAGMENT_SHADER_FILE = "colorDepthImagePixelShader.glsl";

    private static int programId;
    private static int colorTextureLocation;
    private static int depthTextureLocation;
    private static int vaoId;
    private static int positionVboId;
    private static int uvVboId;
    private static int colorTextureId;
    private static int depthTextureId;
    private static int textureXSize;
    private static int textureYSize;
    private static FloatBuffer depthUploadBuffer;

    private Jogl4ColorDepthImageRenderer()
    {
    }

    /**
    Draws the image in the color buffer and its depth in the depth buffer.
    Depth test, depth function and face culling are left as `GL_LESS`
    with depth test enabled and culling disabled.
    @param gl OpenGL context
    @param image color of each pixel
    @param depth window space depth of each pixel, of the size of `image`,
    or null to draw only the color (without touching the depth buffer)
    */
    public static void draw(GL4 gl, RGBImageUncompressed image, ZBuffer depth)
    {
        if ( gl == null || image == null ||
             image.getXSize() <= 0 || image.getYSize() <= 0 ) {
            return;
        }
        if ( depth != null && (depth.getXSize() != image.getXSize() ||
                               depth.getYSize() != image.getYSize()) ) {
            Logger.reportMessage(null, VSDK.WARNING, "draw",
                "Depth buffer size does not match the image size, drawing color only");
            depth = null;
        }
        if ( depth == null ) {
            Jogl4ImageRenderer.unload(gl, image);
            Jogl4ImageRenderer.draw(gl, image);
            return;
        }

        ensureInitialized(gl);
        uploadTextures(gl, image, depth);

        gl.glDisable(GL4.GL_CULL_FACE);
        gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);
        // Depth writes need the depth test enabled; ALWAYS replaces what was there
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDepthFunc(GL4.GL_ALWAYS);
        gl.glDepthMask(true);

        gl.glUseProgram(programId);
        gl.glActiveTexture(GL4.GL_TEXTURE0);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, colorTextureId);
        gl.glUniform1i(colorTextureLocation, 0);
        gl.glActiveTexture(GL4.GL_TEXTURE1);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, depthTextureId);
        gl.glUniform1i(depthTextureLocation, 1);

        drawLowerLeftQuad(gl, image.getXSize(), image.getYSize());

        gl.glActiveTexture(GL4.GL_TEXTURE1);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
        gl.glActiveTexture(GL4.GL_TEXTURE0);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
        gl.glUseProgram(0);
        gl.glDepthFunc(GL4.GL_LESS);
    }

    private static void drawLowerLeftQuad(GL4 gl, int width, int height)
    {
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);
        float w = 2.0f * ((float)width / (float)Math.max(viewport[2], 1));
        float h = 2.0f * ((float)height / (float)Math.max(viewport[3], 1));
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

        gl.glBindVertexArray(vaoId);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, positionVboId);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long)positions.length * Float.BYTES,
            Buffers.newDirectFloatBuffer(positions), GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0L);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, uvVboId);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long)uvCoordinates.length * Float.BYTES,
            Buffers.newDirectFloatBuffer(uvCoordinates), GL4.GL_STREAM_DRAW);
        gl.glEnableVertexAttribArray(2);
        gl.glVertexAttribPointer(2, 2, GL4.GL_FLOAT, false, 0, 0L);

        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, 6);

        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(2);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);
    }

    private static void uploadTextures(GL4 gl, RGBImageUncompressed image, ZBuffer depth)
    {
        int xSize = image.getXSize();
        int ySize = image.getYSize();
        boolean resized = xSize != textureXSize || ySize != textureYSize;

        // The raw image stores its bottom row first, as OpenGL textures, while
        // the depth buffer stores its top row first: depth rows are flipped
        int count = xSize * ySize;
        if ( depthUploadBuffer == null || depthUploadBuffer.capacity() < count ) {
            depthUploadBuffer = Buffers.newDirectFloatBuffer(count);
        }
        float[] values = depth.getZBuffer();
        depthUploadBuffer.clear();
        for ( int row = ySize - 1; row >= 0; row-- ) {
            depthUploadBuffer.put(values, row * xSize, xSize);
        }
        depthUploadBuffer.flip();

        gl.glPixelStorei(GL4.GL_UNPACK_ALIGNMENT, 1);
        gl.glActiveTexture(GL4.GL_TEXTURE0);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, colorTextureId);
        if ( resized ) {
            gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_RGB8, xSize, ySize, 0,
                GL4.GL_RGB, GL4.GL_UNSIGNED_BYTE, image.getRawImageDirectBuffer());
        }
        else {
            gl.glTexSubImage2D(GL4.GL_TEXTURE_2D, 0, 0, 0, xSize, ySize,
                GL4.GL_RGB, GL4.GL_UNSIGNED_BYTE, image.getRawImageDirectBuffer());
        }

        gl.glBindTexture(GL4.GL_TEXTURE_2D, depthTextureId);
        if ( resized ) {
            gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_R32F, xSize, ySize, 0,
                GL4.GL_RED, GL4.GL_FLOAT, depthUploadBuffer);
        }
        else {
            gl.glTexSubImage2D(GL4.GL_TEXTURE_2D, 0, 0, 0, xSize, ySize,
                GL4.GL_RED, GL4.GL_FLOAT, depthUploadBuffer);
        }
        gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);

        textureXSize = xSize;
        textureYSize = ySize;
    }

    private static int createTexture(GL4 gl)
    {
        int[] tmp = new int[1];

        gl.glGenTextures(1, tmp, 0);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, tmp[0]);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
        return tmp[0];
    }

    private static void ensureInitialized(GL4 gl)
    {
        if ( programId != 0 ) {
            return;
        }

        programId = Jogl4ShaderProgramUtil.createProgramFromFiles(
            gl, VERTEX_SHADER_FILE, FRAGMENT_SHADER_FILE);
        colorTextureLocation = gl.glGetUniformLocation(programId, "colorTexture");
        depthTextureLocation = gl.glGetUniformLocation(programId, "depthTexture");

        int[] tmp = new int[1];
        gl.glGenVertexArrays(1, tmp, 0);
        vaoId = tmp[0];
        gl.glGenBuffers(1, tmp, 0);
        positionVboId = tmp[0];
        gl.glGenBuffers(1, tmp, 0);
        uvVboId = tmp[0];

        colorTextureId = createTexture(gl);
        depthTextureId = createTexture(gl);
        textureXSize = 0;
        textureYSize = 0;
    }

    /**
    Releases the shader program, buffers and textures of this renderer.
    PRE: the OpenGL context is current.
    @param gl OpenGL context
    */
    public static void dispose(GL4 gl)
    {
        if ( programId == 0 ) {
            return;
        }
        int[] tmp = new int[1];

        gl.glDeleteProgram(programId);
        tmp[0] = positionVboId;
        gl.glDeleteBuffers(1, tmp, 0);
        tmp[0] = uvVboId;
        gl.glDeleteBuffers(1, tmp, 0);
        tmp[0] = vaoId;
        gl.glDeleteVertexArrays(1, tmp, 0);
        tmp[0] = colorTextureId;
        gl.glDeleteTextures(1, tmp, 0);
        tmp[0] = depthTextureId;
        gl.glDeleteTextures(1, tmp, 0);

        programId = 0;
        vaoId = 0;
        positionVboId = 0;
        uvVboId = 0;
        colorTextureId = 0;
        depthTextureId = 0;
        textureXSize = 0;
        textureYSize = 0;
    }
}
