//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - February 13 2006 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.common;

import java.text.DecimalFormat;
import java.text.FieldPosition;

import vsdk.toolkit.common.Vector3D;

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

    /**
    Given a double number, it formats it to print in a given precision
    */
    public static String formatDouble(double a)
    {
        DecimalFormat f = new DecimalFormat("0.##");

        return f.format(a, new StringBuffer(""), new FieldPosition(0)).toString();
    }

    public static int signedByte2unsignedInteger(byte in) {
        int a;

        a = (int)in;
        if ( a < 0 ) a += 256;
        return a;

    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
