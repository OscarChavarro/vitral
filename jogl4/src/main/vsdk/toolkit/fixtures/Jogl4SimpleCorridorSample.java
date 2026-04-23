package vsdk.toolkit.fixtures;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.render.jogl.Jogl4MatrixRenderer;
import vsdk.toolkit.render.jogl.Jogl4ShaderProgramUtil;

public class Jogl4SimpleCorridorSample
{
    private double a;
    private int na;
    private double b;
    private int nb;
    private double c;
    private int nc;
    private double interSpace;

    private boolean initialized;
    private int shaderProgramId;
    private int vertexArrayId;
    private int positionBufferId;
    private int colorBufferId;
    private int modelViewProjectionLocalLoc;
    private int withTextureLoc;
    private int withVertexColorsLoc;
    private int diffuseColorLoc;
    private int vertexCount;

    public Jogl4SimpleCorridorSample()
    {
        a = 6;
        na = 6;
        b = 20;
        nb = 20;
        c = 4;
        nc = 4;
        interSpace = 0.05;
    }

    public void drawGL(GL4 gl, Matrix4x4 modelViewProjection)
    {
        if ( !initialized ) {
            initialize(gl);
        }

        gl.glEnable(GL4.GL_CULL_FACE);
        gl.glCullFace(GL4.GL_BACK);

        gl.glUseProgram(shaderProgramId);
        gl.glUniformMatrix4fv(
            modelViewProjectionLocalLoc,
            1,
            false,
            Jogl4MatrixRenderer.toColumnMajorFloatArray(modelViewProjection),
            0);

        if ( withTextureLoc >= 0 ) {
            gl.glUniform1i(withTextureLoc, 0);
        }
        if ( withVertexColorsLoc >= 0 ) {
            gl.glUniform1i(withVertexColorsLoc, 1);
        }
        if ( diffuseColorLoc >= 0 ) {
            gl.glUniform3f(diffuseColorLoc, 1.0f, 1.0f, 1.0f);
        }

        gl.glBindVertexArray(vertexArrayId);
        gl.glDrawArrays(GL4.GL_TRIANGLES, 0, vertexCount);
        gl.glBindVertexArray(0);
        gl.glUseProgram(0);
    }

    public void dispose(GL4 gl)
    {
        int[] tmp = new int[1];

        if ( positionBufferId != 0 ) {
            tmp[0] = positionBufferId;
            gl.glDeleteBuffers(1, tmp, 0);
            positionBufferId = 0;
        }

        if ( colorBufferId != 0 ) {
            tmp[0] = colorBufferId;
            gl.glDeleteBuffers(1, tmp, 0);
            colorBufferId = 0;
        }

        if ( vertexArrayId != 0 ) {
            tmp[0] = vertexArrayId;
            gl.glDeleteVertexArrays(1, tmp, 0);
            vertexArrayId = 0;
        }

        if ( shaderProgramId != 0 ) {
            gl.glDeleteProgram(shaderProgramId);
            shaderProgramId = 0;
        }

        initialized = false;
        vertexCount = 0;
    }

