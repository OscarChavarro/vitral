package vsdk.toolkit.environment;

import vsdk.toolkit.common.Ray;
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

    private CameraSnapshot(Camera camera)
    {
        eyePosition = new Vector3D(camera.getPosition());
        front = new Vector3D(camera.getFront()).normalized();
        left = new Vector3D(camera.getLeft()).normalized();
        up = new Vector3D(camera.getUp()).normalized();
        projectionMode = camera.getProjectionMode();
        orthogonalZoom = camera.getOrthogonalZoom();
        viewportXSize = camera.getViewportXSize();
        viewportYSize = camera.getViewportYSize();

        double safeViewportX = viewportXSize > 0 ? viewportXSize : 1.0;
        double safeViewportY = viewportYSize > 0 ? viewportYSize : 1.0;
        double fovFactor = safeViewportX / safeViewportY;
        double tanHalfFov = Math.tan(Math.toRadians(camera.getFov() / 2.0));

        dir = front.multiply(0.5);
        upWithScale = up.multiply(tanHalfFov);
        rightWithScale = left.multiply(-fovFactor * tanHalfFov);
    }

    public static CameraSnapshot fromCamera(Camera camera)
    {
        return new CameraSnapshot(camera);
    }

    public Ray generateRay(int x, int y)
    {
        double safeViewportX = viewportXSize > 0 ? viewportXSize : 1.0;
        double safeViewportY = viewportYSize > 0 ? viewportYSize : 1.0;

        double u = ((double)x - safeViewportX/2.0) / safeViewportX;
        double v =
            ((safeViewportY - (double)y - 1) - safeViewportY/2.0) / safeViewportY;

        if ( projectionMode == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            double fovFactor = safeViewportX / safeViewportY;
            double duScale = (-fovFactor) * (2*u/orthogonalZoom);
            double dvScale = 2*v/orthogonalZoom;
            Vector3D origin = new Vector3D(
                eyePosition.x() + left.x()*duScale + up.x()*dvScale,
                eyePosition.y() + left.y()*duScale + up.y()*dvScale,
                eyePosition.z() + left.z()*duScale + up.z()*dvScale);
            return new Ray(origin, front);
        }

        Vector3D direction = new Vector3D(
            rightWithScale.x()*u + upWithScale.x()*v + dir.x(),
            rightWithScale.y()*u + upWithScale.y()*v + dir.y(),
            rightWithScale.z()*u + upWithScale.z()*v + dir.z());

        return new Ray(eyePosition, direction);
    }
}
