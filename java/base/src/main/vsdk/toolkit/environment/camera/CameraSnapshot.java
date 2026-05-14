package vsdk.toolkit.environment.camera;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/**
Immutable camera state used by the raytracer to guarantee per-frame
consistency even if the live Camera is edited concurrently.
*/
public final class CameraSnapshot
{
    private final Vector3Dd eyePosition;
    private final Vector3Dd front;
    private final Vector3Dd left;
    private final Vector3Dd up;
    private final int projectionMode;
    private final double orthogonalZoom;
    private final double viewportXSize;
    private final double viewportYSize;
    private final Vector3Dd dir;
    private final Vector3Dd upWithScale;
    private final Vector3Dd rightWithScale;

    public CameraSnapshot(
        Vector3Dd eyePosition,
        Vector3Dd front,
        Vector3Dd left,
        Vector3Dd up,
        int projectionMode,
        double orthogonalZoom,
        double viewportXSize,
        double viewportYSize,
        Vector3Dd dir,
        Vector3Dd upWithScale,
        Vector3Dd rightWithScale)
    {
        this.eyePosition = new Vector3Dd(eyePosition);
        this.front = new Vector3Dd(front);
        this.left = new Vector3Dd(left);
        this.up = new Vector3Dd(up);
        this.projectionMode = projectionMode;
        this.orthogonalZoom = orthogonalZoom;
        this.viewportXSize = viewportXSize;
        this.viewportYSize = viewportYSize;
        this.dir = new Vector3Dd(dir);
        this.upWithScale = new Vector3Dd(upWithScale);
        this.rightWithScale = new Vector3Dd(rightWithScale);
    }

    public Vector3Dd getEyePosition()
    {
        return eyePosition;
    }

    public Vector3Dd getFront()
    {
        return front;
    }

    public Vector3Dd getLeft()
    {
        return left;
    }

    public Vector3Dd getUp()
    {
        return up;
    }

    public int getProjectionMode()
    {
        return projectionMode;
    }

    public double getOrthogonalZoom()
    {
        return orthogonalZoom;
    }

    public double getViewportXSize()
    {
        return viewportXSize;
    }

    public double getViewportYSize()
    {
        return viewportYSize;
    }

    public Vector3Dd getDir()
    {
        return dir;
    }

    public Vector3Dd getUpWithScale()
    {
        return upWithScale;
    }

    public Vector3Dd getRightWithScale()
    {
        return rightWithScale;
    }
}
