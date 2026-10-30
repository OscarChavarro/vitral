#ifndef __SVGALIB_MOUSE__
#define __SVGALIB_MOUSE__
#include "drivers/video/svgalib/vgamouse.h"

// svgalib's mouse (<vgamouse.h> plus the mouse calls of <vga.h>) on top of /dev/input/mice, read with the protocol of
// the configured type (MOUSE_IMPS2, the type a Linux libvga.config gives that device). What svgalib keeps in globals
// is here, one copy per console like the rest of the compat libraries, and starts again when another process uses it.
class SvgalibMouse {
public:
    static char DEVICE[];
    static constexpr int TYPE = MOUSE_IMPS2;
    static constexpr int AXES = 6;
    static constexpr int BUFFER_SIZE = 256;

    using Handler = void (*)(int button, int dx, int dy, int dz, int drx, int dry, int drz);

    class State {
    public:
        int owner;
        bool support;
        int fd;
        int type;
        Handler handler;
        MouseCaps caps;
        bool wantCaps;
        unsigned char buffer[BUFFER_SIZE];
        int bytes;
        int button;
        int scale;
        int wrap;
        // Indexed like the MOUSE_*DIM and MOUSE_WRAP* bits: x, y, z, rx, ry, rz.
        int position[AXES];
        int low[AXES];
        int high[AXES];
    };

    // vga_init(): opens the configured mouse when the program asked for mouse support.
    static void initialize();
    // vga_setmode() of a graphics mode: with mouse support, the range becomes the screen and wrapping is turned off.
    static void fitToMode(int width, int height);

    static State &mine();
    static int open(const char *device, int type);
    static int update(State &s, bool wait);
    static void defaultHandler(int button, int dx, int dy, int dz, int drx, int dry, int drz);

private:
    static State states[];

    static void reset(State &s, int owner);
    static int packetSize(int type) { return type == MOUSE_IMPS2 ? 4 : 3; }
    static bool isHeader(int type, unsigned char c);
    static void report(State &s, const unsigned char *packet);
    static void keepInRange(State &s, int axis);
};

#endif
