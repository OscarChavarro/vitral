package vsdk.toolkit.render.jogl;

import com.jogamp.opengl.GL2;

import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

public class Jogl4PolyhedralBoundedSolidRenderer extends Jogl4Renderer
{
    public static void draw(
        GL2 gl,
        PolyhedralBoundedSolid solid,
        Camera camera,
        RendererConfiguration quality)
    {
        Jogl2PolyhedralBoundedSolidRenderer.draw(gl, solid, camera, quality);
    }

    public static void drawDebugFaceBoundary(
        GL2 gl,
        PolyhedralBoundedSolid solid,
        int faceIndex)
    {
        Jogl2PolyhedralBoundedSolidRenderer.drawDebugFaceBoundary(gl, solid,
            faceIndex);
    }

    public static void drawDebugFace(
        GL2 gl,
        PolyhedralBoundedSolid solid,
        int faceIndex)
    {
        Jogl2PolyhedralBoundedSolidRenderer.drawDebugFace(gl, solid,
            faceIndex);
    }

    public static void drawDebugEdges(
        GL2 gl,
        PolyhedralBoundedSolid solid,
        Camera camera,
        int edgeIndex)
    {
        Jogl2PolyhedralBoundedSolidRenderer.drawDebugEdges(gl, solid, camera,
            edgeIndex);
    }
}
