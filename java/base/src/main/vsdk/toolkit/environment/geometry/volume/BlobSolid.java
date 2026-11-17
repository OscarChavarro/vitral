package vsdk.toolkit.environment.geometry.volume;

import java.util.ArrayList;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.numerics.FloatingInterval;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.numericalAnalysis.polynomial.PolynomialSolver;
import vsdk.toolkit.numericalAnalysis.polynomial.QuadraticSolver;
import vsdk.toolkit.numericalAnalysis.polynomial.QuarticSolver;

/**
Blob of spherical field components (PovRay 1.0 blob): the solid where the
summed component density exceeds the threshold, F = threshold - density.
Along a ray each set of overlapping influence spheres gives a quartic in the
ray distance, solved by the closed-form quartic solver or, when requested, by
Sturm sequences. Certified containment encloses the exact field
strength * (1 - d^2/radius^2)^2; bounds are the boxes of positive components
for a nonnegative threshold, and unbounded for a negative one.
*/
public class BlobSolid extends ImplicitSolid {
    public static final double COEFFICIENT_LIMIT = 1.0e-20;

    public static final double SOLVER_EPSILON = 1.0e-10;

    // Rays grazing an influence sphere closer than this are ignored, and
    // roots with a squared gradient below it are rejected as spurious.
    public static final double GRAZING_LIMIT = 1.0e-5;

    private final ArrayList<BlobComponent> components = new ArrayList<BlobComponent>();

    private double threshold;

    private boolean sturm;

    private long determineInfluences(Vector3Dd p, Vector3Dd d, double influenceStart, ArrayList<BlobInfluenceInterval> intervals) {
    intervals.clear();
    for (long i = 0; i < components.size(); i++) {
        // Standard sphere intersection with the volume of influence.
        final Vector3Dd v = components.get((int)(i)).getCenter().subtract(p);
        final double b = v.dotProduct(d);
        final double t = v.dotProduct(v);
        double disc = b * b - t + components.get((int)(i)).getRadiusSquared();
        if (disc < GRAZING_LIMIT) {
            continue;
        }
        disc = Math.sqrt(disc);
        double t1 = b + disc;
        if (t1 < influenceStart) {
            t1 = 0.0;
        }
        double t0 = b - disc;
        if (t0 < influenceStart) {
            t0 = 0.0;
        }
        if (t1 == t0) {
            continue;
        }
        if (t1 < t0) {
            disc = t0;
            t0 = t1;
            t1 = disc;
        }
        long k;
        for (k = 0; k < intervals.size() && t0 > intervals.get((int)(k)).getBound(); k++) {
        }
        if (k < intervals.size()) {
            // Smaller than an existing bound: insert it here.
            intervals.add((int)k, new BlobInfluenceInterval(true, i, t0));
            for (k = k + 1; k < intervals.size() && t1 > intervals.get((int)(k)).getBound(); k++) {
            }
            if (k < intervals.size()) {
                intervals.add((int)k, new BlobInfluenceInterval(false, i, t1));
            } else {
                intervals.add(new BlobInfluenceInterval(false, i, t1));
            }
        } else {
            intervals.add(new BlobInfluenceInterval(true, i, t0));
            intervals.add(new BlobInfluenceInterval(false, i, t1));
        }
    }
    return intervals.size();
}

    private FloatingInterval clampedFalloff(long index, FloatingInterval x, FloatingInterval y, FloatingInterval z) {
    final BlobComponent component = components.get((int)(index));
    final FloatingInterval dx = x.subtract(FloatingInterval.exact(component.getCenter().x()));
    final FloatingInterval dy = y.subtract(FloatingInterval.exact(component.getCenter().y()));
    final FloatingInterval dz = z.subtract(FloatingInterval.exact(component.getCenter().z()));
    final FloatingInterval q = dx.square().add(dy.square()).add(dz.square()).divide(
        FloatingInterval.exact(component.getRadius()).square());
    if (!q.isValid()) return q;
    if (q.getLower() >= 1.0) return FloatingInterval.exact(0.0);
    // 1 - min(q, 1): the exact falloff is continuous and zero beyond the radius.
    return FloatingInterval.exact(1.0).subtract(FloatingInterval.spanning(
        Math.max(q.getLower(), 0.0), Math.min(q.getUpper(), 1.0)));
}

    public BlobSolid(double thresholdValue, boolean useSturmSequences) {
        this.threshold = thresholdValue;
        this.sturm = useSturmSequences;
        addControlSpecifications();
}

    /**
    The state edited here is the threshold. The components are a list of
    their own, and using Sturm sequences is a choice of the intersection
    algorithm, not part of the shape. A density above a nonpositive
    threshold would fill the whole space, so it must be positive.
    */
    private void addControlSpecifications() {
        getControlSpecifications().add("double;threshold;(0, INFINITE)");
    }

    /** cpp copy constructor; BlobComponent values are immutable and shared. */
    public BlobSolid(BlobSolid other) {
        this.components.addAll(other.components);
        this.threshold = other.threshold;
        this.sturm = other.sturm;
        addControlSpecifications();
    }

