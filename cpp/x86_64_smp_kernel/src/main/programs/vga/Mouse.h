#ifndef __MOUSE__
#define __MOUSE__

#include <stddef.h>

#include "lib/mouseCursor/MouseCursor.h"

class Hud;
class Picture;

class Mouse : private MouseCursor::TextScreen {
  public:
    Mouse();
    ~Mouse() { close(); }
    Mouse(const Mouse &) = delete;
    Mouse &operator=(const Mouse &) = delete;

    void open();
    void close();
    bool isConnected() const { return connected; }
    MouseCursor &cursor() { return pointer; }
    int waitInput();
    void follow();
    void place(const Picture &picture, const Hud &hud);
    void describe(char *position, char *state, size_t size) const;

  private:
    // Mouse counts per text cell (svgalib's scale), and how far the wheel may turn either way.
    static constexpr int TEXT_SCALE = 8;
    static constexpr int WHEEL_RANGE = 1 << 20;

    bool connected;
    const Picture *picture;
    const Hud *hud;
    MouseCursor pointer;

    char cellAt(int column, int row, bool &bold, bool &inverse) const override;
};

#endif
