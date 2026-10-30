#ifndef __MOUSE_CURSOR__
#define __MOUSE_CURSOR__

class MouseCursor {
public:
    static constexpr int MAX_SIZE = 32;

    class Shape {
    public:
        int width;
        int height;
        int hotX;
        int hotY;
        const char *const *rows;
    };
    static const Shape ARROW;
    static const Shape CROSSHAIR;

    // What a text screen shows in a cell. Text mode has no frame buffer to save, so the pointer (the cell in inverse
    // video) is drawn from it and removed by writing the cell back as it was.
    class TextScreen {
    public:
        virtual char cellAt(int column, int row, bool &bold, bool &inverse) const = 0;

    protected:
        ~TextScreen() = default;
    };

    MouseCursor();

    void useGraphics(int width, int height, int fill, int outline, const Shape &shape = ARROW);
    void useText(int columns, int rows, const TextScreen &screen);

    void moveTo(int x, int y);
    int x() const { return posX; }
    int y() const { return posY; }
    bool graphics() const { return graphicsMode; }

    void show();
    void hide();
    bool shown() const { return visible; }
    bool overlaps(int x, int y, int w, int h) const;

private:
    bool graphicsMode;
    int width;
    int height;
    int fillColor;
    int outlineColor;
    const Shape *shape;
    const TextScreen *text;
    int posX;
    int posY;
    bool visible;
    int savedX;
    int savedY;
    int current;
    int saved[2][MAX_SIZE * MAX_SIZE];

    char pixelAt(int originX, int originY, int x, int y) const;
    int colorOf(char c) const { return c == '#' ? outlineColor : fillColor; }
    void draw();
    void restore();
    void writeCell(bool pointer) const;
    void move(int x, int y);
};

#endif
