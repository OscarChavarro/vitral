package vsdk.toolkit.media.solidTexture.procedural;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.statistics.SolidTextureStatistics;
import vsdk.toolkit.numericalAnalysis.lookUpTables.LookUpTableChecksum16;
import vsdk.toolkit.numericalAnalysis.lookUpTables.LookUpTableSine;

public final class ProceduralNoise
{
    private static final int MIN_X = -10000;
    private static final int MIN_Y = MIN_X;
    private static final int MIN_Z = MIN_X;
    private static final int MAXSIZE = 267;
    private static final double REAL_SCALE = 2.0 / 65535.0;

    private short[] permutationTable;
    private double[] rTable;
    private final LookUpTableSine sineLookUpTable;
    private final LookUpTableChecksum16 checksumLookUpTable;
    private final SolidTextureStatistics solidTextureStatistics;

    public ProceduralNoise()
    {
        this(null);
    }

    public ProceduralNoise(SolidTextureStatistics solidTextureStatistics)
    {
        permutationTable = null;
        rTable = null;
        sineLookUpTable = new LookUpTableSine(11);
        checksumLookUpTable = new LookUpTableChecksum16();
        this.solidTextureStatistics = solidTextureStatistics;
    }

    public void initialize()
    {
        initRTable();
    }

    public double sCurve(double a)
    {
        return a * a * (3.0 - 2.0 * a);
    }

    public double cycloidal(double value)
    {
        if ( value >= 0.0 ) {
            return sineLookUpTable.eval(value - Math.floor(value));
        }
        return 0.0 - sineLookUpTable.eval(0.0 - (value + Math.floor(0.0 - value)));
    }

    public double triangleWave(double value)
    {
        double offset;
        if ( value >= 0.0 ) {
            offset = value - Math.floor(value);
        }
        else {
            double temp1 = -1.0 - Math.floor(Math.abs(value));
            offset = value - temp1;
        }
        if ( offset >= 0.5 ) {
            return 2.0 * (1.0 - offset);
        }
        return 2.0 * offset;
    }

    public double noise(double x, double y, double z)
    {
        ensureInitialized();
        if ( solidTextureStatistics != null ) {
            solidTextureStatistics.callsToNoise++;
        }

        Lattice l = setupLattice(x, y, z);
        double sum;
        short m;

        m = (short)(hash3d(l.ix, l.iy, l.iz) & 0xff);
        sum = incrSum(m, l.tx * l.ty * l.tz, l.x - l.ix, l.y - l.iy, l.z - l.iz);
        m = (short)(hash3d(l.jx, l.iy, l.iz) & 0xff);
        sum += incrSum(m, l.sx * l.ty * l.tz, l.x - l.jx, l.y - l.iy, l.z - l.iz);
        m = (short)(hash3d(l.ix, l.jy, l.iz) & 0xff);
        sum += incrSum(m, l.tx * l.sy * l.tz, l.x - l.ix, l.y - l.jy, l.z - l.iz);
        m = (short)(hash3d(l.jx, l.jy, l.iz) & 0xff);
        sum += incrSum(m, l.sx * l.sy * l.tz, l.x - l.jx, l.y - l.jy, l.z - l.iz);
        m = (short)(hash3d(l.ix, l.iy, l.jz) & 0xff);
        sum += incrSum(m, l.tx * l.ty * l.sz, l.x - l.ix, l.y - l.iy, l.z - l.jz);
        m = (short)(hash3d(l.jx, l.iy, l.jz) & 0xff);
        sum += incrSum(m, l.sx * l.ty * l.sz, l.x - l.jx, l.y - l.iy, l.z - l.jz);
        m = (short)(hash3d(l.ix, l.jy, l.jz) & 0xff);
        sum += incrSum(m, l.tx * l.sy * l.sz, l.x - l.ix, l.y - l.jy, l.z - l.jz);
        m = (short)(hash3d(l.jx, l.jy, l.jz) & 0xff);
        sum += incrSum(m, l.sx * l.sy * l.sz, l.x - l.jx, l.y - l.jy, l.z - l.jz);

        sum += 0.5;
        if ( sum < 0.0 ) sum = 0.0;
        if ( sum > 1.0 ) sum = 1.0;
        return sum;
    }