    public void addComponent(BlobComponent component) {
    components.add(component);
}

    public long getComponentCount() { return components.size(); }

    public BlobComponent getComponent(long index) { return components.get((int)(index)); }

    public double getThreshold() { return threshold; }

    public void setThreshold(double value) { threshold = value; }

    public boolean usesSturmSequences() { return sturm; }

    // Summed component density at the point.
    public double evaluateDensity(Vector3Dd pos) {
    double density = 0.0;
    for (long i = 0; i < components.size(); i++) {
        final BlobComponent component = components.get((int)(i));
        final Vector3Dd v = component.getCenter().subtract(pos);
        final double len = v.dotProduct(v);
        if (len < component.getRadiusSquared()) {
            // Inside the radius of influence of this component.
            density += len * (len * component.getCoefficients()[0] +
                component.getCoefficients()[1]) + component.getCoefficients()[2];
        }
    }
    return density;
}

    // Gradient of threshold - density, pointing out of the solid.
    public Vector3Dd gradient(Vector3Dd point) {
    Vector3Dd result = new Vector3Dd(0.0, 0.0, 0.0);
    for (long i = 0; i < components.size(); i++) {
        final BlobComponent component = components.get((int)(i));
        final Vector3Dd v = new Vector3Dd(point.x() - component.getCenter().x(),
            point.y() - component.getCenter().y(), point.z() - component.getCenter().z());
        final double dist = v.x() * v.x() + v.y() * v.y() + v.z() * v.z();
        if (dist <= component.getRadiusSquared()) {
            final double val = -2.0 * (2.0 * component.getCoefficients()[0] * dist +
                component.getCoefficients()[1]);
            result = new Vector3Dd(result.x() + val * v.x(), result.y() + val * v.y(),
                result.z() + val * v.z());
        }
    }
    return result;
}

    // Appends the PovRay ray roots: distances d >= minimumDistance along the
    // unit direction, inside the influence intervals that start no earlier than
    // influenceStart (earlier starts are moved to 0), whose gradient is usable.
    public void intersect(Vector3Dd p, Vector3Dd d, double minimumDistance, double influenceStart, ArrayList<Double> distances) {
    ArrayList<BlobInfluenceInterval> intervals = new ArrayList<BlobInfluenceInterval>(2 * components.size());
    final long cnt = determineInfluences(p, d, influenceStart, intervals);
    if (cnt == 0) {
        return;
    }
    ArrayList<Double> componentTerms = new ArrayList<Double>(components.size() * 5);
    for (long i = 0; i < components.size() * 5; i++) componentTerms.add(0.0);
    double[] coefficients = new double[5];
    double[] roots = new double[4];
    for (int i = 0; i < 4; i++) {
        coefficients[i] = 0.0;
    }
    coefficients[4] = -threshold;
    int inFlag = 0;
    for (long i = 0; i < cnt; i++) {
        // cpp points into componentTerms; Java keeps the offset of the five terms.
        final int terms = (int)(intervals.get((int)i).getComponent() * 5);
        if (intervals.get((int)i).isStart()) {
            // A component starts influencing the ray: add its terms.
            inFlag++;
            final BlobComponent component = components.get((int)intervals.get((int)i).getComponent());
            final Vector3Dd v = p.subtract(component.getCenter());
            final double c0 = component.getCoefficients()[0];
            final double c1 = component.getCoefficients()[1];
            final double c2 = component.getCoefficients()[2];
            final double t0 = v.dotProduct(v);
            final double t1 = v.dotProduct(d);
            componentTerms.set(terms + 0, c0);
            componentTerms.set(terms + 1, 4.0 * c0 * t1);
            componentTerms.set(terms + 2, 2.0 * c0 * (2.0 * t1 * t1 + t0) + c1);
            componentTerms.set(terms + 3, 2.0 * t1 * (2.0 * c0 * t0 + c1));
            componentTerms.set(terms + 4, c0 * t0 * t0 + c1 * t0 + c2);
            for (int j = 0; j < 5; j++) {
                coefficients[j] += componentTerms.get(terms + j);
            }
        } else {
            // A component stops influencing the ray: subtract its terms.
            for (int j = 0; j < 5; j++) {
                coefficients[j] -= componentTerms.get(terms + j);
            }
            if (--inFlag == 0) {
                // No component affects the ray until the next start.
                continue;
            }
        }
        int rootCount;
        if (!sturm) {
            rootCount = QuarticSolver.solve(coefficients, roots, minimumDistance, SOLVER_EPSILON);
        } else if (Math.abs(coefficients[0]) < COEFFICIENT_LIMIT) {
            if (Math.abs(coefficients[1]) < COEFFICIENT_LIMIT) {
                rootCount = QuadraticSolver.solve(coefficients, 2, roots);
            } else {
                PolynomialSolver cubicSolver = new PolynomialSolver(3, coefficients, 1);
                rootCount = cubicSolver.solve(roots, minimumDistance, SOLVER_EPSILON);
            }
        } else {
            PolynomialSolver quarticSolver = new PolynomialSolver(4, coefficients);
            rootCount = quarticSolver.solve(roots, minimumDistance, SOLVER_EPSILON);
        }
        for (int j = 0; j < rootCount; j++) {
            final double dist = roots[j];
            // Keep roots inside the interval of the active components whose
            // gradient shows a real crossing rather than numerical noise.
            if (dist >= intervals.get((int)i).getBound() && dist <= intervals.get((int)(i + 1)).getBound()) {
                final Vector3Dd point = d.multiply(dist).add(p);
                final Vector3Dd n = gradient(point);
                if (n.dotProduct(n) >= GRAZING_LIMIT) {
                    distances.add(dist);
                }
            }
        }
    }
}

