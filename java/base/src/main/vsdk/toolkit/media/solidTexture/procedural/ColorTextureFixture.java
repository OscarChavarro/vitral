package vsdk.toolkit.media.solidTexture.procedural;

import vsdk.toolkit.common.color.ColorRgba;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.media.RGBAColorPalette;
import vsdk.toolkit.media.solidTexture.TextureUtils;

public final class ColorTextureFixture
{
    private final ProceduralNoise proceduralNoise;
    private final TextureUtils textureUtils;

    public ColorTextureFixture(ProceduralNoise proceduralNoise, TextureUtils textureUtils)
    {
        this.proceduralNoise = proceduralNoise;
        this.textureUtils = textureUtils;
    }

    public void agate(double x, double y, double z, int octaves, RGBAColorPalette colorMap, ColorRgba color)
    {
        double noise = proceduralNoise.cycloidal(
            1.3 * proceduralNoise.turbulence(x, y, z, octaves) + 1.1 * z) + 1.0;
        noise *= 0.5;
        noise = Math.pow(noise, 0.77);

        if ( addMappedColor(color, colorMap, noise) ) {
            return;
        }

        double hue = 1.0 - noise;
        if ( noise < 0.5 ) {
            add(color, 1.0 - noise / 10.0, 1.0 - noise / 5.0, hue, 0.0);
        }
        else if ( noise < 0.6 ) {
            add(color, 0.9, 0.7, hue, 0.0);
        }
        else {
            add(color, 0.6 + hue, 0.3 + hue, hue, 0.0);
        }
    }

    public void bozo(double x, double y, double z, double turbulence, int octaves,
                     RGBAColorPalette colorMap, ColorRgba color)
    {
        if ( turbulence != 0.0 ) {
            Vector3Dd t = proceduralNoise.differentialTurbulence(x, y, z, octaves);
            x += t.x() * turbulence;
            y += t.y() * turbulence;
            z += t.z() * turbulence;
        }

        double noise = proceduralNoise.noise(x, y, z);
        if ( addMappedColor(color, colorMap, noise) ) {
            return;
        }
        if ( noise < 0.4 ) {
            add(color, 1.0, 1.0, 1.0, 0.0);
        }
        else if ( noise < 0.6 ) {
            add(color, 0.0, 1.0, 0.0, 0.0);
        }
        else if ( noise < 0.8 ) {
            add(color, 0.0, 0.0, 1.0, 0.0);
        }
        else {
            add(color, 1.0, 0.0, 0.0, 0.0);
        }
    }

    public void brick(double x, double y, double z, ColorRgba color,
                      ColorRgba color1, ColorRgba color2, double mortar)
    {
        double xr = Math.abs(x % 1.0);
        double yr = Math.abs(y % 1.0);
        double zr = Math.abs(z % 1.0);
        color.set(color2);
        if ( xr > 0.0 && xr < mortar ) {
            color.set(color1);
            return;
        }
        if ( yr > 0.0 && yr < mortar ) {
            color.set(color1);
            return;
        }
        if ( zr > 0.0 && zr < mortar ) {
            color.set(color1);
        }
    }

    public void checker(double x, double y, double z, ColorRgba color,
                        ColorRgba color1, ColorRgba color2, double smallTolerance)
    {
        x += smallTolerance;
        y += smallTolerance;
        z += smallTolerance;
        int index = (int)(TextureUtils.floorInline(x) + TextureUtils.floorInline(y) + TextureUtils.floorInline(z));
        add(color, ((index & 1) != 0) ? color1 : color2);
    }

    public void gradient(double x, double y, double z, double turbulence,
                         RGBAColorPalette colorMap, Vector3Dd textureGradient,
                         int octaves, ColorRgba color)
    {
        if ( turbulence != 0.0 ) {
            Vector3Dd t = proceduralNoise.differentialTurbulence(x, y, z, octaves);
            x += t.x() * turbulence;
            y += t.y() * turbulence;
            z += t.z() * turbulence;
        }
        if ( colorMap == null ) {
            return;
        }

        double value = 0.0;
        if ( textureGradient.x() != 0.0 ) {
            x = TextureUtils.fabsInline(x);
            value += x - TextureUtils.floorInline(x);
        }
        if ( textureGradient.y() != 0.0 ) {
            y = TextureUtils.fabsInline(y);
            value += y - TextureUtils.floorInline(y);
        }
        if ( textureGradient.z() != 0.0 ) {
            z = TextureUtils.fabsInline(z);
            value += z - TextureUtils.floorInline(z);
        }
        value = value > 1.0 ? value % 1.0 : value;
        addMappedColor(color, colorMap, value);
    }

