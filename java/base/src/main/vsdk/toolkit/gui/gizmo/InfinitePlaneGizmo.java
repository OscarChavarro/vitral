package vsdk.toolkit.gui.gizmo;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;

public class InfinitePlaneGizmo extends Gizmo {
    public static final double DEFAULT_DISABLE_TIME = 2.0;
    public static final ColorRgb DEFAULT_FRAME_COLOR = new ColorRgb(1, 1, 1);

    /// Fraction of the visible area of the viewport the frame of the plane
    /// should cover
    private static final double VIEWPORT_AREA_FRACTION = 0.125;
    /// Lower bound for the cosine of the angle between the normal of the
    /// plane and the front vector of the camera, so the frame does not grow
    /// without bound when the plane is seen edge-on
    private static final double MIN_PROJECTED_COSINE = 0.10;

    public record PlaneSnapshot(InfinitePlane plane, Vector3Dd point, Vector3Dd normal) {
        public PlaneSnapshot {
            if ( plane == null ) {
                throw new IllegalArgumentException("plane cannot be null");
            }
            if ( point == null ) {
                throw new IllegalArgumentException("point cannot be null");
            }
            if ( normal == null || normal.length() < VSDK.EPSILON ) {
                throw new IllegalArgumentException("normal cannot be null or zero");
            }
            plane = new InfinitePlane(plane);
            point = new Vector3Dd(point);
            normal = normal.normalized();
        }
    }

    private final AtomicReference<PlaneSnapshot> pendingSnapshot = new AtomicReference<>(null);

    private InfinitePlane currentPlane;
    private Vector3Dd currentPoint;
    private Vector3Dd currentNormal;

    private Date lastDataTime;
    private Date previousDataTime;
    private boolean visible;
    private double disableAfterElapsedSeconds;
    private ColorRgb frameColor;

    public InfinitePlaneGizmo() {
        currentPoint = new Vector3Dd(0, 0, 0);
        currentNormal = new Vector3Dd(0, 0, 1);
        currentPlane = new InfinitePlane(currentNormal, currentPoint);

        lastDataTime = new Date();
        previousDataTime = new Date();
        visible = true;
        disableAfterElapsedSeconds = DEFAULT_DISABLE_TIME;
        frameColor = DEFAULT_FRAME_COLOR;
    }

    public void setPlane(InfinitePlane plane, Vector3Dd point, Vector3Dd normal) {
        if ( plane == null || point == null || normal == null ||
             normal.length() < VSDK.EPSILON ) {
            return;
        }

        pendingSnapshot.set(new PlaneSnapshot(plane, point, normal));
        visible = true;
        recordDataArrival();
    }

    public void setPlane(Vector3Dd point, Vector3Dd normal) {
        if ( point == null || normal == null || normal.length() < VSDK.EPSILON ) {
            return;
        }
        setPlane(new InfinitePlane(normal, point), point, normal);
    }

    public PlaneSnapshot acquireSnapshot() {
        PlaneSnapshot snap = pendingSnapshot.getAndSet(null);
        if ( snap == null ) {
            return null;
        }

        currentPlane = new InfinitePlane(snap.plane());
        currentPoint = new Vector3Dd(snap.point());
        currentNormal = snap.normal().normalized();
        return snap;
    }

    public void update() {
        if ( inactivityThresholdExceeded() ) {
            visible = false;
        }
        previousDataTime = lastDataTime;
    }

    public InfinitePlane getPlane() {
        return currentPlane;
    }

    public Vector3Dd getPoint() {
        return currentPoint;
    }

