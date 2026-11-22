package vsdk.toolkit.environment.geometry.boundingVolumeHierarchy;

/**
C++ declares it in `TriangleMeshAccelerationMethod.h` and defines it in
`TriangleAccelerationStructure.cpp`.
*/
public final class TriangleMeshAccelerationMethodUtil {
    public static final int COUNT = 3;

    private TriangleMeshAccelerationMethodUtil()
    {
    }

    public static TriangleMeshAccelerationMethod fromCode(int code)
    {
        if ( code == 1 ) {
            return TriangleMeshAccelerationMethod.WALD2007_BINNED_SAH;
        }
        if ( code == 2 ) {
            return TriangleMeshAccelerationMethod.VOXEL_GRID;
        }
        return TriangleMeshAccelerationMethod.BRUTE_FORCE;
    }

    /** Short name used by command lines: "bruteforce", "wald2007", "voxel" */
    public static String name(TriangleMeshAccelerationMethod value)
    {
        switch ( value ) {
          case BRUTE_FORCE: return "bruteforce";
          case WALD2007_BINNED_SAH: return "wald2007";
          case VOXEL_GRID: return "voxel";
        }
        return "unknown";
    }

    /**
    Inverse of `name`; false when the name is unknown.
    @param text name to parse
    @param outValue C++ reference output parameter: `outValue[0]` receives
    the method
    @return true when the name is known
    */
    public static boolean fromName(String text, TriangleMeshAccelerationMethod[] outValue)
    {
        if ( text == null ) {
            return false;
        }
        for ( int code = 0; code < COUNT; code++ ) {
            final TriangleMeshAccelerationMethod candidate = fromCode(code);
            if ( text.equals(name(candidate)) ) {
                outValue[0] = candidate;
                return true;
            }
        }
        return false;
    }
}
