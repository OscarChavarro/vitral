package vsdk.toolkit.environment.geometry.volume;

import java.util.ArrayList;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.numerics.FloatingInterval;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayBoundary;
import vsdk.toolkit.environment.geometry.element.RayCrossing;
import vsdk.toolkit.environment.geometry.element.RayCrossingKind;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RayCrossingSink;
import vsdk.toolkit.environment.geometry.element.RayCrossingSupport;
import vsdk.toolkit.environment.geometry.element.RayFirstHitStatus;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.element.RaySolidBoundary;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;

/**
Solid F(p) < 0 of a continuous scalar field F whose ray restriction a
subclass can solve. This class turns the roots of F along a ray into the
typed queries: first hit, crossings, ray classification, certified point and
box containment, and surface detail with the outward normal grad F / |grad F|.
Solver roots are refined by Newton steps on F before use.
Ray membership between consecutive roots is decided by the sign of F at a
point inside the segment, so tangent double roots do not split segments and a zero or
non-finite sample is reported as Indeterminate. Queries need a finite minT.
*/
public abstract class ImplicitSolid extends Solid {
    // Largest relative Newton correction applied to a solver root.
    public static final double ROOT_POLISH_REACH = 1.0e-3;

    // Roots closer than this relative distance are one root.
    public static final double ROOT_MERGE_TOLERANCE = 1.0e-12;

    private double polishRoot(Vector3Dd start, Vector3Dd unit, double distance) {
    // Newton steps on F along the ray, kept only while they shrink |F| and stay
    // close, so solver roots reach the accuracy of the field evaluation.
    if (!Double.isFinite(distance)) return distance;
    double value = evaluateField(start.add(unit.multiply(distance)));
    for (int iteration = 0; iteration < 8 && Double.isFinite(value) && value != 0; ++iteration) {
        final double slope = evaluateFieldGradient(start.add(unit.multiply(distance))).dotProduct(unit);
        if (!Double.isFinite(slope) || slope == 0) break;
        final double next = distance - value/slope;
        if (!Double.isFinite(next) ||
            Math.abs(next - distance) > ROOT_POLISH_REACH*(1.0 + Math.abs(distance))) break;
        final double nextValue = evaluateField(start.add(unit.multiply(next)));
        if (!(Math.abs(nextValue) < Math.abs(value))) break;
        distance = next;
        value = nextValue;
    }
    return distance;
}

    private RayFirstHitStatus collectRoots(Ray ray, RayCrossingQuery query, ArrayList<Double> distances, Vector3Dd[] start, Vector3Dd[] unit, double[] length, double[] maximumDistance) {
    distances.clear();
    final Vector3Dd o = ray.getOrigin();
    final Vector3Dd d = ray.getDirection();
    if (!Double.isFinite(o.x()) || !Double.isFinite(o.y()) || !Double.isFinite(o.z()) ||
        !Double.isFinite(d.x()) || !Double.isFinite(d.y()) || !Double.isFinite(d.z()) ||
        Double.isNaN(query.minT) || Double.isNaN(query.maxT) || query.minT > query.maxT)
        return RayFirstHitStatus.InvalidInput;
    length[0] = d.length();
    if (!(length[0] > 0) || !Double.isFinite(length[0])) return RayFirstHitStatus.InvalidInput;
    if (query.minimumChordSquared != 0 || query.minimumExteriorApproach != Double.NEGATIVE_INFINITY ||
        !Double.isFinite(query.minT)) return RayFirstHitStatus.Unsupported;
    unit[0] = d.multiply(1.0/length[0]);
    start[0] = o.add(d.multiply(query.minT));
    maximumDistance[0] = query.maxT == Double.POSITIVE_INFINITY ?
        Double.POSITIVE_INFINITY : (query.maxT - query.minT)*length[0];
    if (!Double.isFinite(start[0].x()) || !Double.isFinite(start[0].y()) ||
        !Double.isFinite(start[0].z()) || Double.isNaN(maximumDistance[0]))
        return RayFirstHitStatus.Indeterminate;
    ArrayList<Double> found = new ArrayList<Double>();
    if (!findRayRoots(start[0], unit[0], maximumDistance[0], found)) return RayFirstHitStatus.Indeterminate;
    for (long i = 0; i < found.size(); ++i) {
        final double value = polishRoot(start[0], unit[0], found.get((int)(i)));
        if (!Double.isFinite(value)) return RayFirstHitStatus.Indeterminate;
        if (value >= 0 && value <= maximumDistance[0]) distances.add(value);
    }
    distances.sort(null);
    // The same root reported by two solver intervals collapses to one.
    long kept = 0;
    for (long i = 0; i < distances.size(); ++i)
        if (kept == 0 || distances.get((int)(i)) - distances.get((int)(kept-1)) > ROOT_MERGE_TOLERANCE*(1.0 + distances.get((int)(i))))
            distances.set((int)(kept++), distances.get((int)(i)));
    while (distances.size() > kept) distances.remove(distances.size()-1);
    return distances.isEmpty() ? RayFirstHitStatus.Miss : RayFirstHitStatus.Hit;
}

