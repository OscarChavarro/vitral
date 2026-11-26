package vsdk.toolkit.render.jogl;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.statistics.RenderingStatistics;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.material.RendererConfiguration;

/**
Renders any triangle mesh (given as non indexed arrays of positions, normals
and texture coordinates) with the fixed function pipeline of OpenGL 2,
honoring the bits of a `RendererConfiguration` (surfaces, wires, points,
normals, bounding volume and selection corners, and the shading type through
`Jogl2GeometryRenderer.prepareSurfaceQuality`). It is the common flow shared
by the mesh based renderers of each geometry (see `Jogl2GeometryRenderer`),
the JOGL2 counterpart of `Jogl4MeshRenderer` as the C++ `OpenGL1MeshRenderer`
is of `OpenGL4MeshRenderer`.

Unlike `Jogl4MeshRenderer` (and the C++ `OpenGL1MeshRenderer`), lights,
material, texture and the model transform are not handled here: as for every
JOGL2 geometry renderer, they are part of the current OpenGL state, set by
the caller (i.e. `Jogl2SimpleBodyRenderer`). Bump maps, Phong and
Cook-Torrance shading are not available in the fixed function pipeline.

The vertices go to OpenGL as client side vertex arrays, compiled in a
display list the first time a mesh is drawn; the list must be released with
`release` while the context that created it is current.
*/
public class Jogl2MeshRenderer extends Jogl2Renderer {
    private static final float SURFACE_POLYGON_OFFSET_FACTOR = 1.0f;
    private static final float SURFACE_POLYGON_OFFSET_UNITS = 1.0f;
    private static final float LINE_POLYGON_OFFSET_FACTOR = -1.0f;
    private static final float LINE_POLYGON_OFFSET_UNITS = -1.0f;
    private static final float VERTEX_NORMAL_SCALE = 0.10f;
    private static final float TRIANGLE_NORMAL_SCALE = 0.12f;
    private static final float NORMAL_START_EPSILON = 0.002f;
    private static final float[] VERTEX_NORMAL_COLOR = new float[] { 1.0f, 1.0f, 0.0f };
    private static final float[] TRIANGLE_NORMAL_COLOR = new float[] { 0.0f, 1.0f, 1.0f };

    private Jogl2MeshRenderer() {
    }

    /**
    A triangle mesh and the display list that draws it.
    */
    public static final class Mesh {
        private final float[] positions;
        private final float[] normals;
        private final float[] uvs;
        private final int vertexCount;
        /// Vertices of the sides the geometry defines; the rest (if any) are
        /// back sides added to see open surfaces from both sides
        private int frontVertexCount;
        /// Approximate size of the mesh, used to scale the normal overlays
        private final double characteristicSize;

        private FloatBuffer positionBuffer;
        private FloatBuffer normalBuffer;
        private FloatBuffer uvBuffer;
        private int displayListId;
        private boolean uploaded;

        private float[] vertexNormalLinePositions;
        private float[] triangleNormalLinePositions;

        /**
        @param positions x,y,z of each vertex; three consecutive vertices
        make a triangle
        @param normals x,y,z of the normal of each vertex
        @param uvs u,v of each vertex
        @param characteristicSize approximate size of the mesh, used to scale
        the normal overlays
        */
        public Mesh(float[] positions, float[] normals, float[] uvs,
            double characteristicSize)
        {
            this.positions = positions;
            this.normals = normals;
            this.uvs = uvs;
            this.vertexCount = positions.length / 3;
            this.frontVertexCount = vertexCount;
            this.characteristicSize = characteristicSize;
            this.displayListId = 0;
            this.uploaded = false;
        }

        /**
        Marks the vertices after the first `count` ones as back sides of open
        surfaces: they are drawn as surfaces and wires, but their points and
        normals are not shown again.
        @param count number of vertices of the front sides
        */
        void setFrontVertexCount(int count)
        {
            frontVertexCount = Math.max(0, Math.min(count, vertexCount));
        }

        public int getVertexCount()
        {
            return vertexCount;
        }

        public int getFrontVertexCount()
        {
            return frontVertexCount;
        }

        public float[] getPositions()
        {
            return positions;
        }

        public float[] getNormals()
        {
            return normals;
        }

        public float[] getUvs()
        {
            return uvs;
        }
    }

    /**
    Draws a mesh, with the passes selected by the configuration, using the
    lights, material, texture and transformations of the current OpenGL
    state.

    @param gl OpenGL context
    @param mesh mesh to draw
    @param geometry geometry the mesh comes from, used for the bounding
    volume and the selection corners, or null
    @param camera camera that views the mesh
    @param quality bits of rendering configuration
    */
    public static void draw(GL2 gl, Mesh mesh, Geometry geometry,
        Camera camera, RendererConfiguration quality)
    {
        if ( mesh == null || quality == null ) {
            return;
        }
        upload(gl, mesh);
        RenderingStatistics.accumulatePrimitiveCount(VSDK.TRIANGLE,
            mesh.vertexCount / 3);

        gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_POLYGON_BIT |
            GL2.GL_LIGHTING_BIT | GL2.GL_DEPTH_BUFFER_BIT |
            GL2.GL_CURRENT_BIT | GL2.GL_LINE_BIT | GL2.GL_POINT_BIT);
        gl.glEnable(GL2.GL_NORMALIZE);

