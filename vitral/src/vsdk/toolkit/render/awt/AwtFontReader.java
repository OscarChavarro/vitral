//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 15 2006 - Oscar Chavarro: Original base version                =
//===========================================================================

package vsdk.toolkit.render.awt;

import vsdk.toolkit.io.geometry.FontReader;
import vsdk.toolkit.environment.geometry.ParametricCurve;

/**
This is an implementation of the persistence interface FontReader. It is
managed as a rendering operation because it actually uses font rendering
capabilities from Awt. Current mechanism used for font persistence (import)
is to "render" the glyph into an Awt rendering data structure, but not
showing it, instead using it to construct another data structure, which is
returned.

This class is a concrete factory in an abstract factory design pattern role.
*/
public class AwtFontReader extends FontReader
{
    public ParametricCurve extractGlyph(
        String fontFile, String characterAndItsContext)
    {
        ParametricCurve curve;

        curve = new ParametricCurve();

        return curve;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