    private int sampleSign(Vector3Dd start, Vector3Dd unit, double distance) {
    final double value = evaluateField(start.add(unit.multiply(distance)));
    if (!Double.isFinite(value)) return 0;
    return value < 0 ? -1 : value > 0 ? 1 : 0;
}

    private int enclosureSign(Vector3Dd point) {
    final FloatingInterval value = encloseField(FloatingInterval.exact(point.x()),
        FloatingInterval.exact(point.y()), FloatingInterval.exact(point.z()));
    if (!value.isValid()) return 0;
    return value.getUpper() < 0 ? -1 : value.getLower() > 0 ? 1 : 0;
}

    private int boxSign(FloatingInterval x, FloatingInterval y, FloatingInterval z, Vector3Dd center) {
    final FloatingInterval range = encloseField(x, y, z);
    if (range.isValid() && range.getUpper() < 0) return -1;
    if (range.isValid() && range.getLower() > 0) return 1;
    FloatingInterval[] gxH = {new FloatingInterval()}, gyH = {new FloatingInterval()}, gzH = {new FloatingInterval()};
    if (!encloseFieldGradient(x, y, z, gxH, gyH, gzH) || !gxH[0].isValid() || !gyH[0].isValid() || !gzH[0].isValid())
        return 0;
    final FloatingInterval gx = gxH[0], gy = gyH[0], gz = gzH[0];
    final FloatingInterval cx = FloatingInterval.exact(center.x());
    final FloatingInterval cy = FloatingInterval.exact(center.y());
    final FloatingInterval cz = FloatingInterval.exact(center.z());
    FloatingInterval value = encloseField(cx, cy, cz);
    value = value.add(gx.multiply(x.subtract(cx)));
    value = value.add(gy.multiply(y.subtract(cy)));
    value = value.add(gz.multiply(z.subtract(cz)));
    if (!value.isValid()) return 0;
    return value.getUpper() < 0 ? -1 : value.getLower() > 0 ? 1 : 0;
}

    private RaySolidQueryStatus classifySegments(ArrayList<Double> distances, Vector3Dd start, Vector3Dd unit, double maximumDistance, ArrayList<Boolean> inside) {
    inside.clear();
    final long n = distances.size();
    for (long i = 0; i <= n; ++i) {
        final double a = i == 0 ? 0.0 : distances.get((int)(i-1));
        final double b = i == n ? maximumDistance : distances.get((int)(i));
        if (!(b > a)) return RaySolidQueryStatus.Indeterminate;
        // Any point of a root-free segment decides it; long segments are
        // sampled near their start to keep F finite.
        final double reach = Math.max(1.0, a);
        final double sample = b - a > 2.0*reach ? a + reach : 0.5*(a + b);
        final int sign = sampleSign(start, unit, sample);
        if (sign == 0) return RaySolidQueryStatus.Indeterminate;
        inside.add(sign < 0);
    }
    return RaySolidQueryStatus.Supported;
}

    // Field value; the solid is F < 0.
    public abstract double evaluateField(Vector3Dd point);

    // Gradient of the field, pointing out of the solid.
    public abstract Vector3Dd evaluateFieldGradient(Vector3Dd point);

