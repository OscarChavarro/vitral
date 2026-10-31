#include <vga.h>
#include <vgamouse.h>
#include <fcntl.h>
#include <poll.h>
#include <stdio.h>
#include <string.h>
#include <sys/select.h>
#include <unistd.h>
#include "lib/svgalib/SvgalibMouse.h"

char SvgalibMouse::DEVICE[] = "/dev/input/mice";

// What svgalib writes to an IMPS/2 mouse to turn its wheel on (sample rates 200, 100, 80).
static const unsigned char WHEEL_KNOCK[] = {0xF3, 200, 0xF3, 100, 0xF3, 80};

void
SvgalibMouse::reset(State &s, int owner) {
    s = State{};
    s.owner = owner;
    s.fd = -1;
    s.type = MOUSE_NONE;
    s.handler = defaultHandler;
    s.scale = 1;
    for (int axis = 0; axis < AXES; axis++) {
        s.high[axis] = 32767;
    }
}

SvgalibMouse::State &
SvgalibMouse::mine() {
    State &s = contextState();
    int pid = getpid();
    if (s.owner != pid) {
        reset(s, pid);
    }
    return s;
}

void
SvgalibMouse::initialize() {
    if (mine().support && mouse_init(DEVICE, TYPE, MOUSE_DEFAULTSAMPLERATE)) {
        fprintf(stderr, "svgalib: Failed to initialize mouse.\n");
    }
}

void
SvgalibMouse::fitToMode(int width, int height) {
    State &s = mine();
    if (s.support && s.fd >= 0) {
        mouse_setxrange(0, width - 1);
        mouse_setyrange(0, height - 1);
        mouse_setwrap(MOUSE_NOWRAP);
    }
}

int
SvgalibMouse::open(const char *device, int type) {
    State &s = mine();
    if (s.fd >= 0) {
        return s.fd;
    }
    type &= MOUSE_TYPE_MASK;
    if (type != MOUSE_PS2 && type != MOUSE_IMPS2) {
        return -1;
    }
    int fd = ::open(*device ? device : DEVICE, O_RDWR | O_NONBLOCK);
    if (fd < 0) {
        return -1;
    }
    if (type == MOUSE_IMPS2 && write(fd, WHEEL_KNOCK, sizeof(WHEEL_KNOCK)) != (ssize_t)sizeof(WHEEL_KNOCK)) {
        close(fd);
        return -1;
    }
    s.fd = fd;
    s.type = type;
    s.bytes = 0;
    s.handler = defaultHandler;
    s.caps = MouseCaps{};
    s.caps.key = MOUSE_GOTCAPS;
    s.caps.buttons = MOUSE_LEFTBUTTON | MOUSE_MIDDLEBUTTON | MOUSE_RIGHTBUTTON;
    s.caps.axes = MOUSE_2DIM;
    if (type == MOUSE_IMPS2) {
        s.caps.buttons |= MOUSE_FOURTHBUTTON;
        s.caps.axes |= MOUSE_RXDIM;
        s.caps.info = MOUSE_INFO_WHEEL;
    }
    return fd;
}

bool
SvgalibMouse::isHeader(int type, unsigned char c) {
    if (type == MOUSE_IMPS2) {
        return (c & 0xC8) == 0x08;
    }
    return (c & 0xC0) == 0x00;
}

void
SvgalibMouse::report(State &s, const unsigned char *packet) {
    int button = (packet[0] & 0x04) >> 1 | (packet[0] & 0x02) >> 1 | (packet[0] & 0x01) << 2;
    int dx = packet[0] & 0x10 ? packet[1] - 256 : packet[1];
    int dy = packet[0] & 0x20 ? -(packet[2] - 256) : -packet[2];
    int drx = 0;
    if (s.type == MOUSE_IMPS2 && packet[3]) {
        drx = packet[3] > 0x7F ? -DEFAULT_WHEEL_DELTA : DEFAULT_WHEEL_DELTA;
    }
    s.handler(button, dx, dy, 0, drx, 0, 0);
}

