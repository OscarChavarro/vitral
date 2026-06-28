package vsdk.toolkit.gui.gizmo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.environment.geometry.element.Intersection;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleScene;

/**
Gizmo that represents a 3D ray (origin + direction) as a volumetric arrow.

The Arrow geometry is defined in object space along the +Z axis.  The
internal {@link SimpleBody} is positioned and rotated so that the arrow
points from the ray origin toward its direction.

An optional {@link Function} callback can be provided at construction time
to query scene intersections.  When present, RayGizmo calls it for each
ray bounce (primary + reflections up to {@code maxNumOfReflections}) and
stores the results in the snapshot so the renderer can visualize hits and
missed rays differently.

Thread-safety contract:
  - The network (or any non-render) thread calls {@link #setRay} to post a
    new ray.  The call is non-blocking and always stores the <em>latest</em>
    update; intermediate updates are discarded if the render thread has not
    consumed the previous one yet.
  - The render thread calls {@link #acquireSnapshot} once per frame.  If a
    new snapshot is pending it is applied to the {@link SimpleBody} and
    returned; otherwise {@code null} is returned and the body is unchanged.
  - During a single frame the body is never modified: it is only updated at
    the moment {@link #acquireSnapshot} is called, before any drawing begins.
*/
public class RayGizmo extends Gizmo {
    public static final double DEFAULT_DISABLE_TIME = 2.0;

    private static final double ARROW_BASE_LENGTH = 3.0;
    private static final double ARROW_HEAD_LENGTH = 1.0;
    private static final double ARROW_BASE_RADIUS = 0.15;
    private static final double ARROW_HEAD_RADIUS = 0.40;

    public static final ColorRgb DEFAULT_RAY_COLOR    = new ColorRgb(1, 0.8, 0.8);
    public static final ColorRgb DEFAULT_NORMAL_COLOR = new ColorRgb(1, 1, 0.8);

    public record RaySnapshot(double rotationAngleInRadians, List<Ray> rays, List<Intersection> intersections) {
        public RaySnapshot(double rotationAngleInRadians, List<Ray> rays, List<Intersection> intersections) {
            if ( rays.size() != intersections.size() ) {
                throw new IllegalArgumentException(
                    "rays and intersections must be the same size: " +
                    rays.size() + " vs " + intersections.size());
            }
            this.rotationAngleInRadians = rotationAngleInRadians;
            this.rays = List.copyOf(rays);
            this.intersections = Collections.unmodifiableList(new ArrayList<>(intersections));
        }
    }

    private final Arrow arrow;
    private final Sphere dotSphere;
    private final SimpleBody body;
    private final Function<Ray, Intersection> intersectionCallback;
    private final int maxNumOfReflections;
    private final AtomicReference<RaySnapshot> pendingSnapshot = new AtomicReference<>(null);

    private Vector3Dd currentPosition;
    private Vector3Dd currentDirection;
    private double currentRotationAngleInRadians;
    private RaySnapshot currentSnapshot;

    private Date lastDataTime;
    private Date previousDataTime;
    private boolean visible;
    private double disableAfterElapsedSeconds;

    private ColorRgb sourceRayColor;
    private List<ColorRgb> normalRayColors;
    private List<ColorRgb> reflectedRayColors;
    private List<ColorRgb> refractedRayColors;