    // Certified enclosure of F over the box x by y by z; invalid when unknown.
    public abstract FloatingInterval encloseField(FloatingInterval x, FloatingInterval y, FloatingInterval z);

    // Certified enclosures of the gradient over the box, used for the
    // mean-value form F(c) + grad F(box) . (box - c); false when unavailable.
    public boolean encloseFieldGradient(FloatingInterval x, FloatingInterval y, FloatingInterval z, FloatingInterval[] gx, FloatingInterval[] gy, FloatingInterval[] gz) {
    return false;
}

    // Appends the distances d >= 0 at which F(origin + d*unitDirection) = 0,
    // in any order, including at least every root up to maximumDistance.
    // Returns false when the roots cannot be found.
    public abstract boolean findRayRoots(Vector3Dd origin, Vector3Dd unitDirection, double maximumDistance, ArrayList<Double> distances);

    @Override
    public GeometryContainment classifyPoint(Vector3Dd point, double tolerance) {
    if (!Double.isFinite(tolerance) || tolerance < 0) return GeometryContainment.InvalidInput;
    if (tolerance != 0) return classifyPointCertified(point, tolerance);
    final double value = evaluateField(point);
    if (!Double.isFinite(value)) return GeometryContainment.Indeterminate;
    return value < 0 ? GeometryContainment.Inside : value > 0 ?
        GeometryContainment.Outside : GeometryContainment.Boundary;
}

    // Sign enclosures over the ball's bounding box prove Inside/Outside;
    // Boundary requires two certified points of opposite sign within the ball.
    @Override
    public GeometryContainment classifyPointCertified(Vector3Dd point, double tolerance) {
    if (!Double.isFinite(tolerance) || tolerance < 0 || !Double.isFinite(point.x()) ||
        !Double.isFinite(point.y()) || !Double.isFinite(point.z()))
        return GeometryContainment.InvalidInput;
    if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
    final FloatingInterval x = FloatingInterval.around(point.x(), tolerance);
    final FloatingInterval y = FloatingInterval.around(point.y(), tolerance);
    final FloatingInterval z = FloatingInterval.around(point.z(), tolerance);
    if (!x.isValid() || !y.isValid() || !z.isValid()) return GeometryContainment.Indeterminate;
    final int sign = boxSign(x, y, z, point);
    if (sign < 0) return GeometryContainment.Inside;
    if (sign > 0) return GeometryContainment.Outside;
    if (tolerance == 0) return GeometryContainment.Indeterminate;
    // F is continuous and the ball is convex: certified opposite signs at two
    // points of the ball prove a boundary point on the segment joining them.
    final Vector3Dd g = evaluateFieldGradient(point);
    final double length = g.length();
    if (!(length > 0) || !Double.isFinite(length)) return GeometryContainment.Indeterminate;
    final Vector3Dd unit = g.multiply(1.0/length);
    final int centre = enclosureSign(point);
    final double[] steps = {tolerance*(1.0 - 1.0/1048576.0), tolerance*0.5};
    for (int s = 0; s < 2; ++s) {
        int[] signs = {0, 0};
        for (int side = 0; side < 2; ++side) {
            final Vector3Dd q = point.add(unit.multiply(side == 0 ? -steps[s] : steps[s]));
            FloatingInterval offset = FloatingInterval.exact(0);
            offset = offset.add(FloatingInterval.exact(q.x()).subtract(FloatingInterval.exact(point.x())).square());
            offset = offset.add(FloatingInterval.exact(q.y()).subtract(FloatingInterval.exact(point.y())).square());
            offset = offset.add(FloatingInterval.exact(q.z()).subtract(FloatingInterval.exact(point.z())).square());
            final FloatingInterval distance = offset.squareRoot();
            if (!distance.isValid() || distance.getUpper() > tolerance) continue;
            signs[side] = enclosureSign(q);
        }
        if (signs[0]*signs[1] < 0) return GeometryContainment.Boundary;
        if (centre != 0 && (signs[0] == -centre || signs[1] == -centre)) return GeometryContainment.Boundary;
    }
    return GeometryContainment.Indeterminate;
}

