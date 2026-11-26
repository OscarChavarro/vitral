package vsdk.toolkit.render.jogl.volume;

import java.util.ArrayList;

import com.jogamp.opengl.GL2;

import vsdk.toolkit.common.linealAlgebra.Vector2Dd;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.Prism;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.render.jogl.Jogl2GeometryRenderer;
import vsdk.toolkit.render.jogl.Jogl2MeshBuilder;
import vsdk.toolkit.render.jogl.Jogl2MeshRenderer;
import vsdk.toolkit.render.jogl.Jogl2Renderer;

/**
Renders a `Prism` with the fixed function pipeline (OpenGL 2): it is tessellated by `buildMesh` into
its walls (one flat quadrilateral per edge of the base) and its two caps (the
base triangulated by `Prism.triangulateBase`, so concave bases work), and
the mesh is cached by `Jogl2GeometryRenderer`, identified by the
parameters of the prism.
*/
public class Jogl2PrismRenderer extends Jogl2Renderer {
    private Jogl2PrismRenderer() {
    }

    /**
    Draws the prism, with the passes selected by the configuration (see
    `Jogl2MeshRenderer`), using the lights, material, texture and
    transformations of the current OpenGL state.

    @param gl OpenGL context
    @param prism prism to draw
    @param camera camera that views the prism
    @param quality bits of rendering configuration
    */
    public static void draw(GL2 gl, Prism prism, Camera camera,
        RendererConfiguration quality)
    {
        Jogl2GeometryRenderer.drawMesh(gl, prism, camera, quality);
    }

    /**
    @param prism a prism
    @return a string that identifies the mesh of the prism
    */
    public static String meshKey(Prism prism)
    {
        ArrayList<Vector2Dd> base = prism.getBase();
        StringBuilder key = new StringBuilder();
        key.append("prism/").append(prism.getHeight()).append('/')
            .append(base.size());
        for ( int i = 0; i < base.size(); i++ ) {
            key.append('/').append(base.get(i).x).append('/')
                .append(base.get(i).y);
        }
        return key.toString();
    }

    /**
    @param prism prism to tessellate
    @return the mesh of the walls and the caps of the prism, or null if the
    prism has no valid parameters
    */
    public static Jogl2MeshRenderer.Mesh buildMesh(Prism prism)
    {
        ArrayList<Integer> triangles = new ArrayList<Integer>();
        if ( !prism.hasValidParameters() || prism.triangulateBase(triangles) <= 0 ) {
            return null;
        }
        ArrayList<Vector2Dd> base = prism.getBase();
        int n = base.size();
        double h = prism.getHeight();
        double r = prism.getCircumradius();
        Jogl2MeshBuilder builder = new Jogl2MeshBuilder(Math.max(2 * r, h));

        // Texture coordinates as `Prism.doExtraInformation`: the caps are
        // mapped from the box of the base, the walls around the perimeter
        double minX = base.get(0).x;
        double minY = base.get(0).y;
        double maxX = minX;
        double maxY = minY;
        for ( int i = 1; i < n; i++ ) {
            minX = Math.min(minX, base.get(i).x);
            minY = Math.min(minY, base.get(i).y);
            maxX = Math.max(maxX, base.get(i).x);
            maxY = Math.max(maxY, base.get(i).y);
        }
        double width = maxX - minX;
        double depth = maxY - minY;

        //- Walls: the base is counterclockwise, the exterior on the right -
        for ( int i = 0; i < n; i++ ) {
            Vector2Dd a = base.get(i);
            Vector2Dd b = base.get((i + 1) % n);
            double ex = b.x - a.x;
            double ey = b.y - a.y;
            double length = Math.sqrt(ex * ex + ey * ey);
            Vector3Dd normal = new Vector3Dd(ey / length, -ex / length, 0);
            double u0 = ((double)i) / ((double)n);
            double u1 = ((double)(i + 1)) / ((double)n);
            builder.addQuad(
                new Vector3Dd(a.x, a.y, 0), normal, u0, 0,
                new Vector3Dd(b.x, b.y, 0), normal, u1, 0,
                new Vector3Dd(b.x, b.y, h), normal, u1, 1,
                new Vector3Dd(a.x, a.y, h), normal, u0, 1);
        }

        //- Caps -----------------------------------------------------------
        Vector3Dd up = new Vector3Dd(0, 0, 1);
        Vector3Dd down = new Vector3Dd(0, 0, -1);
        for ( int i = 0; i < triangles.size(); i += 3 ) {
            Vector2Dd a = base.get(triangles.get(i));
            Vector2Dd b = base.get(triangles.get(i + 1));
            Vector2Dd c = base.get(triangles.get(i + 2));
            double ua = (a.x - minX) / width;
            double va = (a.y - minY) / depth;
            double ub = (b.x - minX) / width;
            double vb = (b.y - minY) / depth;
            double uc = (c.x - minX) / width;
            double vc = (c.y - minY) / depth;
            builder.addTriangle(
                new Vector3Dd(a.x, a.y, h), up, ua, va,
                new Vector3Dd(b.x, b.y, h), up, ub, vb,
                new Vector3Dd(c.x, c.y, h), up, uc, vc);
            builder.addTriangle(
                new Vector3Dd(a.x, a.y, 0), down, ua, va,
                new Vector3Dd(c.x, c.y, 0), down, uc, vc,
                new Vector3Dd(b.x, b.y, 0), down, ub, vb);
        }
        return builder.build();
    }
}