    /**
     * @param intersectionCallback called per-bounce to query scene hits; may be null
     * @param maxNumOfReflections  number of reflection bounces to trace (0 = primary only)
     */
    public RayGizmo(Function<Ray, Intersection> intersectionCallback, int maxNumOfReflections) {
        arrow = new Arrow(ARROW_BASE_LENGTH, ARROW_HEAD_LENGTH,
                ARROW_BASE_RADIUS, ARROW_HEAD_RADIUS);
        dotSphere = new Sphere(ARROW_BASE_RADIUS);

        body = new SimpleBody();
        body.setGeometry(arrow);

        SimpleMaterial mat = new SimpleMaterial();
        mat = mat.withAmbient(new ColorRgb(0.1, 0.0, 0.0));
        mat = mat.withDiffuse(new ColorRgb(0.9, 0.2, 0.1));
        mat = mat.withSpecular(new ColorRgb(1.0, 1.0, 1.0));
        mat = mat.withPhongExponent(32.0);
        body.setMaterial(mat);

        this.intersectionCallback = intersectionCallback;
        this.maxNumOfReflections = maxNumOfReflections;

        currentPosition = new Vector3Dd(0, 0, 0);
        currentDirection = new Vector3Dd(0, 0, 1);
        currentRotationAngleInRadians = 0.0;
        applyTransform(currentPosition, currentDirection);

        lastDataTime = new Date();
        previousDataTime = new Date();
        visible = true;
        disableAfterElapsedSeconds = DEFAULT_DISABLE_TIME;

        sourceRayColor = DEFAULT_RAY_COLOR;
        normalRayColors = new ArrayList<>(List.of(DEFAULT_NORMAL_COLOR));
        reflectedRayColors = new ArrayList<>(List.of(DEFAULT_RAY_COLOR));
        refractedRayColors = new ArrayList<>(List.of(DEFAULT_RAY_COLOR));
    }

    public Vector3Dd getPosition() {
        return currentPosition;
    }

    public Vector3Dd getDirection() {
        return currentDirection;
    }

    public void setRay(Ray ray, double rotationAngleInRadians) {
        if ( ray == null ) {
            return;
        }

        List<Ray> rays = new ArrayList<>();
        List<Intersection> intersections = new ArrayList<>();

        Intersection primaryIntersection = (intersectionCallback != null) ?
            intersectionCallback.apply(ray) : null;
        rays.add(ray);
        intersections.add(primaryIntersection);

        if ( intersectionCallback != null && primaryIntersection != null ) {
            Ray currentRay = ray;
            Intersection currentIntersection = primaryIntersection;
            for ( int i = 0; i < maxNumOfReflections; i++ ) {
                Ray reflectedRay = computeReflectedRay(currentRay, currentIntersection);
                if ( reflectedRay == null ) {
                    break;
                }
                Intersection reflectedIntersection = intersectionCallback.apply(reflectedRay);
                rays.add(reflectedRay);
                intersections.add(reflectedIntersection);
                if ( reflectedIntersection == null ) {
                    break;
                }
                currentRay = reflectedRay;
                currentIntersection = reflectedIntersection;
            }
        }

        pendingSnapshot.set(new RaySnapshot(rotationAngleInRadians, rays, intersections));
        visible = true;
        recordDataArrival();
    }

