#ifndef __VSDK_TOOLKIT_RENDER_TILE_H__
#define __VSDK_TOOLKIT_RENDER_TILE_H__

class Image;

class Tile {
private:
    Image* image;
    int x0;
    int y0;
    int dx;
    int dy;

public:
    Tile();
    Tile(Image* image, int x0, int y0, int dx, int dy);

    Image* getImage() const;
    int getX0() const;
    int getY0() const;
    int getDx() const;
    int getDy() const;
    int getX1() const;
    int getY1() const;
};

#endif