    @Override
    public GeometryContainment classifyBoxCertified(Vector3Dd center, Vector3Dd halfWidths) {
    if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
    final FloatingInterval x = FloatingInterval.around(center.x(), halfWidths.x());
    final FloatingInterval y = FloatingInterval.around(center.y(), halfWidths.y());
    final FloatingInterval z = FloatingInterval.around(center.z(), halfWidths.z());
    if (!x.isValid() || !y.isValid() || !z.isValid()) return GeometryContainment.InvalidInput;
    final int sign = boxSign(x, y, z, center);
    return sign < 0 ? GeometryContainment.Inside : sign > 0 ? GeometryContainment.Outside :
        GeometryContainment.Indeterminate;
}

    @Override
    public RayFirstHitStatus queryFirstHit(Ray ray, RayCrossingQuery query, RayHit hit, RayBoundary boundary) {
    if (hit != null) hit.clear();
    if (boundary != null) boundary.assign(new RayBoundary());
    ArrayList<Double> distances = new ArrayList<Double>();
    Vector3Dd[] startH = {new Vector3Dd()}, unitH = {new Vector3Dd()};
    double[] lengthH = {0}, maximumDistanceH = {0};
    final RayFirstHitStatus status = collectRoots(ray, query, distances, startH, unitH,
        lengthH, maximumDistanceH);
    final Vector3Dd start = startH[0], unit = unitH[0];
    final double length = lengthH[0], maximumDistance = maximumDistanceH[0];
    if (status != RayFirstHitStatus.Hit) return status;
    final double t = query.minT + distances.get((int)(0))/length;
    if (t > query.maxT) return RayFirstHitStatus.Miss;
    if (boundary != null) boundary.assign(new RayBoundary(t, this));
    if (hit != null) {
        hit.setHitDistance(t);
        if (hit.shouldStoreRay()) hit.setRay(ray.withT(t));
        if (hit.needsAnySurfaceData()) this.doExtraInformation(ray, t, hit);
    }
    return RayFirstHitStatus.Hit;
}

    @Override
    public RayFirstHitStatus querySurfaceDetail(Ray ray, RayBoundary boundary, RayHit hit) {
    if (hit != null) hit.clear();
    if (hit == null || boundary.geometry != this || !Double.isFinite(boundary.t))
        return RayFirstHitStatus.InvalidInput;
    if (boundary.getInstanceCount() != 0) return RayFirstHitStatus.Unsupported;
    hit.setHitDistance(boundary.t);
    if (hit.shouldStoreRay()) hit.setRay(ray.withT(boundary.t));
    this.doExtraInformation(ray, boundary.t, hit);
    if (boundary.normalReversed && hit.needsNormal() &&
        (hit.getAvailableDetailMask() & RayHit.DETAIL_NORMAL) != 0)
        hit.normal = hit.normal.multiply(-1);
    return RayFirstHitStatus.Hit;
}

    @Override
    public RayCrossingSupport emitRayCrossings(Ray ray, RayCrossingQuery query, RayCrossingSink sink) {
    ArrayList<Double> distances = new ArrayList<Double>();
    Vector3Dd[] startH = {new Vector3Dd()}, unitH = {new Vector3Dd()};
    double[] lengthH = {0}, maximumDistanceH = {0};
    final RayFirstHitStatus status = collectRoots(ray, query, distances, startH, unitH,
        lengthH, maximumDistanceH);
    final Vector3Dd start = startH[0], unit = unitH[0];
    final double length = lengthH[0], maximumDistance = maximumDistanceH[0];
    if (status == RayFirstHitStatus.Miss) return RayCrossingSupport.Supported;
    if (status == RayFirstHitStatus.InvalidInput) return RayCrossingSupport.InvalidInput;
    if (status == RayFirstHitStatus.Unsupported) return RayCrossingSupport.Unsupported;
    if (status != RayFirstHitStatus.Hit) return RayCrossingSupport.Indeterminate;
    ArrayList<Boolean> inside = new ArrayList<Boolean>();
    if (classifySegments(distances, start, unit, maximumDistance, inside) !=
        RaySolidQueryStatus.Supported) return RayCrossingSupport.Indeterminate;
    for (long i = 0; i < distances.size(); ++i) {
        final RayCrossingKind kind = inside.get((int)(i)) == inside.get((int)(i+1)) ? RayCrossingKind.Tangent :
            inside.get((int)(i+1)) ? RayCrossingKind.Entering : RayCrossingKind.Leaving;
        sink.crossing(new RayCrossing(query.minT + distances.get((int)(i))/length, this, kind));
    }
    return RayCrossingSupport.Supported;
}

