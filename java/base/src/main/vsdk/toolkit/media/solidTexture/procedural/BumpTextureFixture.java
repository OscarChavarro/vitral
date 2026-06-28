package vsdk.toolkit.media.solidTexture.procedural;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.media.solidTexture.TextureUtils;

public final class BumpTextureFixture
{
    private final ProceduralNoise proceduralNoise;
    private final TextureUtils textureUtils;

    public BumpTextureFixture(ProceduralNoise proceduralNoise, TextureUtils textureUtils)
    {
        this.proceduralNoise = proceduralNoise;
        this.textureUtils = textureUtils;
    }

    public Vector3Dd ripples(double x, double y, double z, double bumpAmount,
                             double frequency, double phase, int numberOfWaves,
                             Vector3Dd normal)
    {
        Vector3Dd out = normal;
        for ( int i = 0; i < numberOfWaves; i++ ) {
            Vector3Dd point = new Vector3Dd(x, y, z).subtract(textureUtils.waveSources()[i]);
            double length = point.dotProduct(point);
            if ( length == 0.0 ) {
                length = 1.0;
            }
            length = Math.sqrt(length);
            double index = length * frequency + phase;
            double scalar = proceduralNoise.cycloidal(index) * bumpAmount;
            point = point.multiply(scalar / length / numberOfWaves);
            out = out.add(point);
        }
        return out.normalized();
    }

    public void ripples(double x, double y, double z, double bumpAmount,
                        double frequency, double phase, int numberOfWaves,
                        Vector3Dd[] normal)
    {
        normal[0] = ripples(x, y, z, bumpAmount, frequency, phase, numberOfWaves, normal[0]);
    }

    public Vector3Dd waves(double x, double y, double z, double bumpAmount,
                           double frequency, double phase, int numberOfWaves,
                           Vector3Dd normal)
    {
        Vector3Dd out = normal;
        for ( int i = 0; i < numberOfWaves; i++ ) {
            Vector3Dd point = new Vector3Dd(x, y, z).subtract(textureUtils.waveSources()[i]);
            double length = point.dotProduct(point);
            if ( length == 0.0 ) {
                length = 1.0;
            }
            length = Math.sqrt(length);
            double index = length * frequency * textureUtils.waveFrequency()[i] + phase;
            double scalar = proceduralNoise.cycloidal(index) * bumpAmount / textureUtils.waveFrequency()[i];
            point = point.multiply(scalar / length / numberOfWaves);
            out = out.add(point);
        }
        return out.normalized();
    }

    public void waves(double x, double y, double z, double bumpAmount,
                      double frequency, double phase, int numberOfWaves,
                      Vector3Dd[] normal)
    {
        normal[0] = waves(x, y, z, bumpAmount, frequency, phase, numberOfWaves, normal[0]);
    }

    public Vector3Dd bumps(double x, double y, double z, double bumpAmount, Vector3Dd normal)
    {
        if ( bumpAmount == 0.0 ) {
            return normal;
        }
        Vector3Dd bumpTurbulence = proceduralNoise.differentialNoise(x, y, z).multiply(bumpAmount);
        return normal.add(bumpTurbulence).normalized();
    }

    public void bumps(double x, double y, double z, double bumpAmount, Vector3Dd[] normal)
    {
        normal[0] = bumps(x, y, z, bumpAmount, normal[0]);
    }

    public Vector3Dd dents(double x, double y, double z, double bumpAmount, Vector3Dd normal)
    {
        if ( bumpAmount == 0.0 ) {
            return normal;
        }
        double noise = proceduralNoise.noise(x, y, z);
        noise = noise * noise * noise * bumpAmount;
        Vector3Dd stuccoTurbulence = proceduralNoise.differentialNoise(x, y, z).multiply(noise);
        return normal.add(stuccoTurbulence).normalized();
    }

    public void dents(double x, double y, double z, double bumpAmount, Vector3Dd[] normal)
    {
        normal[0] = dents(x, y, z, bumpAmount, normal[0]);
    }

    public Vector3Dd wrinkles(double x, double y, double z, double bumpAmount, Vector3Dd normal)
    {
        if ( bumpAmount == 0.0 ) {
            return normal;
        }
        double rx = 0.0;
        double ry = 0.0;
        double rz = 0.0;
        for ( int i = 0; i < 10; i++ ) {
            double scale = Math.pow(2.0, i);
            Vector3Dd value = proceduralNoise.differentialNoise(x * scale, y * scale, z * scale);
            rx += TextureUtils.fabsInline(value.x() / scale);
            ry += TextureUtils.fabsInline(value.y() / scale);
            rz += TextureUtils.fabsInline(value.z() / scale);
        }
        Vector3Dd result = new Vector3Dd(rx, ry, rz).multiply(bumpAmount);
        return normal.add(result).normalized();
    }

    public void wrinkles(double x, double y, double z, double bumpAmount, Vector3Dd[] normal)
    {
        normal[0] = wrinkles(x, y, z, bumpAmount, normal[0]);
    }
}