    public void granite(double x, double y, double z, RGBAColorPalette colorMap, ColorRgba color)
    {
        double noise = 0.0;
        double freq = 1.0;
        for ( int i = 0; i < 6; freq *= 2.0, i++ ) {
            double temp = 0.5 - proceduralNoise.noise(x * 4.0 * freq, y * 4.0 * freq, z * 4.0 * freq);
            noise += TextureUtils.fabsInline(temp) / freq;
        }
        if ( !addMappedColor(color, colorMap, noise) ) {
            add(color, noise, noise, noise, 0.0);
        }
    }

    public void marble(double x, double y, double z, double turbulence, int octaves,
                       RGBAColorPalette colorMap, ColorRgba color)
    {
        double noise = proceduralNoise.triangleWave(
            x + proceduralNoise.turbulence(x, y, z, octaves) * turbulence);
        if ( addMappedColor(color, colorMap, noise) ) {
            return;
        }
        if ( noise < 0.0 ) {
            add(color, 0.9, 0.8, 0.8, 0.0);
        }
        else if ( noise < 0.9 ) {
            double hue = 0.8 - noise * 0.8;
            add(color, 0.9, hue, hue, 0.0);
        }
    }

    public void spotted(double x, double y, double z, RGBAColorPalette colorMap, ColorRgba color)
    {
        double noise = proceduralNoise.noise(x, y, z);
        if ( !addMappedColor(color, colorMap, noise) ) {
            add(color, noise, noise, noise, 0.0);
        }
    }

    public void wood(double x, double y, double z, double turbulence, int octaves,
                     RGBAColorPalette colorMap, ColorRgba color)
    {
        Vector3Dd t = proceduralNoise.differentialTurbulence(x, y, z, octaves);
        double pointX = proceduralNoise.cycloidal((x + t.x()) * turbulence) + x;
        double pointY = proceduralNoise.cycloidal((y + t.y()) * turbulence) + y;
        double noise = proceduralNoise.triangleWave(new Vector3Dd(pointX, pointY, 0.0).length());

        if ( addMappedColor(color, colorMap, noise) ) {
            return;
        }
        if ( noise > 0.6 ) {
            add(color, 0.4, 0.133, 0.066, 0.0);
        }
        else {
            add(color, 0.666, 0.312, 0.2, 0.0);
        }
    }

    public void leopard(double x, double y, double z, double turbulence, int octaves,
                        RGBAColorPalette colorMap, ColorRgba color)
    {
        if ( turbulence != 0.0 ) {
            Vector3Dd t = proceduralNoise.differentialTurbulence(x, y, z, octaves);
            x += t.x() * turbulence;
            y += t.y() * turbulence;
            z += t.z() * turbulence;
        }
        double temp = (Math.sin(x) + Math.sin(y) + Math.sin(z)) / 3.0;
        double noise = temp * temp;
        if ( !addMappedColor(color, colorMap, noise) ) {
            add(color, noise, noise, noise, 0.0);
        }
    }

    public void onion(double x, double y, double z, double turbulence, int octaves,
                      RGBAColorPalette colorMap, ColorRgba color)
    {
        if ( turbulence != 0.0 ) {
            Vector3Dd t = proceduralNoise.differentialTurbulence(x, y, z, octaves);
            x += t.x() * turbulence;
            y += t.y() * turbulence;
            z += t.z() * turbulence;
        }
        double noise = Math.sqrt(x * x + y * y + z * z) % 1.0;
        if ( !addMappedColor(color, colorMap, noise) ) {
            add(color, noise, noise, noise, 0.0);
        }
    }

    private boolean addMappedColor(ColorRgba color, RGBAColorPalette colorMap, double value)
    {
        if ( colorMap == null ) {
            return false;
        }
        ColorRgba newColor = new ColorRgba();
        TextureUtils.computeColor(newColor, colorMap, value);
        add(color, newColor);
        return true;
    }

    private static void add(ColorRgba color, ColorRgba v)
    {
        add(color, v.getR(), v.getG(), v.getB(), v.getA());
    }

    private static void add(ColorRgba color, double r, double g, double b, double a)
    {
        color.setR(color.getR() + r);
        color.setG(color.getG() + g);
        color.setB(color.getB() + b);
        color.setA(color.getA() + a);
    }
}
