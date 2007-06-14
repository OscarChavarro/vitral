//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - February 13 2006 - Oscar Chavarro: Original base version              =
//===========================================================================

package vsdk.toolkit.common;

import java.text.DecimalFormat;
import java.text.FieldPosition;

import vsdk.toolkit.common.Vector3D;

/**
!\mainpage VSDK Toolkit documentation.

\section intro Introduction: the VSDK Architecture and design specification

This is an automatically generated page from the VSDK source code. It is not
editable.  You can browse the literate programming style documentation, but
remember to take a look first at the document describing the VSDK toolkit:<P>

<A HREF="../ArchitectureAndDesignSpecification.html">Architecture and design specification of the VSDK toolkit</A>.

\section model The VSDK toolkit data model

At some point in the future, the data model implemented on the toolkit will be 
described in an external document, and that document will be linked here. At
the moment such a detailed description is not available.

\section next What to do next?

The recomended steps are: 1. Install the VSDK toolkit. 2. Take a look at the
samples in the testsuite directory. 3. Have this API documentation available
and look at specific method description for help. 4. Have in hand a good
computer graphics textbook.

*/

public class VSDK
{
    public static final double EPSILON = 1e-6;

    public static final int WARNING = 1;
    public static final int ERROR = 2;
    public static final int DEBUG = 3;
    public static final int VERBOSE = 4;

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

    /** Converts integers in the domain [-128, 127] to integers in the range
    [0, 256] */
    public static int signedByte2unsignedInteger(byte in) {
        int a;

        a = (int)in;
        if ( a < 0 ) a += 256;
        return a;

    }

    public static byte unsigned8BitInteger2signedByte(int in) {
        if ( in > 255 ) in = 255;
        if ( in < 0 ) in = 0;
        if ( in > 127 ) in -= 256;
        return (byte)in;
    }

    public static void reportMessage(Object o, int level, String method, String message)
    {
        System.err.println("===========================================================================");
        System.err.println("= VSDK Exception report                                                   =");
        if ( o != null ) {
            System.err.println(" - An exception has been thrown in the \"" + o.getClass().getName() + "\" class");
    }
    else {
            System.err.println(" - An exception has been thrown from a static context");
    }
        System.err.println(" - Exception located at method " + method);
        System.err.println(" - Exception message:\n" + message);
        System.err.println("===========================================================================");
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