    public Vector3Dd differentialNoise(double x, double y, double z)
    {
        ensureInitialized();
        if ( solidTextureStatistics != null ) {
            solidTextureStatistics.callsToDNoise++;
        }

        Lattice l = setupLattice(x, y, z);
        double px = l.x - l.ix;
        double py = l.y - l.iy;
        double pz = l.z - l.iz;
        double s = l.tx * l.ty * l.tz;
        short m = (short)(hash3d(l.ix, l.iy, l.iz) & 0xff);
        double rx = incrSum(m, s, px, py, pz);
        double ry = incrSum(m + 4, s, px, py, pz);
        double rz = incrSum(m + 8, s, px, py, pz);

        m = (short)(hash3d(l.jx, l.iy, l.iz) & 0xff);
        px = l.x - l.jx;
        s = l.sx * l.ty * l.tz;
        rx += incrSum(m, s, px, py, pz);
        ry += incrSum(m + 4, s, px, py, pz);
        rz += incrSum(m + 8, s, px, py, pz);

        m = (short)(hash3d(l.jx, l.jy, l.iz) & 0xff);
        py = l.y - l.jy;
        s = l.sx * l.sy * l.tz;
        rx += incrSum(m, s, px, py, pz);
        ry += incrSum(m + 4, s, px, py, pz);
        rz += incrSum(m + 8, s, px, py, pz);

        m = (short)(hash3d(l.ix, l.jy, l.iz) & 0xff);
        px = l.x - l.ix;
        s = l.tx * l.sy * l.tz;
        rx += incrSum(m, s, px, py, pz);
        ry += incrSum(m + 4, s, px, py, pz);
        rz += incrSum(m + 8, s, px, py, pz);

        m = (short)(hash3d(l.ix, l.jy, l.jz) & 0xff);
        pz = l.z - l.jz;
        s = l.tx * l.sy * l.sz;
        rx += incrSum(m, s, px, py, pz);
        ry += incrSum(m + 4, s, px, py, pz);
        rz += incrSum(m + 8, s, px, py, pz);

        m = (short)(hash3d(l.jx, l.jy, l.jz) & 0xff);
        px = l.x - l.jx;
        s = l.sx * l.sy * l.sz;
        rx += incrSum(m, s, px, py, pz);
        ry += incrSum(m + 4, s, px, py, pz);
        rz += incrSum(m + 8, s, px, py, pz);

        m = (short)(hash3d(l.jx, l.iy, l.jz) & 0xff);
        py = l.y - l.iy;
        s = l.sx * l.ty * l.sz;
        rx += incrSum(m, s, px, py, pz);
        ry += incrSum(m + 4, s, px, py, pz);
        rz += incrSum(m + 8, s, px, py, pz);

        m = (short)(hash3d(l.ix, l.iy, l.jz) & 0xff);
        px = l.x - l.ix;
        s = l.tx * l.ty * l.sz;
        rx += incrSum(m, s, px, py, pz);
        ry += incrSum(m + 4, s, px, py, pz);
        rz += incrSum(m + 8, s, px, py, pz);

        return new Vector3Dd(rx, ry, rz);
    }

    public void differentialNoise(Vector3Dd[] result, double x, double y, double z)
    {
        result[0] = differentialNoise(x, y, z);
    }

    public double turbulence(double x, double y, double z, int octaves)
    {
        double t = 0.0;
        for ( int i = 0; i < octaves; i++ ) {
            double scale = Math.pow(0.5, i);
            t += fabsInline(noise(x / scale, y / scale, z / scale)) * scale;
        }
        return t;
    }