        if ( quality.isSurfacesSet() ) {
            Jogl2GeometryRenderer.prepareSurfaceQuality(gl, quality);
            // Pass 1: surfaces, pushed slightly backwards to avoid
            // z-fighting with the overlay passes (wires / points)
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthMask(true);
            gl.glDepthFunc(GL.GL_LESS);
            gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset(SURFACE_POLYGON_OFFSET_FACTOR,
                SURFACE_POLYGON_OFFSET_UNITS);
            gl.glEnable(GL.GL_CULL_FACE);
            gl.glCullFace(GL.GL_BACK);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
            renderMesh(gl, mesh);
            gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
        }

        if ( quality.isWiresSet() ) {
            // Pass 2: wires, with depth test but biased in front of surfaces
            ColorRgb c = quality.getWireColor();
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL.GL_TEXTURE_2D);
            gl.glColor3d(c.r(), c.g(), c.b());
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthMask(false);
            gl.glDepthFunc(GL.GL_LEQUAL);
            gl.glEnable(GL2GL3.GL_POLYGON_OFFSET_LINE);
            gl.glPolygonOffset(LINE_POLYGON_OFFSET_FACTOR,
                LINE_POLYGON_OFFSET_UNITS);
            gl.glDisable(GL.GL_CULL_FACE);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
            gl.glLineWidth(1.0f);
            renderMesh(gl, mesh);
            gl.glDisable(GL2GL3.GL_POLYGON_OFFSET_LINE);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
        }

        if ( quality.isPointsSet() ) {
            // Pass 3: points, last, tested against surfaces (and not
            // writing depth), so they appear above the wires
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL.GL_TEXTURE_2D);
            gl.glColor3d(1, 0, 0);
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthMask(false);
            gl.glDepthFunc(GL.GL_LEQUAL);
            gl.glDisable(GL.GL_CULL_FACE);
            gl.glPointSize(4.0f);
            enableArrays(gl, mesh);
            gl.glDrawArrays(GL.GL_POINTS, 0, mesh.frontVertexCount);
            disableArrays(gl);
        }

        if ( quality.isNormalsSet() || quality.isTrianglesNormalsSet() ) {
            drawNormalOverlays(gl, mesh, quality);
        }
        gl.glPopAttrib();

        if ( geometry != null && quality.isBoundingVolumeSet() ) {
            Jogl2GeometryRenderer.drawMinMaxBox(gl, geometry, quality);
        }
        if ( geometry != null && quality.isSelectionCornersSet() ) {
            Jogl2GeometryRenderer.drawSelectionCorners(gl, geometry, quality);
        }
    }

    /**
    Releases the display list of a mesh. PRE: the context that created it is
    current.
    @param gl OpenGL context that created the mesh resources
    @param mesh mesh to release
    */
    public static void release(GL2 gl, Mesh mesh)
    {
        if ( mesh == null || !mesh.uploaded ) {
            return;
        }
        if ( mesh.displayListId != 0 ) {
            gl.glDeleteLists(mesh.displayListId, 1);
        }
        mesh.uploaded = false;
        mesh.displayListId = 0;
    }

    private static void enableArrays(GL2 gl, Mesh mesh)
    {
        if ( mesh.positionBuffer == null ) {
            mesh.positionBuffer = Buffers.newDirectFloatBuffer(mesh.positions);
            mesh.normalBuffer = mesh.normals.length == mesh.positions.length ?
                Buffers.newDirectFloatBuffer(mesh.normals) : null;
            mesh.uvBuffer = mesh.uvs.length / 2 == mesh.positions.length / 3 ?
                Buffers.newDirectFloatBuffer(mesh.uvs) : null;
        }
        mesh.positionBuffer.rewind();
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL.GL_FLOAT, 0, mesh.positionBuffer);
        if ( mesh.normalBuffer != null ) {
            mesh.normalBuffer.rewind();
            gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
            gl.glNormalPointer(GL.GL_FLOAT, 0, mesh.normalBuffer);
        }
        if ( mesh.uvBuffer != null ) {
            mesh.uvBuffer.rewind();
            gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
            gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, mesh.uvBuffer);
        }
    }

    private static void disableArrays(GL2 gl)
    {
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
        gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
    }

    private static void renderMesh(GL2 gl, Mesh mesh)
    {
        if ( mesh.displayListId != 0 ) {
            gl.glCallList(mesh.displayListId);
            return;
        }
        enableArrays(gl, mesh);
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, mesh.vertexCount);
        disableArrays(gl);
    }

    /**
    Compiles the triangles of the mesh in a display list: the vertex arrays
    are copied into it, so later frames do not send them again.
    */
    private static void upload(GL2 gl, Mesh mesh)
    {
        if ( mesh.uploaded ) {
            return;
        }
        mesh.uploaded = true;
        if ( mesh.vertexCount <= 0 ) {
            return;
        }
        mesh.displayListId = gl.glGenLists(1);
        if ( mesh.displayListId == 0 ) {
            return;
        }
        gl.glNewList(mesh.displayListId, GL2.GL_COMPILE);
        enableArrays(gl, mesh);
        gl.glDrawArrays(GL.GL_TRIANGLES, 0, mesh.vertexCount);
        disableArrays(gl);
        gl.glEndList();
    }

    private static void drawLines(GL2 gl, float[] lines, float[] color)
    {
        gl.glColor3f(color[0], color[1], color[2]);
        gl.glBegin(GL.GL_LINES);
        for ( int i = 0; i + 2 < lines.length; i += 3 ) {
            gl.glVertex3f(lines[i], lines[i + 1], lines[i + 2]);
        }
        gl.glEnd();
    }

    private static void addLine(float[] lines, int at, float sx, float sy,
        float sz, float nx, float ny, float nz, float length)
    {
        lines[at] = sx;
        lines[at + 1] = sy;
        lines[at + 2] = sz;
        lines[at + 3] = sx + nx * length;
        lines[at + 4] = sy + ny * length;
        lines[at + 5] = sz + nz * length;
    }

    private static void drawNormalOverlays(GL2 gl, Mesh mesh,
        RendererConfiguration quality)
    {
        if ( mesh.positions.length == 0 || mesh.normals.length == 0 ) {
            return;
        }
        double size = Math.max(mesh.characteristicSize, 1e-6);
        float epsilon = (float)size * NORMAL_START_EPSILON;

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthMask(false);
        gl.glDepthFunc(GL.GL_LEQUAL);
        gl.glDisable(GL.GL_CULL_FACE);
        gl.glLineWidth(1.0f);

        if ( quality.isNormalsSet() ) {
            if ( mesh.vertexNormalLinePositions == null ) {
                float length = (float)size * VERTEX_NORMAL_SCALE;
                float[] lines = new float[mesh.frontVertexCount * 6];
                for ( int i = 0; i < mesh.frontVertexCount; i++ ) {
                    float[] p = mesh.positions;
                    float[] n = mesh.normals;
                    int k = i * 3;
                    addLine(lines, i * 6, p[k] + n[k] * epsilon,
                        p[k + 1] + n[k + 1] * epsilon,
                        p[k + 2] + n[k + 2] * epsilon,
                        n[k], n[k + 1], n[k + 2], length);
                }
                mesh.vertexNormalLinePositions = lines;
            }
            drawLines(gl, mesh.vertexNormalLinePositions, VERTEX_NORMAL_COLOR);
        }

        if ( quality.isTrianglesNormalsSet() ) {
            if ( mesh.triangleNormalLinePositions == null ) {
                float length = (float)size * TRIANGLE_NORMAL_SCALE;
                int triangleCount = mesh.frontVertexCount / 3;
                float[] lines = new float[triangleCount * 6];
                float[] p = mesh.positions;
                for ( int t = 0; t < triangleCount; t++ ) {
                    int k = t * 9;
                    float cx = (p[k] + p[k + 3] + p[k + 6]) / 3.0f;
                    float cy = (p[k + 1] + p[k + 4] + p[k + 7]) / 3.0f;
                    float cz = (p[k + 2] + p[k + 5] + p[k + 8]) / 3.0f;
                    float ux = p[k + 3] - p[k];
                    float uy = p[k + 4] - p[k + 1];
                    float uz = p[k + 5] - p[k + 2];
                    float vx = p[k + 6] - p[k];
                    float vy = p[k + 7] - p[k + 1];
                    float vz = p[k + 8] - p[k + 2];
                    float nx = uy * vz - uz * vy;
                    float ny = uz * vx - ux * vz;
                    float nz = ux * vy - uy * vx;
                    float norm = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if ( norm <= 1e-12f ) {
                        nx = 0.0f;
                        ny = 0.0f;
                        nz = 1.0f;
                    }
                    else {
                        nx /= norm;
                        ny /= norm;
                        nz /= norm;
                    }
                    if ( nx * cx + ny * cy + nz * cz < 0.0f ) {
                        nx = -nx;
                        ny = -ny;
                        nz = -nz;
                    }
                    addLine(lines, t * 6, cx + nx * epsilon, cy + ny * epsilon,
                        cz + nz * epsilon, nx, ny, nz, length);
                }
                mesh.triangleNormalLinePositions = lines;
            }
            drawLines(gl, mesh.triangleNormalLinePositions, TRIANGLE_NORMAL_COLOR);
        }
    }
}
