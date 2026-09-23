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
    private final double nearPlaneDistance;
    private final double farPlaneDistance;

    /**
    Snapshot with the default clipping planes of `Camera` (0.05 and 100).
    @param eyePosition eye position
    @param front unit vector of the viewing direction
    @param left unit vector to the left of the view
    @param up unit vector upwards in the view
    @param projectionMode `Camera.PROJECTION_MODE_*`
    @param orthogonalZoom zoom of the orthogonal projection
    @param viewportXSize width of the viewport in pixels
    @param viewportYSize height of the viewport in pixels
    @param dir precalculated projector direction to the viewport center
    @param upWithScale precalculated up vector scaled by the field of view
    @param rightWithScale precalculated right vector scaled by the field of view
    */
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
        this(eyePosition, front, left, up, projectionMode, orthogonalZoom,
             viewportXSize, viewportYSize, dir, upWithScale, rightWithScale,
             0.05, 100.0);
    }

    /**
    @param eyePosition eye position
    @param front unit vector of the viewing direction
    @param left unit vector to the left of the view
    @param up unit vector upwards in the view
    @param projectionMode `Camera.PROJECTION_MODE_*`
    @param orthogonalZoom zoom of the orthogonal projection
    @param viewportXSize width of the viewport in pixels
    @param viewportYSize height of the viewport in pixels
    @param dir precalculated projector direction to the viewport center
    @param upWithScale precalculated up vector scaled by the field of view
    @param rightWithScale precalculated right vector scaled by the field of view
    @param nearPlaneDistance distance from the eye to the near clipping plane
    @param farPlaneDistance distance from the eye to the far clipping plane
    */
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
        Vector3Dd rightWithScale,
        double nearPlaneDistance,
        double farPlaneDistance)
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
        this.nearPlaneDistance = nearPlaneDistance;
        this.farPlaneDistance = farPlaneDistance;
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

    /**
    @return distance from the eye to the near clipping plane, as used by the
    projection matrix of the camera (see `Camera.calculateViewVolumeMatrix`)
    */
    public double getNearPlaneDistance()
    {
        return nearPlaneDistance;
    }

    /**
    @return distance from the eye to the far clipping plane, as used by the
    projection matrix of the camera (see `Camera.calculateViewVolumeMatrix`)
    */
    public double getFarPlaneDistance()
    {
        return farPlaneDistance;
    }
}
