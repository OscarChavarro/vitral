package vsdk.toolkit.gui.gizmo;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;

public class InfinitePlaneGizmo extends Gizmo {
    public static final double DEFAULT_DISABLE_TIME = 2.0;
    public static final ColorRgb DEFAULT_FRAME_COLOR = new ColorRgb(1, 1, 1);

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

    private void recordDataArrival() {
        previousDataTime = lastDataTime;
        lastDataTime = new Date();
    }

    private boolean inactivityThresholdExceeded() {
        double elapsed = (new Date().getTime() - lastDataTime.getTime()) / 1000.0;
        return disableAfterElapsedSeconds > 0.0 && elapsed > disableAfterElapsedSeconds;
    }
}
