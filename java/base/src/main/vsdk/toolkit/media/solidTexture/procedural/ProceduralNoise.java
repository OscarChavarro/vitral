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

        m = (short)(hash3d(l.lowerCellX, l.lowerCellY, l.lowerCellZ) & 0xff);
        sum = incrSum(m, l.lowerWeightX * l.lowerWeightY * l.lowerWeightZ, l.x - l.lowerCellX, l.y - l.lowerCellY, l.z - l.lowerCellZ);
        m = (short)(hash3d(l.upperCellX, l.lowerCellY, l.lowerCellZ) & 0xff);
        sum += incrSum(m, l.upperWeightX * l.lowerWeightY * l.lowerWeightZ, l.x - l.upperCellX, l.y - l.lowerCellY, l.z - l.lowerCellZ);
        m = (short)(hash3d(l.lowerCellX, l.upperCellY, l.lowerCellZ) & 0xff);
        sum += incrSum(m, l.lowerWeightX * l.upperWeightY * l.lowerWeightZ, l.x - l.lowerCellX, l.y - l.upperCellY, l.z - l.lowerCellZ);
        m = (short)(hash3d(l.upperCellX, l.upperCellY, l.lowerCellZ) & 0xff);
        sum += incrSum(m, l.upperWeightX * l.upperWeightY * l.lowerWeightZ, l.x - l.upperCellX, l.y - l.upperCellY, l.z - l.lowerCellZ);
        m = (short)(hash3d(l.lowerCellX, l.lowerCellY, l.upperCellZ) & 0xff);
        sum += incrSum(m, l.lowerWeightX * l.lowerWeightY * l.upperWeightZ, l.x - l.lowerCellX, l.y - l.lowerCellY, l.z - l.upperCellZ);
        m = (short)(hash3d(l.upperCellX, l.lowerCellY, l.upperCellZ) & 0xff);
        sum += incrSum(m, l.upperWeightX * l.lowerWeightY * l.upperWeightZ, l.x - l.upperCellX, l.y - l.lowerCellY, l.z - l.upperCellZ);
        m = (short)(hash3d(l.lowerCellX, l.upperCellY, l.upperCellZ) & 0xff);
        sum += incrSum(m, l.lowerWeightX * l.upperWeightY * l.upperWeightZ, l.x - l.lowerCellX, l.y - l.upperCellY, l.z - l.upperCellZ);
        m = (short)(hash3d(l.upperCellX, l.upperCellY, l.upperCellZ) & 0xff);
        sum += incrSum(m, l.upperWeightX * l.upperWeightY * l.upperWeightZ, l.x - l.upperCellX, l.y - l.upperCellY, l.z - l.upperCellZ);

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
        double px = l.x - l.lowerCellX;
        double py = l.y - l.lowerCellY;
        double pz = l.z - l.lowerCellZ;
        double s = l.lowerWeightX * l.lowerWeightY * l.lowerWeightZ;
        short m = (short)(hash3d(l.lowerCellX, l.lowerCellY, l.lowerCellZ) & 0xff);
        double rx = incrSum(m, s, px, py, pz);
        double ry = incrSum(m + 4, s, px, py, pz);
        double rz = incrSum(m + 8, s, px, py, pz);

        m = (short)(hash3d(l.upperCellX, l.lowerCellY, l.lowerCellZ) & 0xff);
        px = l.x - l.upperCellX;
        s = l.upperWeightX * l.lowerWeightY * l.lowerWeightZ;
        rx += incrSum(m, s, px, py, pz);
        ry += incrSum(m + 4, s, px, py, pz);
        rz += incrSum(m + 8, s, px, py, pz);

        m = (short)(hash3d(l.upperCellX, l.upperCellY, l.lowerCellZ) & 0xff);
        py = l.y - l.upperCellY;
        s = l.upperWeightX * l.upperWeightY * l.lowerWeightZ;
        rx += incrSum(m, s, px, py, pz);
        ry += incrSum(m + 4, s, px, py, pz);
        rz += incrSum(m + 8, s, px, py, pz);

        m = (short)(hash3d(l.lowerCellX, l.upperCellY, l.lowerCellZ) & 0xff);
        px = l.x - l.lowerCellX;
        s = l.lowerWeightX * l.upperWeightY * l.lowerWeightZ;
        rx += incrSum(m, s, px, py, pz);
        ry += incrSum(m + 4, s, px, py, pz);
        rz += incrSum(m + 8, s, px, py, pz);

        m = (short)(hash3d(l.lowerCellX, l.upperCellY, l.upperCellZ) & 0xff);
        pz = l.z - l.upperCellZ;
        s = l.lowerWeightX * l.upperWeightY * l.upperWeightZ;
        rx += incrSum(m, s, px, py, pz);
        ry += incrSum(m + 4, s, px, py, pz);
        rz += incrSum(m + 8, s, px, py, pz);

        m = (short)(hash3d(l.upperCellX, l.upperCellY, l.upperCellZ) & 0xff);
        px = l.x - l.upperCellX;
        s = l.upperWeightX * l.upperWeightY * l.upperWeightZ;
        rx += incrSum(m, s, px, py, pz);
        ry += incrSum(m + 4, s, px, py, pz);
        rz += incrSum(m + 8, s, px, py, pz);

        m = (short)(hash3d(l.upperCellX, l.lowerCellY, l.upperCellZ) & 0xff);
        py = l.y - l.lowerCellY;
        s = l.upperWeightX * l.lowerWeightY * l.upperWeightZ;
        rx += incrSum(m, s, px, py, pz);
        ry += incrSum(m + 4, s, px, py, pz);
        rz += incrSum(m + 8, s, px, py, pz);

        m = (short)(hash3d(l.lowerCellX, l.lowerCellY, l.upperCellZ) & 0xff);
        px = l.x - l.lowerCellX;
        s = l.lowerWeightX * l.lowerWeightY * l.upperWeightZ;
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
        l.lowerCellX = (long)l.x;
        l.lowerCellY = (long)l.y;
        l.lowerCellZ = (long)l.z;
        l.upperCellX = l.lowerCellX + 1;
        l.upperCellY = l.lowerCellY + 1;
        l.upperCellZ = l.lowerCellZ + 1;
        l.upperWeightX = sCurve(l.x - l.lowerCellX);
        l.upperWeightY = sCurve(l.y - l.lowerCellY);
        l.upperWeightZ = sCurve(l.z - l.lowerCellZ);
        l.lowerWeightX = 1.0 - l.upperWeightX;
        l.lowerWeightY = 1.0 - l.upperWeightY;
        l.lowerWeightZ = 1.0 - l.upperWeightZ;
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
        long lowerCellX;
        long lowerCellY;
        long lowerCellZ;
        long upperCellX;
        long upperCellY;
        long upperCellZ;
        double upperWeightX;
        double upperWeightY;
        double upperWeightZ;
        double lowerWeightX;
        double lowerWeightY;
        double lowerWeightZ;
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
