import com.jogamp.opengl.GL2;

import models.DebuggerModel;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.render.jogl.Jogl2MaterialRenderer;
import vsdk.toolkit.render.jogl.Jogl2PolyhedralBoundedSolidRenderer;

public class JoglHudOperandsRenderer
{
    private static final double HUD_INSET_DEPTH = 2.8;

    private final DebuggerModel model;
    private final Material csgOperandMaterialA;
    private final Material csgOperandMaterialB;

    public JoglHudOperandsRenderer(DebuggerModel model)
    {
        this.model = model;
        this.csgOperandMaterialA = createInsetMaterial(1.0, 0.502, 0.502);
        this.csgOperandMaterialB = createInsetMaterial(0.502, 1.0, 0.502);
    }

    private static Material createInsetMaterial(double r, double g, double b)
    {
        Material m = new Material();
        m.setAmbient(new ColorRgb(0.2 * r, 0.2 * g, 0.2 * b));
        m.setDiffuse(new ColorRgb(r, g, b));
        m.setSpecular(new ColorRgb(1.0, 1.0, 1.0));
        m.setDoubleSided(false);
        m.setPhongExponent(100);
        return m;
    }

    private static Vector3D solidCenter(PolyhedralBoundedSolid solid)
    {
        double[] minMax;

        if ( solid == null ) {
            return new Vector3D(0, 0, 0);
        }
        minMax = solid.getMinMax();
        if ( minMax == null || minMax.length < 6 ) {
            return new Vector3D(0, 0, 0);
        }
        return new Vector3D(
            (minMax[0] + minMax[3]) / 2.0,
            (minMax[1] + minMax[4]) / 2.0,
            (minMax[2] + minMax[5]) / 2.0);
    }

    private static double solidMaxExtent(PolyhedralBoundedSolid solid)
    {
        double[] minMax;
        double ex;
        double ey;
        double ez;

        if ( solid == null ) {
            return 1.0;
        }
        minMax = solid.getMinMax();
        if ( minMax == null || minMax.length < 6 ) {
            return 1.0;
        }
        ex = Math.abs(minMax[0] - minMax[3]);
        ey = Math.abs(minMax[1] - minMax[4]);
        ez = Math.abs(minMax[2] - minMax[5]);
        return Math.max(ex, Math.max(ey, ez));
    }

    private static Vector3D cameraRelativeAnchor(Camera camera,
        double ndcX,
        double ndcY,
        double depth)
    {
        Vector3D eye = camera.getPosition();
        Vector3D front = camera.getFront().normalized();
        Vector3D up = camera.getUp().normalized();
        Vector3D right = camera.getLeft().multiply(-1).normalized();
        double viewportY = Math.max(camera.getViewportYSize(), 1e-9);
        double aspect = camera.getViewportXSize() / viewportY;
        double offsetX;
        double offsetY;
        double safeDepth = Math.max(depth, 1e-9);

        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            double zoom = Math.max(camera.getOrthogonalZoom(), 1e-9);
            offsetX = ndcX * (aspect / zoom);
            offsetY = ndcY * (1.0 / zoom);
        }
        else {
            double halfHeight = safeDepth *
                Math.tan(Math.toRadians(camera.getFov() / 2.0));
            double halfWidth = halfHeight * aspect;
            offsetX = ndcX * halfWidth;
            offsetY = ndcY * halfHeight;
        }

        return eye.add(front.multiply(safeDepth))
                  .add(right.multiply(offsetX))
                  .add(up.multiply(offsetY));
    }

    private void drawInsetSolid(GL2 gl,
        PolyhedralBoundedSolid solid,
        Material material,
        Vector3D anchorPoint,
        double mainSolidExtent)
    {
        Vector3D center;
        double extent;
        double scale;

        if ( solid == null ) {
            return;
        }
        center = solidCenter(solid);
        extent = solidMaxExtent(solid);
        if ( extent < 1e-12 ) {
            extent = 1.0;
        }
        if ( mainSolidExtent < 1e-12 ) {
            mainSolidExtent = 1.0;
        }
        scale = 0.75 * (mainSolidExtent / extent);

        gl.glPushMatrix();
        gl.glTranslated(anchorPoint.x(), anchorPoint.y(), anchorPoint.z());
        gl.glScaled(scale, scale, scale);
        gl.glTranslated(-center.x(), -center.y(), -center.z());
        Jogl2MaterialRenderer.activate(gl, material);
        Jogl2PolyhedralBoundedSolidRenderer.draw(gl, solid, model.getCamera(),
            model.getQuality());
        gl.glPopMatrix();
    }

    public void draw(GL2 gl, int viewportWidth, int viewportHeight)
    {
        PolyhedralBoundedSolid operandA = model.getCsgPreviewOperandA();
        PolyhedralBoundedSolid operandB = model.getCsgPreviewOperandB();
        PolyhedralBoundedSolid mainSolid = model.getSolid();
        Camera camera = model.getCamera();
        double mainExtent;
        Vector3D leftAnchor;
        Vector3D rightAnchor;

        if ( operandA == null || operandB == null || mainSolid == null ) {
            return;
        }
        if ( viewportWidth <= 0 || viewportHeight <= 0 ) {
            return;
        }
        mainExtent = solidMaxExtent(mainSolid);

        leftAnchor = cameraRelativeAnchor(camera, -0.76, -0.5,
            HUD_INSET_DEPTH);
        rightAnchor = cameraRelativeAnchor(camera, 0.76, -0.5,
            HUD_INSET_DEPTH);

        drawInsetSolid(gl, operandA, csgOperandMaterialA, leftAnchor, mainExtent);
        drawInsetSolid(gl, operandB, csgOperandMaterialB, rightAnchor, mainExtent);
        Jogl2MaterialRenderer.activate(gl, model.getMaterial());
    }
}
