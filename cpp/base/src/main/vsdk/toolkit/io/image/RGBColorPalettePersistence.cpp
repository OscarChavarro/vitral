#include <cstdlib>

#include "vsdk/toolkit/io/image/RGBColorPalettePersistence.h"
#include "vsdk/toolkit/media/RGBColorPalette.h"

namespace {

bool isWordChar(int c)
{
    return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
}

bool isNumberStart(int c)
{
    return (c >= '0' && c <= '9') || c == '.' || c == '-';
}

}

RGBColorPalette* RGBColorPalettePersistence::importGimpPalette(
    java::InputStream& source)
{
    // Tokenization follows java.io.StreamTokenizer as configured by the Java
    // version: '#' comments, significant end of lines, ' ', ',' and '\t'
    // as white space, and parsed numbers.
    RGBColorPalette* p = new RGBColorPalette();
    p->init(0);

    int startline = 0;
    double r = 0.0;
    double g = 0.0;
    int c = source.read();

    while ( c >= 0 ) {
        if ( c == '\n' || c == '\r' ) {
            startline = 0;
            c = source.read();
        }
        else if ( c == '#' ) {
            while ( c >= 0 && c != '\n' && c != '\r' ) {
                c = source.read();
            }
        }
        else if ( isNumberStart(c) ) {
            char buffer[64];
            int n = 0;
            do {
                if ( n < 63 ) {
                    buffer[n++] = (char)c;
                }
                c = source.read();
            } while ( (c >= '0' && c <= '9') || c == '.' );
            buffer[n] = '\0';
            double nval = std::atof(buffer);
            switch ( startline ) {
              case 0:
                r = nval / 255.0;
                break;
              case 1:
                g = nval / 255.0;
                break;
              case 2:
                p->addColor(r, g, nval / 255.0);
                break;
              default:
                break;
            }
            startline++;
        }
        else if ( isWordChar(c) ) {
            while ( c >= 0 && (isWordChar(c) || (c >= '0' && c <= '9')) ) {
                c = source.read();
            }
        }
        else {
            c = source.read();
        }
    }

    return p;
}

RGBColorPalette* RGBColorPalettePersistence::importRawPalette(
    java::InputStream& dis)
{
    RGBColorPalette* p = new RGBColorPalette();
    p->init(0);

    while ( true ) {
        int nr = dis.read();
        int ng = dis.read();
        int nb = dis.read();
        if ( nr < 0 || ng < 0 || nb < 0 ) {
            break;
        }
        p->addColor(nr, ng, nb);
    }

    dis.close();
    return p;
}
