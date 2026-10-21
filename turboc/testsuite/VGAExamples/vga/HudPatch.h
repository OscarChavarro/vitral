#ifndef __HUD_PATCH__
#define __HUD_PATCH__

class Hud;
class Picture;

class HudPatch {
  public:
    void set(const Hud *hud, const char *before, const char *after, int top, int glyphHeight, int first, int last);
    int uncovers(int x, int y) const;
    void draw(const Picture &picture) const;
    int minX() const { return xMin; }
    int maxX() const { return xMax; }
    int minY() const { return yMin; }
    int maxY() const { return yMax; }

  private:
    const Hud *hud;
    const char *before;
    const char *after;
    int left;
    int top;
    int xMin;
    int yMin;
    int xMax;
    int yMax;
};

#endif