    public Vector3Dd getNormal() {
        return currentNormal;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public double getDisableAfterElapsedSeconds() {
        return disableAfterElapsedSeconds;
    }

    public void setDisableAfterElapsedSeconds(double disableAfterElapsedSeconds) {
        this.disableAfterElapsedSeconds = disableAfterElapsedSeconds;
    }

    public Date getLastDataTime() {
        return lastDataTime;
    }

    public Date getPreviousDataTime() {
        return previousDataTime;
    }

    public ColorRgb getFrameColor() {
        return frameColor;
    }

    public void setFrameColor(ColorRgb frameColor) {
        if ( frameColor != null ) {
            this.frameColor = frameColor;
        }
    }

    /**
    Builds the square frame that represents the plane of this gizmo, as seen
    from a camera: a square centered at the point of the plane, contained in
    it, oriented so one of its sides is as parallel as possible to the screen,
    and sized so it covers a fixed fraction of the area of the viewport (the
    more the plane is seen edge-on, the larger it is, up to a limit).

    @param camera camera that views the gizmo; its vectors are updated
    @return the 4 corners of the frame, in the order they are drawn as a
    closed contour, or null if the plane is degenerate
    */
    public Vector3Dd[] buildFrameCorners(Camera camera) {
        if ( camera == null || currentPoint == null || currentNormal == null ||
             currentNormal.length() < VSDK.EPSILON ) {
            return null;
        }

        camera.updateVectors();

        Vector3Dd n = currentNormal.normalized();
        Vector3Dd u = buildTangent(n, camera);
        Vector3Dd v = n.crossProduct(u).normalized();
        double halfSide = calculateHalfSide(camera, currentPoint, n);

        return new Vector3Dd[] {
            currentPoint.add(u.multiply(-halfSide)).add(v.multiply(-halfSide)),
            currentPoint.add(u.multiply( halfSide)).add(v.multiply(-halfSide)),
            currentPoint.add(u.multiply( halfSide)).add(v.multiply( halfSide)),
            currentPoint.add(u.multiply(-halfSide)).add(v.multiply( halfSide))
        };
    }

    /**
    @param normal normal of the plane, normalized
    @param camera camera that views the gizmo
    @return a unit vector contained in the plane, as aligned with the screen
    as the orientation of the plane allows
    */
    private static Vector3Dd buildTangent(Vector3Dd normal, Camera camera) {
        Vector3Dd tangent = normal.crossProduct(camera.getFront());

        if ( tangent.length() < VSDK.EPSILON ) {
            tangent = normal.crossProduct(camera.getUp());
        }
        if ( tangent.length() < VSDK.EPSILON ) {
            tangent = normal.crossProduct(camera.getLeft());
        }
        if ( tangent.length() < VSDK.EPSILON ) {
            tangent = new Vector3Dd(1, 0, 0);
        }
        return tangent.normalized();
    }

    /**
    @param camera camera that views the gizmo, with its vectors updated
    @param center center of the frame
    @param normal normal of the plane, normalized
    @return half of the length of the side of the frame
    */
    private static double calculateHalfSide(Camera camera, Vector3Dd center, Vector3Dd normal) {
        double aspect = Math.max(1.0e-6, camera.getViewportXSize() / camera.getViewportYSize());
        double visibleHeight;
        double visibleWidth;

        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            visibleHeight = 2.0 / Math.max(camera.getOrthogonalZoom(), 1.0e-6);
        }
        else {
            double depth = center.subtract(camera.getPosition()).dotProduct(camera.getFront());

            depth = Math.max(camera.getNearPlaneDistance(), Math.abs(depth));
            visibleHeight = 2.0 * depth * Math.tan(Math.toRadians(camera.getFov()) / 2.0);
        }
        visibleWidth = visibleHeight * aspect;

        double projectedCosine = Math.abs(normal.dotProduct(camera.getFront().normalized()));

        projectedCosine = Math.max(MIN_PROJECTED_COSINE, projectedCosine);

        double side = Math.sqrt(visibleWidth * visibleHeight * VIEWPORT_AREA_FRACTION
            / projectedCosine);

        return side * 0.5;
    }

    private void recordDataArrival() {
        previousDataTime = lastDataTime;
        lastDataTime = new Date();
    }

    private boolean inactivityThresholdExceeded() {
        double elapsed = (new Date().getTime() - lastDataTime.getTime()) / 1000.0;
        return disableAfterElapsedSeconds > 0.0 && elapsed > disableAfterElapsedSeconds;
    }
}
