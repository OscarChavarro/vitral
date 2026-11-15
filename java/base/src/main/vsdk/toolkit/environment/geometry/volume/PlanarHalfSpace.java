package vsdk.toolkit.environment.geometry.volume;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.numerics.FloatingInterval;
import vsdk.toolkit.environment.geometry.Geometry;
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
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;

/**
The closed solid n dot p + d <= 0. The plane owns its mathematical
coefficients; placement and complement are independent scene operations.
*/
public class PlanarHalfSpace extends Solid {
    private final InfinitePlane plane;

    public PlanarHalfSpace(double a, double b, double c, double d) {
        plane = new InfinitePlane(a,b,c,d);
    }

    public PlanarHalfSpace(InfinitePlane boundary) {
        plane = new InfinitePlane(boundary);
    }

    @Override
    public Geometry copyGeometry() { return new PlanarHalfSpace(plane); }

    public InfinitePlane getBoundaryPlane() { return plane; }

    @Override
    public void getBounds(GeometryBounds result) { plane.getBounds(result); }

    @Override
    public GeometryContainment classifyPoint(Vector3Dd point, double tolerance) {
    final double a = plane.getA(), b = plane.getB(), c = plane.getC(), d = plane.getD();
    if (!Double.isFinite(a) || !Double.isFinite(b) || !Double.isFinite(c) ||
        !Double.isFinite(d) || (a == 0 && b == 0 && c == 0) ||
        !Double.isFinite(point.x()) || !Double.isFinite(point.y()) ||
        !Double.isFinite(point.z()) || !Double.isFinite(tolerance) || tolerance < 0)
        return GeometryContainment.InvalidInput;
    if (tolerance > 0) return classifyPointCertified(point, tolerance);
    final double squared = a*a + b*b + c*c;
    final double value = plane.pointDistance(point);
    if (!Double.isFinite(squared) || squared == 0 || !Double.isFinite(value))
        return GeometryContainment.Indeterminate;
    if (value == 0 &&
        ((a != 0 && point.x() != 0 && a * point.x() == 0) ||
         (b != 0 && point.y() != 0 && b * point.y() == 0) ||
         (c != 0 && point.z() != 0 && c * point.z() == 0)))
        return GeometryContainment.Indeterminate;
    final double signedDistance = value / Math.sqrt(squared);
    if (!Double.isFinite(signedDistance) || (value != 0 && signedDistance == 0))
        return GeometryContainment.Indeterminate;
    if (signedDistance < -tolerance) return GeometryContainment.Inside;
    if (signedDistance > tolerance) return GeometryContainment.Outside;
    return GeometryContainment.Boundary;
}

    @Override
    public GeometryContainment classifyPointCertified(Vector3Dd point, double tolerance) {
    final double a = plane.getA(), b = plane.getB(), c = plane.getC(), d = plane.getD();
    if (!Double.isFinite(a) || !Double.isFinite(b) || !Double.isFinite(c) ||
        !Double.isFinite(d) || (a == 0 && b == 0 && c == 0) ||
        !Double.isFinite(point.x()) || !Double.isFinite(point.y()) ||
        !Double.isFinite(point.z()) || !Double.isFinite(tolerance) || tolerance < 0)
        return GeometryContainment.InvalidInput;
    if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
    final FloatingInterval A = FloatingInterval.exact(a), B = FloatingInterval.exact(b),
        C = FloatingInterval.exact(c);
    final FloatingInterval value = A.multiply(FloatingInterval.exact(point.x()))
        .add(B.multiply(FloatingInterval.exact(point.y())))
        .add(C.multiply(FloatingInterval.exact(point.z()))).add(FloatingInterval.exact(d));
    final FloatingInterval norm = A.square().add(B.square()).add(C.square()).squareRoot();
    if (!value.isValid() || !norm.isValid() || !(norm.getLower() > 0)) return GeometryContainment.Indeterminate;
    final FloatingInterval signedDistance = value.divide(norm);
    if (!signedDistance.isValid()) return GeometryContainment.Indeterminate;
    if (signedDistance.getUpper() < -tolerance) return GeometryContainment.Inside;
    if (signedDistance.getLower() > tolerance) return GeometryContainment.Outside;
    if (signedDistance.getLower() >= -tolerance && signedDistance.getUpper() <= tolerance)
        return GeometryContainment.Boundary;
    return GeometryContainment.Indeterminate;
}

    @Override
    public GeometryContainment classifyBoxCertified(Vector3Dd center, Vector3Dd halfWidths) {
    final double a = plane.getA(), b = plane.getB(), c = plane.getC(), d = plane.getD();
    if (!Double.isFinite(a) || !Double.isFinite(b) || !Double.isFinite(c) ||
        !Double.isFinite(d) || (a == 0 && b == 0 && c == 0))
        return GeometryContainment.InvalidInput;
    if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
    final FloatingInterval x = FloatingInterval.around(center.x(), halfWidths.x());
    final FloatingInterval y = FloatingInterval.around(center.y(), halfWidths.y());
    final FloatingInterval z = FloatingInterval.around(center.z(), halfWidths.z());
    if (!x.isValid() || !y.isValid() || !z.isValid()) return GeometryContainment.InvalidInput;
    final FloatingInterval value = FloatingInterval.exact(a).multiply(x)
        .add(FloatingInterval.exact(b).multiply(y))
        .add(FloatingInterval.exact(c).multiply(z)).add(FloatingInterval.exact(d));
    if (!value.isValid()) return GeometryContainment.Indeterminate;
    if (value.getUpper() < 0) return GeometryContainment.Inside;
    if (value.getLower() > 0) return GeometryContainment.Outside;
    return GeometryContainment.Indeterminate;
}

