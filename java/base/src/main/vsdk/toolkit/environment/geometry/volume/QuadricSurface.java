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

/**
F(x,y,z) = square dot (x*x,y*y,z*z) + mixed dot (xy,xz,yz)
+ linear dot (x,y,z) + constant; the solid is F <= 0.
*/
public class QuadricSurface extends Solid {
    private Vector3Dd square;

    private Vector3Dd mixed;

    private Vector3Dd linear;

    private double constant;

    private FloatingInterval enclose(FloatingInterval x, FloatingInterval y, FloatingInterval z) {
    FloatingInterval result = FloatingInterval.exact(constant);
    result = result.add(FloatingInterval.exact(linear.x()).multiply(x));
    result = result.add(FloatingInterval.exact(linear.y()).multiply(y));
    result = result.add(FloatingInterval.exact(linear.z()).multiply(z));
    result = result.add(FloatingInterval.exact(square.x()).multiply(x.square()));
    result = result.add(FloatingInterval.exact(square.y()).multiply(y.square()));
    result = result.add(FloatingInterval.exact(square.z()).multiply(z.square()));
    result = result.add(FloatingInterval.exact(mixed.x()).multiply(x.multiply(y)));
    result = result.add(FloatingInterval.exact(mixed.y()).multiply(x.multiply(z)));
    return result.add(FloatingInterval.exact(mixed.z()).multiply(y.multiply(z)));
}

    private RayFirstHitStatus roots(Ray ray, RayCrossingQuery query, double[] first, double[] second) {
    final Vector3Dd o = ray.getOrigin();
    final Vector3Dd d = ray.getDirection();
    if (!Double.isFinite(o.x()) || !Double.isFinite(o.y()) || !Double.isFinite(o.z()) ||
        !Double.isFinite(d.x()) || !Double.isFinite(d.y()) || !Double.isFinite(d.z()) ||
        (d.x()==0 && d.y()==0 && d.z()==0) || Double.isNaN(query.minT) ||
        Double.isNaN(query.maxT) || query.minT > query.maxT) return RayFirstHitStatus.InvalidInput;
    if (query.minimumChordSquared != 0 || query.minimumExteriorApproach != Double.NEGATIVE_INFINITY)
        return RayFirstHitStatus.Unsupported;
    final double a = square.x()*d.x()*d.x() + square.y()*d.y()*d.y() + square.z()*d.z()*d.z() +
        mixed.x()*d.x()*d.y() + mixed.y()*d.x()*d.z() + mixed.z()*d.y()*d.z();
    final double b = gradient(o).dotProduct(d);
    final double c = evaluate(o);
    if (!Double.isFinite(a) || !Double.isFinite(b) || !Double.isFinite(c))
        return RayFirstHitStatus.Indeterminate;
    if (a==0 && b==0 && c==0) return RayFirstHitStatus.Indeterminate;
    if (a != 0 && !Double.isFinite(b*b - 4.0*a*c)) return RayFirstHitStatus.Indeterminate;
    if (!solve(a,b,c,first,second)) return RayFirstHitStatus.Miss;
    if (!Double.isFinite(first[0]) || !Double.isFinite(second[0])) return RayFirstHitStatus.Indeterminate;
    if (first[0] > second[0]) { final double tmp=first[0]; first[0]=second[0]; second[0]=tmp; }
    return RayFirstHitStatus.Hit;
}

    public QuadricSurface(Vector3Dd s, Vector3Dd m, Vector3Dd l, double c) {
        this.square = s;
        this.mixed = m;
        this.linear = l;
        this.constant = c;}

    /** cpp copy constructor. */
    public QuadricSurface(QuadricSurface other) {
        this(other.square, other.mixed, other.linear, other.constant);
    }

    // The evaluation order matches the historical POV implementation.
    public double evaluate(Vector3Dd point) {
        double result = point.dotProduct(linear) + constant;
        result += point.multiply(point).dotProduct(square);
        result += mixed.x()*point.x()*point.y() + mixed.y()*point.x()*point.z() +
            mixed.z()*point.y()*point.z();
        return result;
    }

    public Vector3Dd gradient(Vector3Dd point) {
        final Vector3Dd base = square.multiply(2.0).multiply(point).add(linear);
        return new Vector3Dd(base.x()+mixed.x()*point.y()+mixed.y()*point.z(),
            base.y()+mixed.x()*point.x()+mixed.z()*point.z(),
            base.z()+mixed.y()*point.x()+mixed.z()*point.y());
    }

