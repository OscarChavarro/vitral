//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//= - November 15 2005 - Oscar Chavarro: quality check                      =
//===========================================================================

package vitral.toolkits.common;

import java.text.DecimalFormat;
import java.text.FieldPosition;

public class Vector3D {
    /// Yes, they are public due to efficiency issues
    public double x, y, z;

    /**
     * The default Vector3D value is the zero value
     */
    public Vector3D() {
        x = 0;
        y = 0;
        z = 0;
    }

    /**
     *
     * @param x double
     * @param y double
     * @param z double
     */
    public Vector3D(double x, double y, double z) {
        this.x = x; this.y = y; this.z = z;
    }

    public Vector3D(Vector3D B) {
        this.x = B.x; this.y = B.y; this.z = B.z;
    }

    public final Vector3D multiply(double a) {
        return new Vector3D(a * x, a * y, a * z);
    }

    /**
     *
     * @param B Vector3D
     * @return Vector3D
     */
    public final Vector3D crossProduct(Vector3D B) {
        return new Vector3D(y*B.z - z*B.y, z*B.x - x*B.z, x*B.y - y*B.x);
    }

    /**
     *
     * @param B Vector3D
     * @return double
     */
    public final double dotProduct(Vector3D B) {
        return (x*B.x + y*B.y + z*B.z);
    }

    /**
     *
     */
    public final void normalize() {
        double t = x*x + y*y + z*z;
        if (t != 0 && t != 1) t = (double) (1 / Math.sqrt(t));
        x *= t;
        y *= t;
        z *= t;
    }

    /**
     *
     * @return double
     */
    public final double length() {
        return (double)Math.sqrt(x*x + y*y + z*z);
    }

    public final Vector3D add(Vector3D b)
    {
        return new Vector3D(x + b.x, y + b.y, z + b.z);
    }

    public final Vector3D substract(Vector3D b)
    {
        return new Vector3D(x - b.x, y - b.y, z - b.z);
    }

    /**
    Given a double number, it formats it to print in a given precision
    */
    private String f(double a)
    {
        DecimalFormat f = new DecimalFormat("0.##");

        return f.format(a, new StringBuffer(""), new FieldPosition(0)).toString();
    }

    public String toString()
    {
        String msg;

        msg = "<" + f(x) + ", " + f(y) + ", " + f(z) + ">";

        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