int
SvgalibMouse::update(State &s, bool wait) {
    if (s.fd < 0) {
        return -1;
    }
    int size = packetSize(s.type);
    bool handled = false;
    for (;;) {
        ssize_t got = read(s.fd, s.buffer + s.bytes, (size_t)(BUFFER_SIZE - s.bytes));
        if (got > 0) {
            s.bytes += (int)got;
        }
        int first = 0;
        while (s.bytes - first >= size) {
            if (!isHeader(s.type, s.buffer[first])) {
                first++;
                continue;
            }
            report(s, s.buffer + first);
            handled = true;
            first += size;
        }
        memmove(s.buffer, s.buffer + first, (size_t)(s.bytes - first));
        s.bytes -= first;
        if (!wait || handled) {
            return handled ? 1 : 0;
        }
        pollfd ready = {s.fd, POLLIN, 0};
        poll(&ready, 1, -1);
    }
}

void
SvgalibMouse::keepInRange(State &s, int axis) {
    int &p = s.position[axis];
    int span = (s.high[axis] - s.low[axis]) * s.scale;
    bool wraps = s.wrap & (1 << axis);
    if (p / s.scale > s.high[axis]) {
        p = wraps ? p - span : s.high[axis] * s.scale;
    }
    if (p / s.scale < s.low[axis]) {
        p = wraps ? p + span : s.low[axis] * s.scale;
    }
}

void
SvgalibMouse::defaultHandler(int button, int dx, int dy, int dz, int drx, int dry, int drz) {
    State &s = mine();
    int deltas[AXES] = {dx, dy, dz, drx, dry, drz};
    s.button = button;
    for (int axis = 0; axis < AXES; axis++) {
        if (axis < 3 || (s.wrap & MOUSE_ROT_COORDS) == MOUSE_ROT_RX_RY_RZ) {
            s.position[axis] += deltas[axis];
        } else if ((s.wrap & MOUSE_ROT_COORDS) == MOUSE_ROT_INFINITESIMAL) {
            s.position[axis] = deltas[axis];
        }
        keepInRange(s, axis);
    }
}

int
mouse_init_return_fd(char *dev, int type, int samplerate) {
    (void)samplerate;
    return SvgalibMouse::open(dev, type);
}

int
mouse_init(char *dev, int type, int samplerate) {
    if (SvgalibMouse::mine().fd >= 0) {
        return 0;
    }
    if (!dev) {
        dev = SvgalibMouse::DEVICE;
        type = SvgalibMouse::TYPE;
    }
    return mouse_init_return_fd(dev, type, samplerate) == -1 ? -1 : 0;
}

void
mouse_seteventhandler(__mouse_handler handler) {
    SvgalibMouse::mine().handler = reinterpret_cast<SvgalibMouse::Handler>(handler);
}

void
mouse_setdefaulteventhandler(void) {
    SvgalibMouse::mine().handler = SvgalibMouse::defaultHandler;
}

void
mouse_close(void) {
    SvgalibMouse::State &s = SvgalibMouse::mine();
    if (s.fd >= 0) {
        close(s.fd);
        s.fd = -1;
    }
}

int
mouse_update(void) {
    return SvgalibMouse::update(SvgalibMouse::mine(), false);
}

void
mouse_waitforupdate(void) {
    SvgalibMouse::update(SvgalibMouse::mine(), true);
}

void
mouse_setposition(int x, int y) {
    mouse_setposition_6d(x, y, 0, 0, 0, 0, MOUSE_2DIM);
}

void
mouse_setposition_6d(int x, int y, int z, int rx, int ry, int rz, int dim_mask) {
    SvgalibMouse::State &s = SvgalibMouse::mine();
    int values[SvgalibMouse::AXES] = {x, y, z, rx, ry, rz};
    for (int axis = 0; axis < SvgalibMouse::AXES; axis++) {
        if (dim_mask & (1 << axis)) {
            s.position[axis] = values[axis] * s.scale;
        }
    }
    if ((dim_mask & MOUSE_CAPS) && x == MOUSE_WANTCAPS) {
        s.wantCaps = true;
    }
}

