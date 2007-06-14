//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - February 24 2006 - Gina Chiquillo: Original base version              =
//= - April 4 2006 - Oscar Chavarro: Quality check - comments added         =
//===========================================================================

package vsdk.toolkit.media;

import java.util.ArrayList;
import java.util.Iterator;
import vsdk.toolkit.common.ColorRgb;

public class RGBColorPalette {
    private ArrayList<ColorRgb> colors;

    public RGBColorPalette()
    {
        colors = new ArrayList<ColorRgb>();
        init(256);
    }

    public void init(int size)
    {
        colors = new ArrayList<ColorRgb>();
        int i;

        ColorRgb c;
        for ( i = 0; i < size; i++ ) {
            c = new ColorRgb(0, 0, 0);
            colors.add(c);
        }
        buildGrayLevelsTable();
    }

    public int size()
    {
        return colors.size();
    }

    public void buildGrayLevelsTable()
    {
        int pos = 0;
        double val = 0;

        for ( Iterator i = colors.iterator(); i.hasNext(); ) {
            ColorRgb c = (ColorRgb)i.next();
            c.r = c.g = c.b = val;
            val += 1.0/((double)(colors.size()-1));
            pos++;        
        }
    }

    public ColorRgb getColorAt(int i)
    {
        if ( i < 0 || i >= colors.size() ) return null;
        return (ColorRgb)colors.get(i);
    }

    public void setColorAt(int i, ColorRgb c)
    {
        if ( i < 0 || i >= colors.size() ) return;
        colors.set(i, c);
    }

    public void setColorAt(int i, double r, double g, double b)
    {
        if ( i < 0 || i >= colors.size() ) return;

        colors.set(i, new ColorRgb(r, g, b));
    }

    public void addColor(ColorRgb c)
    {
        colors.add(c);
    }

    public void addColor(double r, double g, double b)
    {
        colors.add(new ColorRgb(r, g, b));
    }

    /**
    The parameter `t` must be between 0 and 1
    */
    public ColorRgb evalNearest(double t)
    {
        if ( t < 0.0 ) t = 0.0;
        if ( t > 1.0 ) t = 1.0;

        int N = colors.size();

        int i = (int)(t*((double)N));

        if ( i < 0 ) i = 0;
        if ( i >= N ) i = N-1;

        return (ColorRgb)colors.get(i);
    }

    /**
    The parameter `t` must be between 0 and 1.
    */
    public ColorRgb evalLinear(double t)
    {
        if ( t < 0.0 ) t = 0.0;
        if ( t > 1.0 ) t = 1.0;

        int N = colors.size()-1;

        int inf = (int)(t*((double)N));
        int sup = inf+1;

        double delta = 1/((double)N);
        double r = t - inf*delta;
        double p = r / delta;

        if ( inf < 0 ) inf = 0;
        if ( inf > N ) inf = N;
        if ( sup < 0 ) sup = 0;
        if ( sup > N ) sup = N;

        ColorRgb a = colors.get(inf);
        ColorRgb b = colors.get(sup);
        ColorRgb c = new ColorRgb(
            a.r + (b.r-a.r)*p,
            a.g + (b.g-a.g)*p,
            a.b + (b.b-a.b)*p    );

        return c;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