    public Vector3Dd differentialTurbulence(double x, double y, double z, int octaves)
    {
        double rx = 0.0;
        double ry = 0.0;
        double rz = 0.0;
        for ( int i = 0; i < octaves; i++ ) {
            double scale = Math.pow(0.5, i);
            Vector3Dd value = differentialNoise(x / scale, y / scale, z / scale);
            rx += value.x() * scale;
            ry += value.y() * scale;
            rz += value.z() * scale;
        }
        return new Vector3Dd(rx, ry, rz);
    }

    public void differentialTurbulence(Vector3Dd[] result, double x, double y, double z,
                                       int octaves)
    {
        result[0] = differentialTurbulence(x, y, z, octaves);
    }

    public short[] hashTable()
    {
        return permutationTable;
    }

    public LookUpTableChecksum16 checksumTable()
    {
        return checksumLookUpTable;
    }

    private void ensureInitialized()
    {
        if ( permutationTable == null || rTable == null ) {
            initialize();
        }
    }

    private void initTextureTable()
    {
        CRandom random = new CRandom(0L);
        permutationTable = new short[4096];
        for ( int i = 0; i < 4096; i++ ) {
            permutationTable[i] = (short)i;
        }
        for ( int i = 4095; i >= 0; i-- ) {
            int j = random.next() % 4096;
            short temp = permutationTable[i];
            permutationTable[i] = permutationTable[j];
            permutationTable[j] = temp;
        }
    }

    private void initRTable()
    {
        initTextureTable();
        rTable = new double[MAXSIZE];
        for ( int i = 0; i < MAXSIZE; i++ ) {
            rTable[i] = (checksumVector(new Vector3Dd(i, i, i)) & 0xffff) * REAL_SCALE - 1.0;
        }
    }

    private int checksumVector(Vector3Dd v)
    {
        Vector3Dd scaled = new Vector3Dd(v.x() * 0.12345, v.y() * 0.12345, v.z() * 0.12345);
        ByteBuffer buffer = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(scaled.x());
        buffer.putDouble(scaled.y());
        buffer.putDouble(scaled.z());
        return checksumLookUpTable.eval(buffer.array(), 24);
    }

    private short hash3d(long a, long b, long c)
    {
        int i0 = (int)(a & 0xfffL);
        int i1 = (permutationTable[i0] ^ (int)(b & 0xfffL)) & 0xfff;
        int i2 = (permutationTable[i1] ^ (int)(c & 0xfffL)) & 0xfff;
        return permutationTable[i2];
    }

    private double incrSum(int m, double s, double x, double y, double z)
    {
        return s * (rTable[m] * 0.5 + rTable[m + 1] * x + rTable[m + 2] * y + rTable[m + 3] * z);
    }

    private Lattice setupLattice(double x, double y, double z)
    {
        Lattice l = new Lattice();
        l.x = x - MIN_X;
        l.y = y - MIN_Y;
        l.z = z - MIN_Z;
        l.ix = (long)l.x;
        l.iy = (long)l.y;
        l.iz = (long)l.z;
        l.jx = l.ix + 1;
        l.jy = l.iy + 1;
        l.jz = l.iz + 1;
        l.sx = sCurve(l.x - l.ix);
        l.sy = sCurve(l.y - l.iy);
        l.sz = sCurve(l.z - l.iz);
        l.tx = 1.0 - l.sx;
        l.ty = 1.0 - l.sy;
        l.tz = 1.0 - l.sz;
        return l;
    }

    private static double fabsInline(double x)
    {
        return x < 0.0 ? 0.0 - x : x;
    }

    private static final class Lattice
    {
        double x;
        double y;
        double z;
        long ix;
        long iy;
        long iz;
        long jx;
        long jy;
        long jz;
        double sx;
        double sy;
        double sz;
        double tx;
        double ty;
        double tz;
    }

    private static final class CRandom
    {
        private long state;

        CRandom(long seed)
        {
            state = seed & 0x7fffffffL;
        }

        int next()
        {
            state = (state * 1103515245L + 12345L) & 0x7fffffffL;
            return (int)((state >>> 16) & 0x7fffL);
        }
    }
}