void
mouse_setxrange(int x1, int x2) {
    mouse_setrange_6d(x1, x2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, MOUSE_XDIM);
}

void
mouse_setyrange(int y1, int y2) {
    mouse_setrange_6d(0, 0, y1, y2, 0, 0, 0, 0, 0, 0, 0, 0, MOUSE_YDIM);
}

void
mouse_setrange_6d(int x1, int x2, int y1, int y2, int z1, int z2, int rx1, int rx2, int ry1, int ry2, int rz1,
                  int rz2, int dim_mask) {
    SvgalibMouse::State &s = SvgalibMouse::mine();
    int lows[SvgalibMouse::AXES] = {x1, y1, z1, rx1, ry1, rz1};
    int highs[SvgalibMouse::AXES] = {x2, y2, z2, rx2, ry2, rz2};
    for (int axis = 0; axis < SvgalibMouse::AXES; axis++) {
        if (dim_mask & (1 << axis)) {
            s.low[axis] = lows[axis];
            s.high[axis] = highs[axis];
        }
    }
}

void
mouse_setscale(int s) {
    SvgalibMouse::State &state = SvgalibMouse::mine();
    if (s == 0) {
        fprintf(stderr, "Mouse scale must be non-zero!\n");
        return;
    }
    for (int axis = 0; axis < SvgalibMouse::AXES; axis++) {
        state.position[axis] = state.position[axis] * s / state.scale;
    }
    state.scale = s;
}

void
mouse_setwrap(int w) {
    SvgalibMouse::mine().wrap = w;
}

int
mouse_getx(void) {
    SvgalibMouse::State &s = SvgalibMouse::mine();
    return s.position[0] / s.scale;
}

int
mouse_gety(void) {
    SvgalibMouse::State &s = SvgalibMouse::mine();
    return s.position[1] / s.scale;
}

void
mouse_getposition_6d(int *x, int *y, int *z, int *rx, int *ry, int *rz) {
    SvgalibMouse::State &s = SvgalibMouse::mine();
    int *out[SvgalibMouse::AXES] = {x, y, z, rx, ry, rz};
    if (s.wantCaps) {
        int caps[SvgalibMouse::AXES] = {s.caps.key, s.caps.buttons, s.caps.axes, s.caps.info, s.caps.reserved0,
                                        s.caps.reserved1};
        for (int axis = 0; axis < SvgalibMouse::AXES; axis++) {
            *out[axis] = caps[axis];
        }
        s.wantCaps = false;
        return;
    }
    for (int axis = 0; axis < SvgalibMouse::AXES; axis++) {
        if (out[axis]) {
            *out[axis] = s.position[axis] / s.scale;
        }
    }
}

int
mouse_getbutton(void) {
    return SvgalibMouse::mine().button;
}

int
vga_getmousetype() {
    return SvgalibMouse::TYPE;
}

void
vga_setmousesupport(int s) {
    SvgalibMouse::mine().support = s != 0;
}

int
vga_waitevent(int which, fd_set *in, fd_set *out, fd_set *except, timeval *timeout) {
    fd_set inSet;
    if (!in) {
        in = &inSet;
        FD_ZERO(in);
    }
    SvgalibMouse::State &s = SvgalibMouse::mine();
    int keys = fileno(stdin);
    if ((which & VGA_MOUSEEVENT) && s.fd >= 0) {
        FD_SET(s.fd, in);
    }
    if (which & VGA_KEYEVENT) {
        FD_SET(keys, in);
    }
    if (select(FD_SETSIZE, in, out, except, timeout) < 0) {
        return -1;
    }
    int events = 0;
    if ((which & VGA_MOUSEEVENT) && s.fd >= 0 && FD_ISSET(s.fd, in)) {
        events |= VGA_MOUSEEVENT;
        FD_CLR(s.fd, in);
        mouse_update();
    }
    if ((which & VGA_KEYEVENT) && FD_ISSET(keys, in)) {
        events |= VGA_KEYEVENT;
        FD_CLR(keys, in);
    }
    return events;
}
