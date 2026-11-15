package vsdk.toolkit.render.raytracing;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

public class SnellRefraction {
    /**
    Writes the transmitted direction for relative index eta = n1 / n2 and
    returns true, or returns false when the angle exceeds the critical
    angle (total internal reflection) and leaves `outDirection` untouched.
    `outDirection` is a one-element output holder.
    */
    public static boolean refract(Vector3Dd towardViewer, Vector3Dd normal, double eta, Vector3Dd[] outDirection) {
    double cosI = towardViewer.dotProduct(normal);
    double k = 1.0 - eta*eta*(1.0 - cosI*cosI);
    if ( k < 0.0 ) {
        return false;
    }
    double c = eta*cosI - Math.sqrt(k);
    outDirection[0] = (new Vector3Dd(
        -eta*towardViewer.x() + c*normal.x(),
        -eta*towardViewer.y() + c*normal.y(),
        -eta*towardViewer.z() + c*normal.z()));
    return true;
}

    /** Mirror direction of `towardViewer` about `normal`. */
    public static Vector3Dd reflect(Vector3Dd towardViewer, Vector3Dd normal) {
    double twoCos = 2.0*towardViewer.dotProduct(normal);
    return new Vector3Dd(
        twoCos*normal.x() - towardViewer.x(),
        twoCos*normal.y() - towardViewer.y(),
        twoCos*normal.z() - towardViewer.z());
}
}

