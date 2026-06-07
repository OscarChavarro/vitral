#include "vsdk/toolkit/render/Tile.h"
#include "vsdk/toolkit/media/Image.h"

#include <stdexcept>

Tile::Tile() : image(nullptr), x0(0), y0(0), dx(0), dy(0) {}

Tile::Tile(Image* imageIn, int x0In, int y0In, int dxIn, int dyIn)
    : image(imageIn), x0(x0In), y0(y0In), dx(dxIn), dy(dyIn)
{
    if ( image == 0 ) throw std::invalid_argument("image can not be null");
    if ( x0 < 0 || y0 < 0 ) throw std::invalid_argument("tile origin must be >= 0");
    if ( dx <= 0 || dy <= 0 ) throw std::invalid_argument("tile size must be > 0");
    if ( x0 + dx > image->getXSize() || y0 + dy > image->getYSize() ) {
        throw std::invalid_argument("tile bounds must be inside target image");
    }
}

Image* Tile::getImage() const { return image; }
int Tile::getX0() const { return x0; }
int Tile::getY0() const { return y0; }
int Tile::getDx() const { return dx; }
int Tile::getDy() const { return dy; }
int Tile::getX1() const { return x0 + dx; }
int Tile::getY1() const { return y0 + dy; }
