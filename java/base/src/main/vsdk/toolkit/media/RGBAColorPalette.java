package vsdk.toolkit.media;

import java.util.ArrayList;
import java.util.List;

import vsdk.toolkit.common.color.ColorRgba;

public class RGBAColorPalette extends MediaEntity
{
    private final ArrayList<ColorRgba> colors;
    private final ArrayList<Double> positions;

    public RGBAColorPalette()
    {
        colors = new ArrayList<ColorRgba>();
        positions = new ArrayList<Double>();
    }

    public void init(int size)
    {
        colors.clear();
        positions.clear();
        for ( int i = 0; i < size; i++ ) {
            colors.add(new ColorRgba());
        }
    }

    public int size()
    {
        return colors.size();
    }

    public boolean hasPositions()
    {
        return !positions.isEmpty();
    }

    public ColorRgba getColorAt(int i)
    {
        if ( i < 0 || i >= colors.size() ) {
            return null;
        }
        return new ColorRgba(colors.get(i));
    }

    public double getPositionAt(int i)
    {
        if ( i < 0 || i >= positions.size() ) {
            return 0.0;
        }
        return positions.get(i);
    }

    public void setColorAt(int i, ColorRgba c)
    {
        if ( i < 0 || i >= colors.size() ) {
            return;
        }
        colors.set(i, new ColorRgba(c));
    }

    public void setColorAt(int i, double r, double g, double b, double a)
    {
        if ( i < 0 || i >= colors.size() ) {
            return;
        }
        colors.set(i, new ColorRgba(r, g, b, a));
    }

    public void addColor(ColorRgba c)
    {
        colors.add(new ColorRgba(c));
    }

    public void addColor(double r, double g, double b, double a)
    {
        colors.add(new ColorRgba(r, g, b, a));
    }

    public void addColorAt(double position, ColorRgba c)
    {
        colors.add(new ColorRgba(c));
        positions.add(position);
    }

    public ColorRgba evalNearest(double t)
    {
        t = clamp01(t);
        int n = colors.size();
        if ( n == 0 ) {
            return new ColorRgba();
        }
        int i = (int)(t * n);
        if ( i < 0 ) {
            i = 0;
        }
        if ( i >= n ) {
            i = n - 1;
        }
        return new ColorRgba(colors.get(i));
    }

    public ColorRgba evalLinear(double t)
    {
        t = clamp01(t);
        if ( colors.isEmpty() ) {
            return new ColorRgba();
        }
        if ( colors.size() == 1 ) {
            return new ColorRgba(colors.get(0));
        }
        if ( positions.size() > 0 && positions.size() == colors.size() ) {
            return evalPositioned(t);
        }
        int n = colors.size() - 1;
        int inf = (int)(t * n);
        int sup = inf + 1;
        double delta = 1.0 / n;
        double p = (t - inf * delta) / delta;

        if ( inf < 0 ) inf = 0;
        if ( inf > n ) inf = n;
        if ( sup < 0 ) sup = 0;
        if ( sup > n ) sup = n;

        return interpolate(colors.get(inf), colors.get(sup), p);
    }

    public List<ColorRgba> colorsView()
    {
        return List.copyOf(colors);
    }

    private ColorRgba evalPositioned(double t)
    {
        int n = colors.size() - 1;
        for ( int i = 0; i < n; i++ ) {
            double a = positions.get(i);
            double b = positions.get(i + 1);
            if ( a == b ) {
                continue;
            }
            if ( t >= a && t <= b ) {
                return interpolate(colors.get(i), colors.get(i + 1), (t - a) / (b - a));
            }
        }
        return new ColorRgba();
    }

    private static ColorRgba interpolate(ColorRgba a, ColorRgba b, double p)
    {
        return new ColorRgba(
            a.getR() + (b.getR() - a.getR()) * p,
            a.getG() + (b.getG() - a.getG()) * p,
            a.getB() + (b.getB() - a.getB()) * p,
            a.getA() + (b.getA() - a.getA()) * p);
    }

    private static double clamp01(double t)
    {
        if ( t < 0.0 ) return 0.0;
        if ( t > 1.0 ) return 1.0;
        return t;
    }
}
