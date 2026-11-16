//= References:                                                             =
//= [WALD2007] Wald, Ingo. "On fast Construction of SAH-based Bounding      =
//=     Volume Hierarchies". Proceedings of the 2007 IEEE Symposium on      =
//=     Interactive Ray Tracing, 2007, pp. 33-40.                           =

package vsdk.toolkit.environment.geometry.boundingVolumeHierarchy;

/**
Axis aligned box of the build. The default box is the "negative box"
[+inf, -inf] of [WALD2007].3.4: enclosing it in another box leaves that box
unchanged, so bins and partitions grow without emptiness checks.
*/
class _BinnedSahBox {
    final double[] lo = new double[3];
    final double[] hi = new double[3];

    _BinnedSahBox()
    {
        for ( int a = 0; a < 3; a++ ) {
            lo[a] = 1e30;
            hi[a] = -1e30;
        }
    }

    boolean isEmpty()
    {
        return lo[0] > hi[0] || lo[1] > hi[1] || lo[2] > hi[2];
    }

    void enclose(_BinnedSahBox other)
    {
        for ( int a = 0; a < 3; a++ ) {
            lo[a] = other.lo[a] < lo[a] ? other.lo[a] : lo[a];
            hi[a] = other.hi[a] > hi[a] ? other.hi[a] : hi[a];
        }
    }

    void enclosePoint(double[] point)
    {
        for ( int a = 0; a < 3; a++ ) {
            lo[a] = point[a] < lo[a] ? point[a] : lo[a];
            hi[a] = point[a] > hi[a] ? point[a] : hi[a];
        }
    }

    double centroid(int axis)
    {
        return 0.5 * (lo[axis] + hi[axis]);
    }

    double extent(int axis)
    {
        return hi[axis] - lo[axis];
    }

    /** Half the surface area: the SAH cost of [WALD2007].3.1 drops the
    constant factors and only compares ratios */
    double halfArea()
    {
        if ( isEmpty() ) {
            return 0.0;
        }
        final double dx = hi[0] - lo[0];
        final double dy = hi[1] - lo[1];
        final double dz = hi[2] - lo[2];
        return dx * dy + dy * dz + dz * dx;
    }
}