    @Override
    public Geometry copyGeometry() { return new BlobSolid(this); }

    @Override
    public double evaluateField(Vector3Dd point) { return threshold - evaluateDensity(point); }

    @Override
    public Vector3Dd evaluateFieldGradient(Vector3Dd point) { return gradient(point); }

    @Override
    public FloatingInterval encloseField(FloatingInterval x, FloatingInterval y, FloatingInterval z) {
    if (!x.isValid() || !y.isValid() || !z.isValid() || !Double.isFinite(threshold))
        return new FloatingInterval();
    FloatingInterval result = FloatingInterval.exact(threshold);
    for (long i = 0; i < components.size(); i++) {
        if (components.get((int)(i)).isDegenerate()) return new FloatingInterval();
        final FloatingInterval e = clampedFalloff(i, x, y, z);
        result = result.subtract(FloatingInterval.exact(components.get((int)(i)).getStrength()).multiply(e.square()));
    }
    return result;
}

    @Override
    public boolean encloseFieldGradient(FloatingInterval x, FloatingInterval y, FloatingInterval z, FloatingInterval[] gx, FloatingInterval[] gy, FloatingInterval[] gz) {
    if (!x.isValid() || !y.isValid() || !z.isValid()) return false;
    gx[0] = gy[0] = gz[0] = FloatingInterval.exact(0.0);
    for (long i = 0; i < components.size(); i++) {
        final BlobComponent component = components.get((int)(i));
        if (component.isDegenerate()) return false;
        // grad (threshold - s (1 - q)^2) = 4 s (1 - q) (p - c) / radius^2
        final FloatingInterval scale = FloatingInterval.exact(4.0 * component.getStrength()).multiply(
            clampedFalloff(i, x, y, z)).divide(FloatingInterval.exact(component.getRadius()).square());
        gx[0] = gx[0].add(scale.multiply(x.subtract(FloatingInterval.exact(component.getCenter().x()))));
        gy[0] = gy[0].add(scale.multiply(y.subtract(FloatingInterval.exact(component.getCenter().y()))));
        gz[0] = gz[0].add(scale.multiply(z.subtract(FloatingInterval.exact(component.getCenter().z()))));
    }
    return gx[0].isValid() && gy[0].isValid() && gz[0].isValid();
}

    @Override
    public boolean findRayRoots(Vector3Dd origin, Vector3Dd unitDirection, double maximumDistance, ArrayList<Double> distances) {
    for (long i = 0; i < components.size(); i++)
        if (components.get((int)(i)).isDegenerate()) return false;
    if (!Double.isFinite(threshold)) return false;
    intersect(origin, unitDirection, 0.0, 0.0, distances);
    return true;
}

    @Override
    public void getBounds(GeometryBounds result) {
    if (!Double.isFinite(threshold)) { result.setIndeterminate(); return; }
    for (long i = 0; i < components.size(); i++)
        if (components.get((int)(i)).isDegenerate()) { result.setIndeterminate(); return; }
    // Density above a nonnegative threshold needs a positive component.
    if (threshold < 0) { result.setUnbounded(); return; }
    boolean any = false;
    Vector3Dd minimum = new Vector3Dd(), maximum = new Vector3Dd();
    for (long i = 0; i < components.size(); i++) {
        final BlobComponent component = components.get((int)(i));
        if (component.getStrength() <= 0) continue;
        final FloatingInterval r = FloatingInterval.exact(component.getRadius());
        final double[] c = {component.getCenter().x(), component.getCenter().y(), component.getCenter().z()};
        double[] low = new double[3], high = new double[3];
        for (int k = 0; k < 3; k++) {
            low[k] = FloatingInterval.exact(c[k]).subtract(r).getLower();
            high[k] = FloatingInterval.exact(c[k]).add(r).getUpper();
        }
        final Vector3Dd a = new Vector3Dd(low[0], low[1], low[2]), b = new Vector3Dd(high[0], high[1], high[2]);
        if (!any) { minimum = a; maximum = b; any = true; continue; }
        minimum = new Vector3Dd(Math.min(minimum.x(), a.x()), Math.min(minimum.y(), a.y()),
            Math.min(minimum.z(), a.z()));
        maximum = new Vector3Dd(Math.max(maximum.x(), b.x()), Math.max(maximum.y(), b.y()),
            Math.max(maximum.z(), b.z()));
    }
    if (!any) { result.setEmpty(); return; }
    if (!result.setFinite(minimum, maximum)) result.setIndeterminate();
}
}
