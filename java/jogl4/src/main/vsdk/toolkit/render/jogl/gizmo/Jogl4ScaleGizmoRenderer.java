package vsdk.toolkit.render.jogl.gizmo;

import java.util.ArrayList;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.gizmo.ScaleGizmo;
import vsdk.toolkit.render.jogl.Jogl4ColoredPrimitiveRenderer;
import vsdk.toolkit.render.jogl.Jogl4LineRenderer;
import vsdk.toolkit.render.jogl.Jogl4Renderer;

/**
Renders a {@link ScaleGizmo} with the GL4 core pipeline, following the
technique `Jogl4TranslateGizmoRenderer` uses for `TranslateGizmo`:

- The three cylinders of its axes (see {@link ScaleGizmo#getElements()}) and
  the small cube at the tip of each one, as solid, colored triangles.
- The flat handles (the trapezoidal band of each two-axis group and the three
  triangles of the uniform one) only as their contour (see
  {@link ScaleGizmo#buildContourSegments()}), drawn as lines.
- The interior of the handle currently selected, as a translucent gray
  surface over everything else (see {@link ScaleGizmo#buildBandQuad(int)},
  {@link ScaleGizmo#buildUniformTriangles()}).

Every element is generated in world space and drawn with the projection
matrix of the camera that views the gizmo.

Usage (render thread, once per frame, after the transformation matrix and
scale of the gizmo have been set):
<pre>
    Jogl4ScaleGizmoRenderer.draw(gl, gizmo, camera);
</pre>
*/
public class Jogl4ScaleGizmoRenderer extends Jogl4Renderer {
    private static final int CONE_SLICES = 16;
    /// Opacity of the interior of the flat handle currently selected
    private static final float HANDLE_OPACITY = 0.55f;
    /// Depth bias (towards the viewer) of the contour of the flat handles
    private static final float CONTOUR_DEPTH_BIAS = -0.0002f;

    private static final double[] SLICE_COS = new double[CONE_SLICES + 1];
    private static final double[] SLICE_SIN = new double[CONE_SLICES + 1];

    /// The 8 corners of a box, indexed by (sx,sy,sz) sign combination as
    /// ((sx+1)/2)*4 + ((sy+1)/2)*2 + (sz+1)/2; each row is a face, given as a
    /// valid (non self-intersecting) GL_TRIANGLE_STRIP of 4 corner indexes
    private static final int[][] BOX_FACES = {
        {0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 4, 5}, {2, 3, 6, 7}, {0, 2, 4, 6}, {1, 3, 5, 7}
    };

    static {
        for ( int i = 0; i <= CONE_SLICES; i++ ) {
            double angle = 2*Math.PI*i/CONE_SLICES;

            SLICE_COS[i] = Math.cos(angle);
            SLICE_SIN[i] = Math.sin(angle);
        }
    }

    /**
    Draws the gizmo over the current contents of the surface.

    @param gl OpenGL context
    @param gizmo gizmo to draw; its transformation matrix must be set
    @param camera camera that views the gizmo
    */
    public static void draw(GL4 gl, ScaleGizmo gizmo, Camera camera)
    {
        if ( gl == null || gizmo == null || camera == null ) {
            return;
        }
        Matrix4x4d mvp = camera.calculateProjectionMatrix();

        gl.glDisable(GL4.GL_CULL_FACE);
        gl.glEnable(GL4.GL_DEPTH_TEST);
        gl.glDepthMask(true);
        gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);

        for ( SimpleBody element : gizmo.getElements() ) {
            Geometry g = element.getGeometry();

            if ( g instanceof Cone ) {
                drawShaft(gl, mvp, element, (Cone)g);
            }
            else if ( g instanceof Box ) {
                drawBox(gl, mvp, element, (Box)g);
            }
        }

        //- Contour of the flat handles -------------------------------------
        drawContour(gl, mvp, gizmo);