    private void initialize(GL4 gl)
    {
        float[][] geometry = buildGeometry();
        float[] positions = geometry[0];
        float[] colors = geometry[1];

        shaderProgramId = Jogl4ShaderProgramUtil.createProgramFromFiles(
            gl,
            "constantvertexshader.glsl",
            "constantpixelshader.glsl");

        modelViewProjectionLocalLoc = gl.glGetUniformLocation(shaderProgramId, "modelViewProjectionLocal");
        if ( modelViewProjectionLocalLoc < 0 ) {
            throw new IllegalStateException("Missing modelViewProjectionLocal uniform");
        }
        withTextureLoc = gl.glGetUniformLocation(shaderProgramId, "withTexture");
        withVertexColorsLoc = gl.glGetUniformLocation(shaderProgramId, "withVertexColors");
        diffuseColorLoc = gl.glGetUniformLocation(shaderProgramId, "diffuseColor");

        int[] tmp = new int[1];

        gl.glGenVertexArrays(1, tmp, 0);
        vertexArrayId = tmp[0];
        gl.glBindVertexArray(vertexArrayId);

        gl.glGenBuffers(1, tmp, 0);
        positionBufferId = tmp[0];
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, positionBufferId);
        FloatBuffer posBuffer = Buffers.newDirectFloatBuffer(positions);
        gl.glBufferData(
            GL4.GL_ARRAY_BUFFER,
            (long)positions.length * Float.BYTES,
            posBuffer,
            GL4.GL_STATIC_DRAW);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 0, 0L);

        gl.glGenBuffers(1, tmp, 0);
        colorBufferId = tmp[0];
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colorBufferId);
        FloatBuffer colorBuffer = Buffers.newDirectFloatBuffer(colors);
        gl.glBufferData(
            GL4.GL_ARRAY_BUFFER,
            (long)colors.length * Float.BYTES,
            colorBuffer,
            GL4.GL_STATIC_DRAW);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, 0L);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
        gl.glBindVertexArray(0);

        vertexCount = positions.length / 3;
        initialized = true;
    }

    private float[][] buildGeometry()
    {
        List<Float> positions = new ArrayList<>();
        List<Float> colors = new ArrayList<>();

        appendTilesCenter(positions, colors, 0.5f, 0.5f, 0.9f, 0, false, 0);
        for ( int i = 0; i < 4; i++ ) {
            appendTilesLong(positions, colors, 0.5f, 0.5f, 0.9f, 90 * i, false, 0);
        }

        appendTilesCenter(positions, colors, 0.0f, 0.0f, 1.0f, 0, true, c);
        for ( int i = 0; i < 4; i++ ) {
            appendTilesLong(positions, colors, 0.0f, 0.0f, 1.0f, 90 * i, true, c);
        }

        for ( int i = 0; i < 4; i++ ) {
            switch ( i ) {
                case 0 -> appendTilesWallA(positions, colors, 0.9f, 0.5f, 0.5f, 90 * i);
                case 1 -> appendTilesWallA(positions, colors, 0.5f, 0.9f, 0.5f, 90 * i);
                case 2 -> appendTilesWallA(positions, colors, 1.0f, 0.0f, 0.0f, 90 * i);
                default -> appendTilesWallA(positions, colors, 0.0f, 1.0f, 0.0f, 90 * i);
            }
        }

        for ( int i = 0; i < 4; i++ ) {
            appendTilesWallB(positions, colors, 0.9f, 0.5f, 0.8f, 90 * i);
            appendTilesWallC(positions, colors, 0.9f, 0.5f, 0.8f, 90 * i);
        }

        return new float[][] { toArray(positions), toArray(colors) };
    }

    private void appendTilesCenter(
        List<Float> positions,
        List<Float> colors,
        float r,
        float g,
        float bColor,
        double rotZDeg,
        boolean flipYZ,
        double translateZ)
    {
        double da = a / ((double)na);
        double epsilon = 0.005;

        for ( int i = 0; i < na; i++ ) {
            double x = -a/2 + i * da;
            for ( int j = 0; j < na; j++ ) {
                double y = -a/2 + j * da;
                addQuad(
                    positions,
                    colors,
                    r,
                    g,
                    bColor,
                    x + interSpace/2, y + interSpace/2, -epsilon,
                    x + da - interSpace/2, y + interSpace/2, -epsilon,
                    x + da - interSpace/2, y + da - interSpace/2, -epsilon,
                    x + interSpace/2, y + da - interSpace/2, -epsilon,
                    rotZDeg,
                    flipYZ,
                    translateZ);
            }
        }
    }

    private void appendTilesLong(
        List<Float> positions,
        List<Float> colors,
        float r,
        float g,
        float bColor,
        double rotZDeg,
        boolean flipYZ,
        double translateZ)
    {
        double da = a / ((double)na);
        double db = b / ((double)nb);
        double epsilon = 0.001;

        for ( int i = 0; i < nb; i++ ) {
            double x = -a/2 - b + i * db;
            for ( int j = 0; j < na; j++ ) {
                double y = -a/2 + j * da;
                addQuad(
                    positions,
                    colors,
                    r,
                    g,
                    bColor,
                    x + interSpace/2, y + interSpace/2, -epsilon,
                    x + da - interSpace/2, y + interSpace/2, -epsilon,
                    x + da - interSpace/2, y + da - interSpace/2, -epsilon,
                    x + interSpace/2, y + da - interSpace/2, -epsilon,
                    rotZDeg,
                    flipYZ,
                    translateZ);
            }
        }
    }

    private void appendTilesWallA(
        List<Float> positions,
        List<Float> colors,
        float r,
        float g,
        float bColor,
        double rotZDeg)
    {
        double da = a / ((double)na);
        double dc = c / ((double)nc);

        for ( int i = 0; i < nc; i++ ) {
            double z = i * dc;
            for ( int j = 0; j < na; j++ ) {
                double y = -a/2 + j * da;
                addQuad(
                    positions,
                    colors,
                    r,
                    g,
                    bColor,
                    -a/2 - b, y + interSpace/2, z + dc - interSpace/2,
                    -a/2 - b, y + interSpace/2, z + interSpace/2,
                    -a/2 - b, y + da - interSpace/2, z + interSpace/2,
                    -a/2 - b, y + da - interSpace/2, z + dc - interSpace/2,
                    rotZDeg,
                    false,
                    0);
            }
        }
    }

    private void appendTilesWallB(
        List<Float> positions,
        List<Float> colors,
        float r,
        float g,
        float bColor,
        double rotZDeg)
    {
        double db = b / ((double)nb);
        double dc = c / ((double)nc);

        for ( int i = 0; i < nc; i++ ) {
            double z = i * dc;
            for ( int j = 0; j < nb; j++ ) {
                double y = a/2 + j * db;
                addQuad(
                    positions,
                    colors,
                    r,
                    g,
                    bColor,
                    -a/2, y + interSpace/2, z + dc - interSpace/2,
                    -a/2, y + interSpace/2, z + interSpace/2,
                    -a/2, y + db - interSpace/2, z + interSpace/2,
                    -a/2, y + db - interSpace/2, z + dc - interSpace/2,
                    rotZDeg,
                    false,
                    0);
            }
        }
    }

    private void appendTilesWallC(
        List<Float> positions,
        List<Float> colors,
        float r,
        float g,
        float bColor,
        double rotZDeg)
    {
        double db = b / ((double)nb);
        double dc = c / ((double)nc);

        for ( int i = 0; i < nb; i++ ) {
            double x = -a/2 - b + i * db;
            for ( int j = 0; j < nc; j++ ) {
                double z = j * dc;
                addQuad(
                    positions,
                    colors,
                    r,
                    g,
                    bColor,
                    x + interSpace/2, a/2, z + interSpace/2,
                    x + db - interSpace/2, a/2, z + interSpace/2,
                    x + db - interSpace/2, a/2, z + dc - interSpace/2,
                    x + interSpace/2, a/2, z + dc - interSpace/2,
                    rotZDeg,
                    false,
                    0);
            }
        }
    }

    private void addQuad(
        List<Float> positions,
        List<Float> colors,
        float r,
        float g,
        float bColor,
        double x1,
        double y1,
        double z1,
        double x2,
        double y2,
        double z2,
        double x3,
        double y3,
        double z3,
        double x4,
        double y4,
        double z4,
        double rotZDeg,
        boolean flipYZ,
        double translateZ)
    {
        addVertex(positions, colors, x1, y1, z1, r, g, bColor, rotZDeg, flipYZ, translateZ);
        addVertex(positions, colors, x2, y2, z2, r, g, bColor, rotZDeg, flipYZ, translateZ);
        addVertex(positions, colors, x3, y3, z3, r, g, bColor, rotZDeg, flipYZ, translateZ);

        addVertex(positions, colors, x1, y1, z1, r, g, bColor, rotZDeg, flipYZ, translateZ);
        addVertex(positions, colors, x3, y3, z3, r, g, bColor, rotZDeg, flipYZ, translateZ);
        addVertex(positions, colors, x4, y4, z4, r, g, bColor, rotZDeg, flipYZ, translateZ);
    }

    private void addVertex(
        List<Float> positions,
        List<Float> colors,
        double x,
        double y,
        double z,
        float r,
        float g,
        float bColor,
        double rotZDeg,
        boolean flipYZ,
        double translateZ)
    {
        double tx = x;
        double ty = y;
        double tz = z;

        if ( flipYZ ) {
            ty = -ty;
            tz = -tz;
        }

        double angle = Math.toRadians(rotZDeg);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double rx = tx * cos - ty * sin;
        double ry = tx * sin + ty * cos;
        double rz = tz + translateZ;

        positions.add((float)rx);
        positions.add((float)ry);
        positions.add((float)rz);

        colors.add(r);
        colors.add(g);
        colors.add(bColor);
    }

    private static float[] toArray(List<Float> input)
    {
        float[] out = new float[input.size()];
        for ( int i = 0; i < input.size(); i++ ) {
            out[i] = input.get(i);
        }
        return out;
    }
}
