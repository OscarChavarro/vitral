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
import vsdk.toolkit.environment.camera.Camera;
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
    /** Global visual weight for ray shafts, heads and miss markers. */
    private static final double RAY_THICKNESS_FACTOR = 0.50;
    private static final double ARROW_BASE_RADIUS = 0.15 * RAY_THICKNESS_FACTOR;
    private static final double ARROW_HEAD_RADIUS = 0.40 * RAY_THICKNESS_FACTOR;

    /** The FOV at which the original, unscaled ray thickness is preserved. */
    private static final double REFERENCE_VERTICAL_FOV_DEGREES = 60.0;

    /** Fraction of the viewport height used by a surface-normal arrow. */
    private static final double NORMAL_VIEWPORT_HEIGHT_FRACTION = 0.20;

    public static final ColorRgb DEFAULT_RAY_COLOR    = new ColorRgb(1, 0.8, 0.8);
    public static final ColorRgb DEFAULT_NORMAL_COLOR = new ColorRgb(1, 1, 0.8);

    // Colors of the boxes of the input gizmo: x, y, z, yaw and pitch
    private static final ColorRgb[] INPUT_FIELD_COLORS = {
        new ColorRgb(1, 0, 0),
        new ColorRgb(0, 1, 0),
        new ColorRgb(0, 0, 1),
        new ColorRgb(1, 1, 0),
        new ColorRgb(0, 1, 1)
    };

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
    private final InputGizmo inputGizmo;
    private int maxNumOfReflections;
    private final AtomicReference<RaySnapshot> pendingSnapshot = new AtomicReference<>(null);

    private Vector3Dd currentPosition;
    private Vector3Dd currentDirection;
    private double currentRotationAngleInRadians;
    private RaySnapshot currentSnapshot;

    private Date lastDataTime;
    private Date previousDataTime;
    private boolean visible;
    private boolean rollIndicatorEnabled;
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
        inputGizmo = new InputGizmo(5, 3, 3);
        for ( int i = 0; i < INPUT_FIELD_COLORS.length; i++ ) {
            inputGizmo.setFieldColor(i, INPUT_FIELD_COLORS[i]);
        }

        currentPosition = new Vector3Dd(0, 0, 0);
        currentDirection = new Vector3Dd(0, 0, 1);
        currentRotationAngleInRadians = 0.0;
        applyTransform(currentPosition, currentDirection);
        syncInputGizmo(currentPosition, currentDirection);

        lastDataTime = new Date();
        previousDataTime = new Date();
        visible = true;
        rollIndicatorEnabled = false;
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
        syncInputGizmo(ray.getOrigin(), ray.getDirection());
        visible = true;
        recordDataArrival();
    }

    public int getMaxNumOfReflections() {
        return maxNumOfReflections;
    }

    public void setMaxNumOfReflections(int maxNumOfReflections) {
        this.maxNumOfReflections = Math.max(0, maxNumOfReflections);
    }

    public InputGizmo getInputGizmo() {
        return inputGizmo;
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

    /**
     * Whether the visual marker of rotation around the ray's own axis is
     * rendered. It is disabled by default because most ray input methods do
     * not provide a meaningful roll angle.
     */
    public boolean isRollIndicatorEnabled() {
        return rollIndicatorEnabled;
    }

    public void setRollIndicatorEnabled(boolean rollIndicatorEnabled) {
        this.rollIndicatorEnabled = rollIndicatorEnabled;
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
            syncInputGizmo(primary.getOrigin(), primary.getDirection());
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
        return buildScene(null);
    }

    /**
     * Builds the visualization scene with a thickness normalized to the
     * active view.  Only dimensions perpendicular to each ray are scaled:
     * its length continues to represent the intersection distance exactly.
     *
     * <p>For a perspective camera the scale follows the vertical view angle;
     * for an orthographic camera it follows its visible world height.  Thus a
     * narrow (zoomed-in) view produces thinner rays and a wide (zoomed-out)
     * view produces thicker ones, preserving their apparent thickness.
     *
     * @param camera active camera, or {@code null} to retain legacy size
     * @return a renderer-independent scene describing the ray visualization
     */
    public SimpleScene buildScene(Camera camera) {
        SimpleScene scene = new SimpleScene();
        double arrowTotalLength = ARROW_BASE_LENGTH + ARROW_HEAD_LENGTH;
        double thicknessScale = calculateViewThicknessScale(camera);

        if ( currentSnapshot == null || currentSnapshot.rays().isEmpty() ) {
            double missingRayLength = calculateMissingRayLength(camera, currentPosition);
            scene.addBody(buildRayBody(
                new Ray(currentPosition, currentDirection), null,
                sourceRayColor, arrowTotalLength, missingRayLength, thicknessScale));
            return scene;
        }

        List<ColorRgb> normalColors = normalRayColors;

        // Source ray (index 0)
        Ray sourceRay = currentSnapshot.rays().get(0);
        Intersection sourceIntersection = currentSnapshot.intersections().get(0);
        double sourceRayLength = sourceIntersection == null ?
            calculateMissingRayLength(camera, sourceRay.getOrigin()) : arrowTotalLength;
        scene.addBody(buildRayBody(sourceRay, sourceIntersection, sourceRayColor,
            arrowTotalLength, sourceRayLength, thicknessScale));

        if ( sourceIntersection == null ) {
            addDotBodies(scene, sourceRay, camera, sourceRayLength,
                calculateVisibleNormalLength(camera, sourceRay.getOrigin()), sourceRayColor);
        } else {
            addNormalBody(scene, sourceIntersection, normalColors.get(0 % normalColors.size()),
                camera, thicknessScale);
        }

        // Reflection bounces (index 1+)
        for ( int i = 1; i < currentSnapshot.rays().size(); i++ ) {
            Ray reflRay = currentSnapshot.rays().get(i);
            Intersection reflIntersection = currentSnapshot.intersections().get(i);
            ColorRgb reflColor = reflectedRayColors.get((i - 1) % reflectedRayColors.size());
            double reflectedRayLength = reflIntersection == null ?
                calculateMissingRayLength(camera, reflRay.getOrigin()) : arrowTotalLength;
            scene.addBody(buildRayBody(reflRay, reflIntersection, reflColor,
                arrowTotalLength, reflectedRayLength, thicknessScale));

            if ( reflIntersection == null ) {
                addDotBodies(scene, reflRay, camera, reflectedRayLength,
                    calculateVisibleNormalLength(camera, reflRay.getOrigin()), reflColor);
            } else {
                addNormalBody(scene, reflIntersection, normalColors.get(i % normalColors.size()),
                    camera, thicknessScale);
            }
        }

        return scene;
    }

    private SimpleBody buildRayBody(
        Ray ray, Intersection intersection, ColorRgb color, double arrowTotalLength,
        double displayedLength, double thicknessScale)
    {
        double rayLength = displayedLength;
        if ( intersection != null ) {
            double hitT = intersection.getT();
            if ( hitT > 1e-6 && arrowTotalLength > 1e-6 ) {
                rayLength = hitT;
            }
        }
        SimpleBody b = new SimpleBody();
        b.setGeometry(buildRayArrow(rayLength));
        b.setPosition(ray.getOrigin());
        b.setRotation(rotationFromZToDirection(ray.getDirection()));
        b.setScale(new Vector3Dd(thicknessScale, thicknessScale, 1.0));
        b.setMaterial(materialFromColor(color));
        return b;
    }

    /**
     * Creates a ray arrow with a fixed-size head.  Only the shaft changes to
     * reach the requested length, so all non-normal ray tips retain both the
     * same length and the same shaft/head radius ratio.
     */
    private static Arrow buildRayArrow(double requestedLength) {
        double totalLength = Math.max(requestedLength, ARROW_HEAD_LENGTH);
        double shaftLength = Math.max(0.0, totalLength - ARROW_HEAD_LENGTH);
        return new Arrow(shaftLength, ARROW_HEAD_LENGTH,
            ARROW_BASE_RADIUS, ARROW_HEAD_RADIUS);
    }

    private void addNormalBody(SimpleScene scene, Intersection intersection, ColorRgb color,
        Camera camera, double thicknessScale) {
        Vector3Dd hitPoint = intersection.getPoint();
        Vector3Dd normal = intersection.getNormal();
        if ( hitPoint == null || normal == null ) {
            return;
        }
        SimpleBody b = new SimpleBody();
        b.setGeometry(arrow);
        b.setPosition(hitPoint);
        b.setRotation(rotationFromZToDirection(normal));
        double normalLength = calculateVisibleNormalLength(camera, hitPoint);
        double arrowTotalLength = ARROW_BASE_LENGTH + ARROW_HEAD_LENGTH;
        b.setScale(new Vector3Dd(0.5 * thicknessScale, 0.5 * thicknessScale,
            normalLength / arrowTotalLength));
        b.setMaterial(materialFromColor(color));
        scene.addBody(b);
    }

    private void addDotBodies(SimpleScene scene, Ray ray, Camera camera,
        double rayLength, double normalLength, ColorRgb rayColor) {
        double len = ray.getDirection().length();
        if ( len < VSDK.EPSILON ) {
            return;
        }
        Vector3Dd dir = ray.getDirection().multiply(1.0 / len);
        Vector3Dd origin = ray.getOrigin();
        SimpleMaterial dotMat = materialFromColor(rayColor);
        double sphereRadius = (camera == null ? ARROW_BASE_RADIUS : normalLength * 0.05) / 3.0;
        double sphereScale = sphereRadius / ARROW_BASE_RADIUS;
        double firstDotOffset = camera == null ? rayLength * 0.25 : 2.5 * sphereRadius;
        double dotSpacing = camera == null ? rayLength * 0.25 : 3.0 * sphereRadius;
        for ( int i = 0; i < 3; i++ ) {
            double dotDistance = rayLength + firstDotOffset + i * dotSpacing;
            Vector3Dd dotPos = origin.add(dir.multiply(dotDistance));
            SimpleBody b = new SimpleBody();
            b.setGeometry(dotSphere);
            b.setPosition(dotPos);
            b.setScale(new Vector3Dd(sphereScale, sphereScale, sphereScale));
            b.setMaterial(dotMat);
            scene.addBody(b);
        }
    }

    /**
     * Returns the scale to apply to dimensions perpendicular to a ray.
     * The reference perspective FOV is 60 degrees, so existing callers that
     * use the default camera retain their former world-space thickness.
     */
    public static double calculateViewThicknessScale(Camera camera) {
        if ( camera == null ) {
            return 1.0;
        }
        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            return 1.0 / Math.max(camera.getOrthogonalZoom(), VSDK.EPSILON);
        }

        double halfFovRadians = Math.toRadians(camera.getFov()) / 2.0;
        double referenceHalfFovRadians = Math.toRadians(REFERENCE_VERTICAL_FOV_DEGREES) / 2.0;
        return Math.max(Math.tan(halfFovRadians), VSDK.EPSILON)
            / Math.tan(referenceHalfFovRadians);
    }

    /**
     * Calculates a normal-arrow length that occupies a stable, modest part
     * of the viewport height at its hit point.  This keeps normals visible
     * without allowing the former fixed world-space length to overwhelm a
     * zoomed-in view.
     *
     * @param camera active camera, or {@code null} to retain the legacy length
     * @param position surface point where the normal starts
     * @return normal-arrow length in world units
     */
    public static double calculateVisibleNormalLength(Camera camera, Vector3Dd position) {
        double legacyLength = ARROW_BASE_LENGTH + ARROW_HEAD_LENGTH;
        if ( camera == null || position == null ) {
            return legacyLength;
        }

        double viewportHeight = Math.max(camera.getViewportYSize(), 1.0);
        double targetPixels = NORMAL_VIEWPORT_HEIGHT_FRACTION * viewportHeight;
        double worldPerPixel;

        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
            double worldViewHeight = 2.0 / Math.max(
                Math.abs(camera.getOrthogonalZoom()), VSDK.EPSILON);
            worldPerPixel = worldViewHeight / viewportHeight;
        } else {
            Vector3Dd toPoint = position.subtract(camera.getPosition());
            double depth = Math.max(Math.abs(toPoint.dotProduct(camera.getFront())),
                camera.getNearPlaneDistance());
            double halfFovRadians = Math.toRadians(camera.getFov()) / 2.0;
            double worldViewHeight = 2.0 * depth * Math.max(
                Math.tan(halfFovRadians), VSDK.EPSILON);
            worldPerPixel = worldViewHeight / viewportHeight;
        }
        return targetPixels * worldPerPixel;
    }

    /**
     * Length of a ray that has no further intersection.  Its endpoint is
     * deliberately tied to the visible normal at the ray origin so an open
     * ray remains readable at every zoom level.
     */
    public static double calculateMissingRayLength(Camera camera, Vector3Dd origin) {
        return 2.0 * calculateVisibleNormalLength(camera, origin);
    }

    /**
     * Model matrix for the roll indicator, with the same view-normalized
     * radial scale used by {@link #buildScene(Camera)}.
     */
    public Matrix4x4d getRollIndicatorModelMatrix(Camera camera) {
        double thicknessScale = calculateViewThicknessScale(camera);
        Matrix4x4d thickness = new Matrix4x4d().scale(
            thicknessScale, thicknessScale, 1.0);
        return body.getTransformationMatrix().multiply(thickness);
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

    /// Outer radius of the roll indicator fin (see `buildRollIndicatorMesh`)
    private static final float ROLL_INDICATOR_OUTER_RADIUS = 0.65f;
    /// Inner radius of the roll indicator fin
    private static final float ROLL_INDICATOR_INNER_RADIUS = 0.17f;
    /// Half width of the roll indicator fin, at its base
    private static final float ROLL_INDICATOR_HALF_WIDTH = 0.12f;
    /// Distance of the tip of the roll indicator fin, along +Z
    private static final float ROLL_INDICATOR_TIP_Z = 0.30f;

    /**
    Geometry of a single triangle, used to mark the current roll angle around
    the ray direction: a fin pointing in local +X at the base of the arrow
    (z=0), with its tip lifted towards +Z. It is generated once around the
    local +Z axis; the renderer rotates it by the roll angle of the gizmo
    (see `getRotationAngleInRadians`) and transforms it with the body of the
    arrow.

    @param positions 3 vertexes (tip, base-left, base-right), 3 floats each
    @param normals one repeated flat normal, 3 floats each
    @param uvs 3 texture coordinates, 2 floats each
    */
    public record RollIndicatorMesh(float[] positions, float[] normals, float[] uvs) {
    }

    /**
    @return the geometry of the roll indicator fin, in the local space of the
    arrow (see {@link RollIndicatorMesh})
    */
    public static RollIndicatorMesh buildRollIndicatorMesh() {
        // P0=tip, P1=base-left, P2=base-right.
        float[] positions = {
            ROLL_INDICATOR_OUTER_RADIUS,  0.0f,                 ROLL_INDICATOR_TIP_Z,
            ROLL_INDICATOR_INNER_RADIUS, -ROLL_INDICATOR_HALF_WIDTH,  0.0f,
            ROLL_INDICATOR_INNER_RADIUS,  ROLL_INDICATOR_HALF_WIDTH,  0.0f
        };
        float[] uvs = { 0.5f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f };
        float ax = ROLL_INDICATOR_INNER_RADIUS - ROLL_INDICATOR_OUTER_RADIUS;
        float ay = -ROLL_INDICATOR_HALF_WIDTH;
        float az = -ROLL_INDICATOR_TIP_Z;
        float bx = ROLL_INDICATOR_INNER_RADIUS - ROLL_INDICATOR_OUTER_RADIUS;
        float by = ROLL_INDICATOR_HALF_WIDTH;
        float bz = -ROLL_INDICATOR_TIP_Z;
        float nx = ay*bz - az*by;
        float ny = az*bx - ax*bz;
        float nz = ax*by - ay*bx;
        float normalLength = (float)Math.sqrt(nx*nx + ny*ny + nz*nz);

        nx /= normalLength;
        ny /= normalLength;
        nz /= normalLength;

        float[] normals = { nx, ny, nz, nx, ny, nz, nx, ny, nz };

        return new RollIndicatorMesh(positions, normals, uvs);
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

    private void syncInputGizmo(Vector3Dd position, Vector3Dd direction) {
        Vector3Dd d = direction.length() > VSDK.EPSILON ?
            direction.normalized() : new Vector3Dd(0, 0, 1);
        double yaw = Math.atan2(d.y(), d.x());
        double pitch = Math.asin(Math.max(-1.0, Math.min(1.0, d.z())));

        inputGizmo.setValue(0, position.x());
        inputGizmo.setValue(1, position.y());
        inputGizmo.setValue(2, position.z());
        inputGizmo.setValue(3, Math.toDegrees(yaw));
        inputGizmo.setValue(4, Math.toDegrees(pitch));
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