        //- Interior of the handle selected, translucent over everything -----
        gl.glEnable(GL4.GL_BLEND);
        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDepthMask(false);
        for ( int group : ScaleGizmo.BAND_GROUPS ) {
            if ( gizmo.isBandHighlighted(group) ) {
                Vector3Dd[] quad = gizmo.buildBandQuad(group);

                drawQuad(gl, mvp, ScaleGizmo.HANDLE_FILL_COLOR, HANDLE_OPACITY,
                    quad[0], quad[1], quad[2], quad[3]);
            }
        }
        if ( gizmo.isUniformHighlighted() ) {
            drawUniformHandle(gl, mvp, gizmo);
        }
        gl.glDepthMask(true);
        gl.glDisable(GL4.GL_BLEND);
    }

    /**
    Draws the outline of the two-axis bands and of the uniform handle (see
    `ScaleGizmo.buildContourSegments`), each half edge with the color the
    gizmo gives it.
    */
    private static void drawContour(GL4 gl, Matrix4x4d mvp, ScaleGizmo gizmo)
    {
        ArrayList<ScaleGizmo.ContourSegment> segments = gizmo.buildContourSegments();

        if ( segments.isEmpty() ) {
            return;
        }

        float[] positions = new float[segments.size()*2*3];
        float[] colors = new float[segments.size()*2*3];
        int vertex = 0;

        for ( ScaleGizmo.ContourSegment segment : segments ) {
            putVertex(positions, vertex, segment.start());
            putRgb(colors, vertex, segment.color());
            vertex++;
            putVertex(positions, vertex, segment.end());
            putRgb(colors, vertex, segment.color());
            vertex++;
        }
        // Slightly towards the viewer, so the translucent interior of a
        // selected handle does not z-fight with its own outline
        Jogl4LineRenderer.drawLines(gl, mvp, positions, colors,
            (float)gizmo.getLineWidth(), CONTOUR_DEPTH_BIAS);
    }

    /**
    Draws the lateral surface of a shaft: a cylinder (equal base and top
    radius) from the local origin of `element`, growing along its local +Z.
    */
    private static void drawShaft(GL4 gl, Matrix4x4d mvp, SimpleBody element, Cone cone)
    {
        double r1 = cone.getBaseRadius();
        double r2 = cone.getTopRadius();
        double h = cone.getHeight();
        ColorRgb c = element.getMaterial().getDiffuse();
        Matrix4x4d local = localTransform(element);
        float[] positions = new float[(CONE_SLICES + 1)*2*3];
        float[] colors = new float[(CONE_SLICES + 1)*2*4];

        for ( int i = 0; i <= CONE_SLICES; i++ ) {
            Vector3Dd base = local.multiply(new Vector3Dd(r1*SLICE_COS[i], r1*SLICE_SIN[i], 0));
            Vector3Dd top = local.multiply(new Vector3Dd(r2*SLICE_COS[i], r2*SLICE_SIN[i], h));

            putVertex(positions, 2*i, base);
            putColor(colors, 2*i, c, 1.0);
            putVertex(positions, 2*i + 1, top);
            putColor(colors, 2*i + 1, c, 1.0);
        }
        Jogl4ColoredPrimitiveRenderer.draw(gl, mvp, GL4.GL_TRIANGLE_STRIP, positions, colors);
    }

    /**
    Draws a box as 6 solid quads, centered at the local origin of `element`.
    */
    private static void drawBox(GL4 gl, Matrix4x4d mvp, SimpleBody element, Box box)
    {
        Vector3Dd size = box.getSize();
        double hx = size.x()/2;
        double hy = size.y()/2;
        double hz = size.z()/2;
        ColorRgb c = element.getMaterial().getDiffuse();
        Matrix4x4d local = localTransform(element);
        Vector3Dd[] corners = new Vector3Dd[8];
        int index = 0;

        for ( int sx = -1; sx <= 1; sx += 2 ) {
            for ( int sy = -1; sy <= 1; sy += 2 ) {
                for ( int sz = -1; sz <= 1; sz += 2 ) {
                    corners[index++] = local.multiply(new Vector3Dd(sx*hx, sy*hy, sz*hz));
                }
            }
        }

        for ( int[] face : BOX_FACES ) {
            drawQuad(gl, mvp, c, 1.0,
                corners[face[0]], corners[face[1]], corners[face[2]], corners[face[3]]);
        }
    }

    private static void drawQuad(GL4 gl, Matrix4x4d mvp, ColorRgb c, double alpha,
                                 Vector3Dd p0, Vector3Dd p1, Vector3Dd p2, Vector3Dd p3)
    {
        float[] positions = new float[4*3];
        float[] colors = new float[4*4];

        putVertex(positions, 0, p0);
        putVertex(positions, 1, p1);
        putVertex(positions, 2, p2);
        putVertex(positions, 3, p3);
        for ( int i = 0; i < 4; i++ ) {
            putColor(colors, i, c, alpha);
        }
        Jogl4ColoredPrimitiveRenderer.draw(gl, mvp, GL4.GL_TRIANGLE_STRIP, positions, colors);
    }

    /**
    Fills the three triangles of the uniform handle (see
    `ScaleGizmo.buildUniformTriangles`), one in each coordinate plane of the
    frame of the gizmo.
    */
    private static void drawUniformHandle(GL4 gl, Matrix4x4d mvp, ScaleGizmo gizmo)
    {
        Vector3Dd[] triangles = gizmo.buildUniformTriangles();
        ColorRgb c = ScaleGizmo.HANDLE_FILL_COLOR;
        float[] positions = new float[triangles.length*3];
        float[] colors = new float[triangles.length*4];

        for ( int i = 0; i < triangles.length; i++ ) {
            putVertex(positions, i, triangles[i]);
            putColor(colors, i, c, HANDLE_OPACITY);
        }
        Jogl4ColoredPrimitiveRenderer.draw(gl, mvp, GL4.GL_TRIANGLES, positions, colors);
    }

    private static Matrix4x4d localTransform(SimpleBody element)
    {
        Vector3Dd position = element.getPosition();

        return new Matrix4x4d().translation(position).multiply(element.getRotation());
    }

    private static void putVertex(float[] positions, int index, Vector3Dd v)
    {
        positions[3*index] = (float)v.x();
        positions[3*index + 1] = (float)v.y();
        positions[3*index + 2] = (float)v.z();
    }

    private static void putRgb(float[] colors, int index, ColorRgb c)
    {
        colors[3*index] = (float)c.r();
        colors[3*index + 1] = (float)c.g();
        colors[3*index + 2] = (float)c.b();
    }

    private static void putColor(float[] colors, int index, ColorRgb c, double alpha)
    {
        colors[4*index] = (float)c.r();
        colors[4*index + 1] = (float)c.g();
        colors[4*index + 2] = (float)c.b();
        colors[4*index + 3] = (float)alpha;
    }
}
