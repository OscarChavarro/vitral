#ifndef __RGB_COLOR_PALETTE_PERSISTENCE__
#define __RGB_COLOR_PALETTE_PERSISTENCE__

#include "java/io/InputStream.h"

class RGBColorPalette;

/**
This class contains the persistence operations to load and save
RGBColorPalettes
*/
class RGBColorPalettePersistence {
public:
    /**
    This method reads a text formated stream in the style of GIMP
    palettes and from its data generates a RGBColorPalette
    @param source text stream to read
    @return the RGBColorPalette builded from the source, owned by the caller
    */
    static RGBColorPalette* importGimpPalette(java::InputStream& source);

    /**
    This method reads a binary raw stream of consecutive RGB color values
    @param dis binary stream to read; it is closed at the end
    @return the RGBColorPalette builded from the source, owned by the caller
    */
    static RGBColorPalette* importRawPalette(java::InputStream& dis);
};

#endif