    @Override
    public Geometry copyGeometry() { return new QuadricSurface(this); }

    public Vector3Dd squareCoefficients() { return square; }

    public Vector3Dd mixedCoefficients() { return mixed; }

    public Vector3Dd linearCoefficients() { return linear; }

    public double constantCoefficient() { return constant; }

    public static boolean solve(double a, double b, double c, double[] first, double[] second) {
        if (a != 0.0) {
            final double discriminant = b*b - 4.0*a*c;
            if (discriminant < 0.0) return false;
            final double root = Math.sqrt(discriminant);
            first[0] = (-b + root)/(2.0*a);
            second[0] = (-b - root)/(2.0*a);
        } else {
            if (b == 0.0) return false;
            first[0] = second[0] = -c/b;
        }
        return true;
    }

    @Override
    public void getBounds(GeometryBounds result) {
    if (!Double.isFinite(square.x()) || !Double.isFinite(square.y()) ||
        !Double.isFinite(square.z()) || !Double.isFinite(mixed.x()) ||
        !Double.isFinite(mixed.y()) || !Double.isFinite(mixed.z()) ||
        !Double.isFinite(linear.x()) || !Double.isFinite(linear.y()) ||
        !Double.isFinite(linear.z()) || !Double.isFinite(constant)) {
        result.setIndeterminate(); return;
    }
    // POV's positive-definite ellipsoid bound. Other signs and singular or
    // poorly conditioned matrices remain conservatively unbounded.
    final double a11 = square.x(), a22 = square.y(), a33 = square.z();
    final double a12 = mixed.x()*0.5, a13 = mixed.y()*0.5, a23 = mixed.z()*0.5;
    final double minor1 = a11;
    final double minor2 = a11*a22 - a12*a12;
    final double det = a11*(a22*a33 - a23*a23) - a12*(a12*a33 - a23*a13) +
        a13*(a12*a23 - a22*a13);
    if (!Double.isFinite(minor2) || !Double.isFinite(det)) {
        result.setIndeterminate(); return;
    }
    if (minor1 <= 1e-12 || minor2 <= 1e-12 || det <= 1e-12) {
        result.setUnbounded(); return;
    }
    final double invDet = 1.0/det;
    final double inv11 = (a22*a33 - a23*a23)*invDet;
    final double inv12 = (a13*a23 - a12*a33)*invDet;
    final double inv13 = (a12*a23 - a13*a22)*invDet;
    final double inv22 = (a11*a33 - a13*a13)*invDet;
    final double inv23 = (a12*a13 - a11*a23)*invDet;
    final double inv33 = (a11*a22 - a12*a12)*invDet;
    if (!Double.isFinite(inv11) || !Double.isFinite(inv12) ||
        !Double.isFinite(inv13) || !Double.isFinite(inv22) ||
        !Double.isFinite(inv23) || !Double.isFinite(inv33)) {
        result.setIndeterminate(); return;
    }
    final Vector3Dd center = new Vector3Dd(
        -0.5*(inv11*linear.x()+inv12*linear.y()+inv13*linear.z()),
        -0.5*(inv12*linear.x()+inv22*linear.y()+inv23*linear.z()),
        -0.5*(inv13*linear.x()+inv23*linear.y()+inv33*linear.z()));
    final Vector3Dd aCenter = new Vector3Dd(
        a11*center.x()+a12*center.y()+a13*center.z(),
        a12*center.x()+a22*center.y()+a23*center.z(),
        a13*center.x()+a23*center.y()+a33*center.z());
    final double radiusSquared = center.dotProduct(aCenter)-constant;
    if (!Double.isFinite(radiusSquared)) { result.setIndeterminate(); return; }
    if (radiusSquared <= 1e-12 || inv11 <= 0 || inv22 <= 0 || inv33 <= 0) {
        result.setUnbounded(); return;
    }
    final Vector3Dd extent = new Vector3Dd(Math.sqrt(radiusSquared*inv11),
        Math.sqrt(radiusSquared*inv22), Math.sqrt(radiusSquared*inv33));
    result.setFinite(center.subtract(extent), center.add(extent));
}

