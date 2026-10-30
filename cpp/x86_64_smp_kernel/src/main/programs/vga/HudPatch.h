#ifndef __HUD_PATCH__
#define __HUD_PATCH__

class Hud;
class Picture;

class HudPatch {
  public:
    void set(const Hud *hud, const char *before, const char *after, int top, int glyphHeight, int first, int last);
    int uncovers(int x, int y) const;
    void draw(const Picture &picture) const;
    int minX() const { return x0; }
    int maxX() const { return x1; }
    int minY() const { return y0; }
    int maxY() const { return y1; }

  private:
    const Hud *hud;
    const char *before;
    const char *after;
    int left;
    int top;
    int x0;
    int y0;
    int x1;
    int y1;
};

#endif
