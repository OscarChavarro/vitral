package vsdk.toolkit.environment;

import vsdk.toolkit.common.linealAlgebra.Vector3D;

/**
Immutable camera state used by the raytracer to guarantee per-frame
consistency even if the live Camera is edited concurrently.
*/
public final class CameraSnapshot
{
    private final Vector3D eyePosition;
    private final Vector3D front;
    private final Vector3D left;
    private final Vector3D up;
    private final int projectionMode;
    private final double orthogonalZoom;
    private final double viewportXSize;
    private final double viewportYSize;
    private final Vector3D dir;
    private final Vector3D upWithScale;
    private final Vector3D rightWithScale;

    public CameraSnapshot(
        Vector3D eyePosition,
        Vector3D front,
        Vector3D left,
        Vector3D up,
        int projectionMode,
        double orthogonalZoom,
        double viewportXSize,
        double viewportYSize,
        Vector3D dir,
        Vector3D upWithScale,
        Vector3D rightWithScale)
    {
        this.eyePosition = new Vector3D(eyePosition);
        this.front = new Vector3D(front);
        this.left = new Vector3D(left);
        this.up = new Vector3D(up);
        this.projectionMode = projectionMode;
        this.orthogonalZoom = orthogonalZoom;
        this.viewportXSize = viewportXSize;
        this.viewportYSize = viewportYSize;
        this.dir = new Vector3D(dir);
        this.upWithScale = new Vector3D(upWithScale);
        this.rightWithScale = new Vector3D(rightWithScale);
    }

    public Vector3D getEyePosition()
    {
        return eyePosition;
    }

    public Vector3D getFront()
    {
        return front;
    }

    public Vector3D getLeft()
    {
        return left;
    }

    public Vector3D getUp()
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

    public Vector3D getDir()
    {
        return dir;
    }

    public Vector3D getUpWithScale()
    {
        return upWithScale;
    }

    public Vector3D getRightWithScale()
    {
        return rightWithScale;
    }
}