    public void update() {
        if ( inactivityThresholdExceeded() ) {
            visible = false;
        }
        previousDataTime = lastDataTime;
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

    public RaySnapshot acquireSnapshot() {
        RaySnapshot snap = pendingSnapshot.getAndSet(null);
        if ( snap == null ) {
            return null;
        }
        if ( !snap.rays().isEmpty() ) {
            Ray primary = snap.rays().get(0);
            applyTransform(primary.getOrigin(), primary.getDirection());
        }
        currentRotationAngleInRadians = snap.rotationAngleInRadians;
        currentSnapshot = snap;
        return snap;
    }

    public RaySnapshot getCurrentSnapshot() {
        return currentSnapshot;
    }

    public double getRotationAngleInRadians() {
        return currentRotationAngleInRadians;
    }

    public SimpleBody getBody() {
        return body;
    }

    public Arrow getArrow() {
        return arrow;
    }

    /**
     * Builds a {@link SimpleScene} from the current snapshot, with all bodies
     * (ray arrows, normal arrows, dot spheres) fully configured — materials,
     * positions, rotations, and scales included.  The scene is ready to be
     * consumed by any renderer without additional logical work.
     *
     * <p>Body types present in the returned scene:
     * <ul>
     *   <li>{@link Arrow} bodies: ray arrows (source + reflections) and surface-normal arrows (thinner).</li>
     *   <li>{@link Sphere} bodies: three dot-spheres beyond a ray arrow that misses geometry.</li>
     * </ul>
     *
     * @return a new scene; empty only if the gizmo has no snapshot yet
     */
    public SimpleScene buildScene() {
        SimpleScene scene = new SimpleScene();
        double arrowTotalLength = ARROW_BASE_LENGTH + ARROW_HEAD_LENGTH;

        if ( currentSnapshot == null || currentSnapshot.rays().isEmpty() ) {
            scene.addBody(buildRayBody(
                new Ray(currentPosition, currentDirection), null,
                sourceRayColor, arrowTotalLength));
            return scene;
        }

        List<ColorRgb> normalColors = normalRayColors;

        // Source ray (index 0)
        Ray sourceRay = currentSnapshot.rays().get(0);
        Intersection sourceIntersection = currentSnapshot.intersections().get(0);
        scene.addBody(buildRayBody(sourceRay, sourceIntersection, sourceRayColor, arrowTotalLength));

        if ( sourceIntersection == null ) {
            addDotBodies(scene, sourceRay, arrowTotalLength);
        } else {
            addNormalBody(scene, sourceIntersection, normalColors.get(0 % normalColors.size()));
        }

        // Reflection bounces (index 1+)
        for ( int i = 1; i < currentSnapshot.rays().size(); i++ ) {
            Ray reflRay = currentSnapshot.rays().get(i);
            Intersection reflIntersection = currentSnapshot.intersections().get(i);
            ColorRgb reflColor = reflectedRayColors.get((i - 1) % reflectedRayColors.size());
            scene.addBody(buildRayBody(reflRay, reflIntersection, reflColor, arrowTotalLength));

            if ( reflIntersection == null ) {
                addDotBodies(scene, reflRay, arrowTotalLength);
            } else {
                addNormalBody(scene, reflIntersection, normalColors.get(i % normalColors.size()));
            }
        }

        return scene;
    }

    private SimpleBody buildRayBody(
        Ray ray, Intersection intersection, ColorRgb color, double arrowTotalLength)
    {
        double scaleZ = 1.0;
        if ( intersection != null ) {
            double hitT = intersection.getT();
            if ( hitT > 1e-6 && arrowTotalLength > 1e-6 ) {
                scaleZ = hitT / arrowTotalLength;
            }
        }
        SimpleBody b = new SimpleBody();
        b.setGeometry(arrow);
        b.setPosition(ray.getOrigin());
        b.setRotation(rotationFromZToDirection(ray.getDirection()));
        b.setScale(new Vector3Dd(1.0, 1.0, scaleZ));
        b.setMaterial(materialFromColor(color));
        return b;
    }

    private void addNormalBody(SimpleScene scene, Intersection intersection, ColorRgb color) {
        Vector3Dd hitPoint = intersection.getPoint();
        Vector3Dd normal = intersection.getNormal();
        if ( hitPoint == null || normal == null ) {
            return;
        }
        SimpleBody b = new SimpleBody();
        b.setGeometry(arrow);
        b.setPosition(hitPoint);
        b.setRotation(rotationFromZToDirection(normal));
        b.setScale(new Vector3Dd(0.5, 0.5, 1.0));
        b.setMaterial(materialFromColor(color));
        scene.addBody(b);
    }

    private void addDotBodies(SimpleScene scene, Ray ray, double arrowTotalLength) {
        double len = ray.getDirection().length();
        if ( len < VSDK.EPSILON ) {
            return;
        }
        Vector3Dd dir = ray.getDirection().multiply(1.0 / len);
        Vector3Dd origin = ray.getOrigin();
        SimpleMaterial dotMat = materialFromColor(new ColorRgb(1.0, 1.0, 0.0));
        double[] factors = { 1.25, 1.50, 1.75 };
        for ( double factor : factors ) {
            Vector3Dd dotPos = origin.add(dir.multiply(arrowTotalLength * factor));
            SimpleBody b = new SimpleBody();
            b.setGeometry(dotSphere);
            b.setPosition(dotPos);
            b.setMaterial(dotMat);
            scene.addBody(b);
        }
    }

    private static SimpleMaterial materialFromColor(ColorRgb color) {
        SimpleMaterial m = new SimpleMaterial();
        m = m.withAmbient(new ColorRgb(color.r() * 0.1, color.g() * 0.1, color.b() * 0.1));
        m = m.withDiffuse(color);
        m = m.withSpecular(new ColorRgb(1.0, 1.0, 1.0));
        m = m.withPhongExponent(32.0);
        return m;
    }

    public ColorRgb getSourceRayColor() {
        return sourceRayColor;
    }

    public void setSourceRayColor(ColorRgb sourceRayColor) {
        this.sourceRayColor = sourceRayColor;
    }

    public List<ColorRgb> getNormalRayColors() {
        return normalRayColors;
    }

    public void setNormalRayColors(List<ColorRgb> normalRayColors) {
        this.normalRayColors = new ArrayList<>(normalRayColors);
    }

    public List<ColorRgb> getReflectedRayColors() {
        return reflectedRayColors;
    }

    public void setReflectedRayColors(List<ColorRgb> reflectedRayColors) {
        this.reflectedRayColors = new ArrayList<>(reflectedRayColors);
    }

    public List<ColorRgb> getRefractedRayColors() {
        return refractedRayColors;
    }

    public void setRefractedRayColors(List<ColorRgb> refractedRayColors) {
        this.refractedRayColors = new ArrayList<>(refractedRayColors);
    }

    private void recordDataArrival() {
        previousDataTime = lastDataTime;
        lastDataTime = new Date();
    }

    private boolean inactivityThresholdExceeded() {
        double elapsed = (new Date().getTime() - lastDataTime.getTime()) / 1000.0;
        return disableAfterElapsedSeconds > 0.0 && elapsed > disableAfterElapsedSeconds;
    }

    private void applyTransform(Vector3Dd position, Vector3Dd direction) {
        currentPosition = position;
        currentDirection = direction;

        Matrix4x4d rotation = rotationFromZToDirection(direction);
        Matrix4x4d rotationInverse = rotation.invert();

        body.setPosition(position);
        body.setRotation(rotation);
        body.setRotationInverse(rotationInverse);
    }

    private static Ray computeReflectedRay(Ray incomingRay, Intersection intersection) {
        if ( intersection == null ) {
            return null;
        }
        Vector3Dd hitPoint = intersection.getPoint();
        Vector3Dd normal = intersection.getNormal();
        if ( hitPoint == null || normal == null ) {
            return null;
        }
        double normalLen = normal.length();
        if ( normalLen < VSDK.EPSILON ) {
            return null;
        }
        Vector3Dd n = normal.multiply(1.0 / normalLen);
        Vector3Dd d = incomingRay.getDirection();
        double dot = d.dotProduct(n);
        Vector3Dd r = d.subtract(n.multiply(2.0 * dot));
        if ( r.length() < VSDK.EPSILON ) {
            return null;
        }
        Vector3Dd origin = hitPoint.add(n.multiply(1e-4));
        return new Ray(origin, r);
    }

    private static Matrix4x4d rotationFromZToDirection(Vector3Dd direction) {
        double len = direction.length();
        if ( len < VSDK.EPSILON ) {
            return new Matrix4x4d();
        }

        Vector3Dd d = direction.multiply(1.0 / len);
        Vector3Dd z = new Vector3Dd(0, 0, 1);
        double dot = z.dotProduct(d);

        if ( dot > 1.0 - VSDK.EPSILON ) {
            return new Matrix4x4d();
        }
        if ( dot < -1.0 + VSDK.EPSILON ) {
            return new Matrix4x4d().axisRotation(Math.PI, 1, 0, 0);
        }

        Vector3Dd axis = z.crossProduct(d).normalized();
        double angle = Math.acos(dot);
        return new Matrix4x4d().axisRotation(angle, axis.x(), axis.y(), axis.z());
    }
}
