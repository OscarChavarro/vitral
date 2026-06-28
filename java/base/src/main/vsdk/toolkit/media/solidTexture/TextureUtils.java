package vsdk.toolkit.media.solidTexture;

import vsdk.toolkit.common.color.ColorRgba;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.common.statistics.SolidTextureStatistics;
import vsdk.toolkit.media.RGBAColorPalette;
import vsdk.toolkit.media.solidTexture.procedural.ProceduralNoise;

public final class TextureUtils
{
    private static final long WAVE_RANDOM_MASK = 0x7fffl;
    private static final float WAVE_RANDOM_DIVISOR = (float)WAVE_RANDOM_MASK;

    private ProceduralNoise proceduralNoise;
    private double[] frequencyInstance;
    private Vector3Dd[] waveSourcesInstance;

    public TextureUtils()
    {
        proceduralNoise = null;
        frequencyInstance = new double[0];
        waveSourcesInstance = new Vector3Dd[0];
    }

    public TextureUtils(SolidTextureStatistics stats)
    {
        this();
        proceduralNoise = new ProceduralNoise(stats);
    }

    public void initialize(SolidTextureStatistics stats)
    {
        proceduralNoise = new ProceduralNoise(stats);
    }

    public ProceduralNoise getProceduralNoise()
    {
        if ( proceduralNoise == null ) {
            proceduralNoise = new ProceduralNoise();
        }
        return proceduralNoise;
    }

    public static double floorInline(double x)
    {
        return x >= 0.0 ? Math.floor(x) : 0.0 - Math.floor(0.0 - x) - 1.0;
    }

    public static double fabsInline(double x)
    {
        return x < 0.0 ? 0.0 - x : x;
    }

    public double[] waveFrequency()
    {
        return frequencyInstance;
    }

    public Vector3Dd[] waveSources()
    {
        return waveSourcesInstance;
    }

    public static void computeColor(ColorRgba color, RGBAColorPalette colorMap, double value)
    {
        color.set(colorMap.evalLinear(value));
    }

    public void initializeNoise(int numberOfWaves)
    {
        ProceduralNoise noise = getProceduralNoise();
        noise.initialize();
        CRandom random = new CRandom(0L);
        for ( int i = 0; i < 4096; i++ ) {
            random.next();
        }
        frequencyInstance = new double[numberOfWaves];
        waveSourcesInstance = new Vector3Dd[numberOfWaves];

        for ( int i = 0; i < numberOfWaves; i++ ) {
            Vector3Dd point = noise.differentialNoise(i, 0.0, 0.0).normalized();
            waveSourcesInstance[i] = point;
            frequencyInstance[i] = (random.next() & WAVE_RANDOM_MASK) / WAVE_RANDOM_DIVISOR + 0.01;
        }
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