    @Override
    public GeometryContainment classifyPoint(Vector3Dd point, double tolerance) {
    if (!Double.isFinite(tolerance) || tolerance < 0) return GeometryContainment.InvalidInput;
    if (tolerance != 0) return classifyPointCertified(point, tolerance);
    final double value = evaluate(point);
    if (!Double.isFinite(value)) return GeometryContainment.Indeterminate;
    return value < 0 ? GeometryContainment.Inside : value > 0 ? GeometryContainment.Outside : GeometryContainment.Boundary;
}

    // Sign enclosures of F over the ball's bounding box prove Inside/Outside;
    // Boundary requires two certified points of opposite sign within the ball.
    @Override
    public GeometryContainment classifyPointCertified(Vector3Dd point, double tolerance) {
    if (!Double.isFinite(tolerance) || tolerance < 0 || !Double.isFinite(point.x()) ||
        !Double.isFinite(point.y()) || !Double.isFinite(point.z()))
        return GeometryContainment.InvalidInput;
    final double[] coefficients = {square.x(), square.y(), square.z(), mixed.x(), mixed.y(),
        mixed.z(), linear.x(), linear.y(), linear.z(), constant};
    for (int i = 0; i < 10; ++i)
        if (!Double.isFinite(coefficients[i])) return GeometryContainment.InvalidInput;
    if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
    final double[] coordinates = {point.x(), point.y(), point.z()};
    FloatingInterval[] box = new FloatingInterval[3];
    for (int i = 0; i < 3; ++i) {
        box[i] = FloatingInterval.around(coordinates[i], tolerance);
        if (!box[i].isValid()) return GeometryContainment.Indeterminate;
    }
    final FloatingInterval range = enclose(box[0], box[1], box[2]);
    if (!range.isValid()) return GeometryContainment.Indeterminate;
    if (range.getUpper() < 0) return GeometryContainment.Inside;
    if (range.getLower() > 0) return GeometryContainment.Outside;
    if (tolerance == 0) return GeometryContainment.Indeterminate;
    // F is continuous and the ball is convex: certified opposite signs at two
    // points of the ball prove a boundary point on the segment joining them.
    final Vector3Dd g = gradient(point);
    final double length = g.length();
    if (!(length > 0) || !Double.isFinite(length)) return GeometryContainment.Indeterminate;
    final Vector3Dd unit = g.multiply(1.0 / length);
    final double[] steps = {tolerance * (1.0 - 1.0 / 1048576.0), tolerance * 0.5};
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
            final FloatingInterval value = enclose(FloatingInterval.exact(q.x()),
                FloatingInterval.exact(q.y()), FloatingInterval.exact(q.z()));
            if (!value.isValid()) continue;
            signs[side] = value.getUpper() < 0 ? -1 : value.getLower() > 0 ? 1 : 0;
        }
        if (signs[0] * signs[1] < 0) return GeometryContainment.Boundary;
        // The centre itself may be the second point.
        final FloatingInterval centre = enclose(FloatingInterval.exact(point.x()),
            FloatingInterval.exact(point.y()), FloatingInterval.exact(point.z()));
        if (centre.isValid()) {
            final int sign = centre.getUpper() < 0 ? -1 : centre.getLower() > 0 ? 1 : 0;
            if (sign != 0 && (signs[0] == -sign || signs[1] == -sign)) return GeometryContainment.Boundary;
        }
    }
    return GeometryContainment.Indeterminate;
}

    @Override
    public GeometryContainment classifyBoxCertified(Vector3Dd center, Vector3Dd halfWidths) {
    final double[] coefficients = {square.x(), square.y(), square.z(), mixed.x(), mixed.y(),
        mixed.z(), linear.x(), linear.y(), linear.z(), constant};
    for (int i = 0; i < 10; ++i)
        if (!Double.isFinite(coefficients[i])) return GeometryContainment.InvalidInput;
    if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
    final FloatingInterval x = FloatingInterval.around(center.x(), halfWidths.x());
    final FloatingInterval y = FloatingInterval.around(center.y(), halfWidths.y());
    final FloatingInterval z = FloatingInterval.around(center.z(), halfWidths.z());
    if (!x.isValid() || !y.isValid() || !z.isValid()) return GeometryContainment.InvalidInput;
    final FloatingInterval range = enclose(x, y, z);
    if (!range.isValid()) return GeometryContainment.Indeterminate;
    if (range.getUpper() < 0) return GeometryContainment.Inside;
    if (range.getLower() > 0) return GeometryContainment.Outside;
    return GeometryContainment.Indeterminate;
}

    @Override
    public RayFirstHitStatus queryFirstHit(Ray ray, RayCrossingQuery query, RayHit hit, RayBoundary boundary) {
    if (hit != null) hit.clear();
    if (boundary != null) boundary.assign(new RayBoundary());
    double[] aH={0},bH={0}; final RayFirstHitStatus status=roots(ray,query,aH,bH);
    final double a=aH[0], b=bH[0];
    if (status != RayFirstHitStatus.Hit) return status;
    final double t=a>=query.minT ? a : b;
    if (t<query.minT || t>query.maxT) return RayFirstHitStatus.Miss;
    if (boundary != null) boundary.assign(new RayBoundary(t,this));
    if (hit != null) { hit.setHitDistance(t); if (hit.shouldStoreRay()) hit.setRay(ray.withT(t));
        if (hit.needsAnySurfaceData()) this.doExtraInformation(ray,t,hit); }
    return RayFirstHitStatus.Hit;
}

    @Override
    public RayFirstHitStatus querySurfaceDetail(Ray ray, RayBoundary boundary, RayHit hit) {
    if (hit != null) hit.clear();
    if (hit == null || boundary.geometry != this || !Double.isFinite(boundary.t))
        return RayFirstHitStatus.InvalidInput;
    if (boundary.getInstanceCount() != 0) return RayFirstHitStatus.Unsupported;
    final RayFirstHitStatus status = queryFirstHit(ray,
        new RayCrossingQuery(boundary.t, boundary.t), null);
    if (status != RayFirstHitStatus.Hit) return status;
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
    double[] aH={0},bH={0}; final RayFirstHitStatus status=roots(ray,query,aH,bH);
    final double a=aH[0], b=bH[0];
    if (status == RayFirstHitStatus.Miss) return RayCrossingSupport.Supported;
    if (status == RayFirstHitStatus.InvalidInput) return RayCrossingSupport.InvalidInput;
    if (status == RayFirstHitStatus.Unsupported) return RayCrossingSupport.Unsupported;
    if (status != RayFirstHitStatus.Hit) return RayCrossingSupport.Indeterminate;
    final Vector3Dd direction=ray.getDirection();
    final boolean emitFirst = a>=query.minT && a<=query.maxT;
    final boolean emitSecond = a!=b && b>=query.minT && b<=query.maxT;
    double slopeFirst = 0, slopeSecond = 0;
    if (emitFirst) {
        slopeFirst=gradient(ray.getOrigin().add(direction.multiply(a))).dotProduct(direction);
        if (!Double.isFinite(slopeFirst) || (a != b && slopeFirst == 0)) return RayCrossingSupport.Indeterminate;
    }
    if (emitSecond) {
        slopeSecond=gradient(ray.getOrigin().add(direction.multiply(b))).dotProduct(direction);
        if (!Double.isFinite(slopeSecond) || slopeSecond == 0) return RayCrossingSupport.Indeterminate;
    }
    if (emitFirst) sink.crossing(new RayCrossing(a,this,slopeFirst == 0 ? RayCrossingKind.Tangent :
        slopeFirst<0 ? RayCrossingKind.Entering : RayCrossingKind.Leaving));
    if (emitSecond) sink.crossing(new RayCrossing(b,this,
        slopeSecond<0 ? RayCrossingKind.Entering : RayCrossingKind.Leaving));
    return RayCrossingSupport.Supported;
}

    @Override
    public RaySolidQueryStatus classifyRay(Ray ray, RayCrossingQuery query, RaySolidClassification result) {
    result.clear();
    if (Double.isNaN(query.minT) || Double.isNaN(query.maxT) ||
        query.minT >= query.maxT) return RaySolidQueryStatus.InvalidInput;
    double[] firstH = {0}, secondH = {0};
    final RayFirstHitStatus status = roots(ray, query, firstH, secondH);
    final double first = firstH[0], second = secondH[0];
    if (status == RayFirstHitStatus.InvalidInput) return RaySolidQueryStatus.InvalidInput;
    if (status == RayFirstHitStatus.Unsupported) return RaySolidQueryStatus.Unsupported;
    if (status == RayFirstHitStatus.Indeterminate) return RaySolidQueryStatus.Indeterminate;
    if (status == RayFirstHitStatus.Miss) {
        final double value = evaluate(ray.getOrigin());
        if (!Double.isFinite(value) || value == 0) return RaySolidQueryStatus.Indeterminate;
        return result.insideSegments.add(value < 0) ? RaySolidQueryStatus.Supported :
            RaySolidQueryStatus.Indeterminate;
    }
    final Vector3Dd direction = ray.getDirection();
    final double slopeFirst = gradient(ray.getOrigin().add(direction.multiply(first))).dotProduct(direction);
    if (!Double.isFinite(slopeFirst)) return RaySolidQueryStatus.Indeterminate;
    if (first == second && slopeFirst == 0) {
        // A tangent does not change interval membership. F's quadratic term
        // determines whether the ray lies outside or inside on both sides.
        final double a = square.x()*direction.x()*direction.x() +
            square.y()*direction.y()*direction.y() + square.z()*direction.z()*direction.z() +
            mixed.x()*direction.x()*direction.y() + mixed.y()*direction.x()*direction.z() +
            mixed.z()*direction.y()*direction.z();
        if (!Double.isFinite(a) || a == 0) return RaySolidQueryStatus.Indeterminate;
        return result.insideSegments.add(a < 0) ? RaySolidQueryStatus.Supported :
            RaySolidQueryStatus.Indeterminate;
    }
    if (slopeFirst == 0) return RaySolidQueryStatus.Indeterminate;
    double slopeSecond = 0;
    if (first != second) {
        slopeSecond = gradient(ray.getOrigin().add(direction.multiply(second))).dotProduct(direction);
        if (!Double.isFinite(slopeSecond) || slopeSecond == 0 ||
            (slopeFirst < 0) == (slopeSecond < 0)) return RaySolidQueryStatus.Indeterminate;
    }
    boolean inside = slopeFirst > 0;
    if (query.minT > first) inside = slopeFirst < 0;
    if (first != second && query.minT > second) inside = slopeSecond < 0;
    if (!result.insideSegments.add(inside)) return RaySolidQueryStatus.Indeterminate;
    if (first >= query.minT && first <= query.maxT) {
        if (!result.boundaries.add(new RaySolidBoundary(first, this)) ||
            !result.insideSegments.add(slopeFirst < 0)) {
            result.clear(); return RaySolidQueryStatus.Indeterminate;
        }
    }
    if (first != second && second >= query.minT && second <= query.maxT) {
        if (!result.boundaries.add(new RaySolidBoundary(second, this)) ||
            !result.insideSegments.add(slopeSecond < 0)) {
            result.clear(); return RaySolidQueryStatus.Indeterminate;
        }
    }
    return RaySolidQueryStatus.Supported;
}

    @Override
    public boolean doIntersectionFirstHit(Ray ray, RayHit hit) {
    return queryFirstHit(ray,new RayCrossingQuery(1e-9,Double.POSITIVE_INFINITY),hit)==RayFirstHitStatus.Hit;
}

    @Override
    public void doExtraInformation(Ray ray, double t, RayHit hit) {
    if (hit == null) return;
    hit.beginDetailEvaluation();
    final Vector3Dd point=ray.getOrigin().add(ray.getDirection().multiply(t));
    if (hit.needsPoint()) { hit.point=point; hit.markDetailAvailable(RayHit.DETAIL_POINT); }
    if (hit.needsNormal()) {
        final Vector3Dd normal=gradient(point); final double length=normal.length();
        if (Double.isFinite(length) && length>0) {
            hit.normal=normal.multiply(1.0/length); hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
        }
    }
}

    @Override
    public int doContainmentTest(Vector3Dd p, double tolerance) {
    // Legacy integer adapter over the typed world-distance contract.
    final GeometryContainment result = classifyPoint(p, tolerance);
    if (result == GeometryContainment.Inside) return INSIDE;
    if (result == GeometryContainment.Boundary) return LIMIT;
    if (result == GeometryContainment.Outside) return OUTSIDE;
    throw new IllegalStateException("Quadric containment cannot be represented by the legacy integer result");
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
