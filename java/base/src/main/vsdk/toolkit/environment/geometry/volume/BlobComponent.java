package vsdk.toolkit.environment.geometry.volume;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/**
Spherical field component of a BlobSolid: strength * (1 - d^2/radius^2)^2
at distance d < radius from the center, and zero beyond. The density is
evaluated as c0*d^4 + c1*d^2 + c2 with the PovRay coefficients.
*/
public class BlobComponent {
    private final Vector3Dd center;

    private double radius;

    private double strength;

    private double radiusSquared;

    private double[] coefficients;

    public BlobComponent() {
        this(new Vector3Dd(0, 0, 0), 1.0, 1.0);
}

    public BlobComponent(Vector3Dd c, double r, double s) {
        this.center = c;
        this.radius = r;
        this.strength = s;
        this.radiusSquared = r*r;
        this.coefficients = new double[3];
    coefficients[2] = s;
    coefficients[1] = -(2.0 * s) / radiusSquared;
    coefficients[0] = s / (radiusSquared * radiusSquared);
}

    public Vector3Dd getCenter() { return center; }

    public double getRadius() { return radius; }

    public double getStrength() { return strength; }

    public double getRadiusSquared() { return radiusSquared; }

    // c0, c1 and c2 of the density polynomial in d^2.
    public double[] getCoefficients() { return coefficients; }

    // PovRay rejects radii or strengths below 1e-5 in magnitude.
    public boolean isDegenerate() {
    return !Double.isFinite(radius) || !Double.isFinite(strength) ||
        !Double.isFinite(center.x()) || !Double.isFinite(center.y()) ||
        !Double.isFinite(center.z()) ||
        Math.abs(strength) < 1.0e-5 || radius < 1.0e-5;
}
}
