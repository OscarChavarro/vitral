//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - February 13 2006 - Oscar Chavarro: Original base version              =
//===========================================================================

package vitral.toolkits.common;

import vitral.toolkits.common.Vector3D;

public class VSDK
{
    public static final double EPSILON = Double.MIN_VALUE*10;

    public static boolean equals(double a, double b)
    {
        if ( Math.abs(a - b) < EPSILON ) {
            return true;
    }
        return false;
    }

    public static double vectorDistance(Vector3D a, Vector3D b)
    {
        return Math.sqrt((a.x-b.x)*(a.x-b.x) + 
                         (a.y-b.y)*(a.y-b.y) +
                         (a.z-b.z)*(a.z-b.z));
    }

    public static double square(double a)
    {
        return a*a;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