    @Override
    public RaySolidQueryStatus classifyRay(Ray ray, RayCrossingQuery query, RaySolidClassification result) {
    result.clear();
    if (Double.isNaN(query.minT) || Double.isNaN(query.maxT) ||
        query.minT >= query.maxT) return RaySolidQueryStatus.InvalidInput;
    ArrayList<Double> distances = new ArrayList<Double>();
    Vector3Dd[] startH = {new Vector3Dd()}, unitH = {new Vector3Dd()};
    double[] lengthH = {0}, maximumDistanceH = {0};
    final RayFirstHitStatus status = collectRoots(ray, query, distances, startH, unitH,
        lengthH, maximumDistanceH);
    final Vector3Dd start = startH[0], unit = unitH[0];
    final double length = lengthH[0], maximumDistance = maximumDistanceH[0];
    if (status == RayFirstHitStatus.InvalidInput) return RaySolidQueryStatus.InvalidInput;
    if (status == RayFirstHitStatus.Unsupported) return RaySolidQueryStatus.Unsupported;
    if (status == RayFirstHitStatus.Indeterminate) return RaySolidQueryStatus.Indeterminate;
    ArrayList<Boolean> inside = new ArrayList<Boolean>();
    if (classifySegments(distances, start, unit, maximumDistance, inside) !=
        RaySolidQueryStatus.Supported) return RaySolidQueryStatus.Indeterminate;
    boolean state = inside.get((int)(0));
    result.insideSegments.add(state);
    for (long i = 0; i < distances.size(); ++i) {
        if (inside.get((int)(i+1)) == state) continue;
        state = inside.get((int)(i+1));
        if (!result.boundaries.add(new RaySolidBoundary(query.minT + distances.get((int)(i))/length, this)) ||
            !result.insideSegments.add(state)) {
            result.clear(); return RaySolidQueryStatus.Indeterminate;
        }
    }
    return RaySolidQueryStatus.Supported;
}

    @Override
    public boolean doIntersectionFirstHit(Ray ray, RayHit hit) {
    return queryFirstHit(ray, new RayCrossingQuery(1e-9, Double.POSITIVE_INFINITY), hit) ==
        RayFirstHitStatus.Hit;
}

    @Override
    public void doExtraInformation(Ray ray, double t, RayHit hit) {
    if (hit == null) return;
    hit.beginDetailEvaluation();
    final Vector3Dd point = ray.getOrigin().add(ray.getDirection().multiply(t));
    if (hit.needsPoint()) { hit.point = point; hit.markDetailAvailable(RayHit.DETAIL_POINT); }
    if (hit.needsNormal()) {
        final Vector3Dd normal = evaluateFieldGradient(point);
        final double length = normal.length();
        if (Double.isFinite(length) && length > 0) {
            hit.normal = normal.multiply(1.0/length);
            hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
        }
    }
}

    @Override
    public int doContainmentTest(Vector3Dd point, double tolerance) {
    // Legacy integer adapter over the typed contract.
    final GeometryContainment result = classifyPoint(point, tolerance);
    if (result == GeometryContainment.Inside) return INSIDE;
    if (result == GeometryContainment.Boundary) return LIMIT;
    if (result == GeometryContainment.Outside) return OUTSIDE;
    throw new IllegalStateException("Implicit solid containment cannot be represented by the legacy integer result");
}

    @Override
    public double[] getMinMax() {
    GeometryBounds bounds = new GeometryBounds();
    getBounds(bounds);
    if (bounds.getKind() == GeometryBounds.Kind.Finite)
        return new double[]{bounds.getMinimum().x(), bounds.getMinimum().y(), bounds.getMinimum().z(),
            bounds.getMaximum().x(), bounds.getMaximum().y(), bounds.getMaximum().z()};
    return new double[]{-1e308,-1e308,-1e308,1e308,1e308,1e308};
}
}