    @Override
    public int doContainmentTest(Vector3Dd point, double tolerance) {
    final GeometryContainment result = classifyPoint(point, tolerance);
    if (result == GeometryContainment.Inside) return INSIDE;
    if (result == GeometryContainment.Outside) return OUTSIDE;
    if (result == GeometryContainment.Boundary) return LIMIT;
    throw new IllegalStateException("Planar half-space containment is indeterminate or invalid");
}

    @Override
    public RayCrossingSupport emitRayCrossings(Ray ray, RayCrossingQuery query, RayCrossingSink sink) {
    RayHit hit = new RayHit(RayHit.DETAIL_NONE, false);
    final RayFirstHitStatus status = plane.queryFirstHit(ray, query, hit);
    if (status == RayFirstHitStatus.Unsupported) return RayCrossingSupport.Unsupported;
    if (status == RayFirstHitStatus.InvalidInput) return RayCrossingSupport.InvalidInput;
    if (status == RayFirstHitStatus.Indeterminate) return RayCrossingSupport.Indeterminate;
    if (status == RayFirstHitStatus.Hit) {
        final Vector3Dd normal = new Vector3Dd(plane.getA(), plane.getB(), plane.getC());
        final double slope = normal.dotProduct(ray.getDirection());
        if (!Double.isFinite(slope) || slope == 0) return RayCrossingSupport.Indeterminate;
        sink.crossing(new RayCrossing(hit.getHitDistance(), this,
            slope < 0 ? RayCrossingKind.Entering : RayCrossingKind.Leaving));
    }
    return RayCrossingSupport.Supported;
}

    @Override
    public RayFirstHitStatus querySurfaceDetail(Ray ray, RayBoundary boundary, RayHit hit) {
    if (hit != null) hit.clear();
    if (hit == null || boundary.geometry != this || !Double.isFinite(boundary.t))
        return RayFirstHitStatus.InvalidInput;
    if (boundary.getInstanceCount() != 0) return RayFirstHitStatus.Unsupported;
    return plane.querySurfaceDetail(ray, new RayBoundary(boundary.t, plane, boundary.normalReversed), hit);
}

    @Override
    public RayFirstHitStatus queryFirstHit(Ray ray, RayCrossingQuery query, RayHit hit, RayBoundary boundary) {
    if (boundary != null) boundary.assign(new RayBoundary());
    RayBoundary planeBoundary = new RayBoundary();
    final RayFirstHitStatus status = plane.queryFirstHit(ray, query, hit,
        boundary != null ? planeBoundary : null);
    if (status == RayFirstHitStatus.Hit && boundary != null) boundary.assign(new RayBoundary(planeBoundary.t, this));
    return status;
}

    @Override
    public RaySolidQueryStatus classifyRay(Ray ray, RayCrossingQuery query, RaySolidClassification result) {
    result.clear();
    if (Double.isNaN(query.minT) || Double.isNaN(query.maxT) || query.minT >= query.maxT)
        return RaySolidQueryStatus.InvalidInput;
    final RayCrossingQuery full = new RayCrossingQuery(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
        query.minimumChordSquared, query.minimumExteriorApproach);
    RayHit hit = new RayHit(RayHit.DETAIL_NONE, false);
    final RayFirstHitStatus status = plane.queryFirstHit(ray, full, hit);
    if (status == RayFirstHitStatus.Unsupported) return RaySolidQueryStatus.Unsupported;
    if (status == RayFirstHitStatus.InvalidInput) return RaySolidQueryStatus.InvalidInput;
    if (status == RayFirstHitStatus.Indeterminate) return RaySolidQueryStatus.Indeterminate;
    if (status == RayFirstHitStatus.Miss) {
        final double value = plane.pointDistance(ray.getOrigin());
        if (!Double.isFinite(value) || value == 0) return RaySolidQueryStatus.Indeterminate;
        return result.insideSegments.add(value < 0) ? RaySolidQueryStatus.Supported :
            RaySolidQueryStatus.Indeterminate;
    }
    final Vector3Dd normal = new Vector3Dd(plane.getA(), plane.getB(), plane.getC());
    final double slope = normal.dotProduct(ray.getDirection());
    final double t = hit.getHitDistance();
    if (!Double.isFinite(slope) || slope == 0 || !Double.isFinite(t))
        return RaySolidQueryStatus.Indeterminate;
    final boolean before = slope > 0;
    final boolean initial = query.minT <= t ? before : !before;
    if (!result.insideSegments.add(initial)) return RaySolidQueryStatus.Indeterminate;
    if (t >= query.minT && t <= query.maxT) {
        if (!result.boundaries.add(new RaySolidBoundary(t, this)) || !result.insideSegments.add(!before)) {
            result.clear(); return RaySolidQueryStatus.Indeterminate;
        }
    }
    return RaySolidQueryStatus.Supported;
}

    @Override
    public boolean doIntersectionFirstHit(Ray ray, RayHit hit) {
    return queryFirstHit(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), hit) == RayFirstHitStatus.Hit;
}

    @Override
    public void doExtraInformation(Ray ray, double t, RayHit hit) {
    plane.doExtraInformation(ray, t, hit);
}

    @Override
    public double[] getMinMax() { return plane.getMinMax(); }
}
