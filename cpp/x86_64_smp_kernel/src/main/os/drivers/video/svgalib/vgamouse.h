#ifndef __VGAMOUSE__
#define __VGAMOUSE__

// svgalib's mouse interface (<vgamouse.h>, svgalib 1.9.25), implemented in SvgalibMouse.cpp on top of
// /dev/input/mice. Same names, values and prototypes as svgalib's header, with the C++ ABI.

#define MOUSE_MICROSOFT 0
#define MOUSE_MOUSESYSTEMS 1
#define MOUSE_MMSERIES 2
#define MOUSE_LOGITECH 3
#define MOUSE_BUSMOUSE 4
#define MOUSE_PS2 5
#define MOUSE_LOGIMAN 6
#define MOUSE_GPM 7
#define MOUSE_SPACEBALL 8
#define MOUSE_NONE 9
#define MOUSE_INTELLIMOUSE 10
#define MOUSE_IMPS2 11
#define MOUSE_PNP 12
#define MOUSE_WACOM_GRAPHIRE 13
#define MOUSE_DRMOUSE4DS 14
#define MOUSE_EXPPS2 15
#define MOUSE_LAST MOUSE_EXPPS2

#define MOUSE_ORIENTATION_VERTICAL 0
#define MOUSE_ORIENTATION_HORIZONTAL 1

#define MOUSE_ACCEL_TYPE_NORMAL 1
#define MOUSE_ACCEL_TYPE_POWER 2

#define DEFAULT_ACCEL_THRESH 5
#define DEFAULT_ACCEL_POWER 1.2
#define DEFAULT_ACCEL_OFFSET 1
#define DEFAULT_ACCEL_MULT 10
#define DEFAULT_MAXDELTA 0
#define DEFAULT_ACCEL_MAXDELTA 0
#define DEFAULT_ACCEL_TYPE 0

#define DEFAULT_WHEEL_STEPS 18
#define DEFAULT_WHEEL_DELTA (360 / DEFAULT_WHEEL_STEPS)

#define MOUSE_CHG_DTR 0x80000000
#define MOUSE_DTR_HIGH 0x40000000
#define MOUSE_CHG_RTS 0x20000000
#define MOUSE_RTS_HIGH 0x10000000

#define MOUSE_TYPE_MASK 0xffff

#define MOUSE_LEFTBUTTON 4
#define MOUSE_MIDDLEBUTTON 2
#define MOUSE_RIGHTBUTTON 1
#define MOUSE_FOURTHBUTTON 8
#define MOUSE_FIFTHBUTTON 16
#define MOUSE_SIXTHBUTTON 32
#define MOUSE_RESETBUTTON 64

#define MOUSE_XDIM 1
#define MOUSE_YDIM 2
#define MOUSE_ZDIM 4
#define MOUSE_RXDIM 8
#define MOUSE_RYDIM 16
#define MOUSE_RZDIM 32
#define MOUSE_2DIM 3
#define MOUSE_3DIM 7
#define MOUSE_6DIM 63

#define MOUSE_DEFAULTSAMPLERATE 150

int mouse_init(char *dev, int type, int samplerate);
int mouse_init_return_fd(char *dev, int type, int samplerate);
// As in svgalib, a handler is void (*)(int button, int dx, int dy, int dz, int drx, int dry, int drz) passed as void *.
typedef void *__mouse_handler;
void mouse_seteventhandler(__mouse_handler handler);
void mouse_close(void);
int mouse_update(void);
void mouse_waitforupdate(void);

#define MOUSE_NOWRAP 0
#define MOUSE_WRAPX 1
#define MOUSE_WRAPY 2
#define MOUSE_WRAPZ 4
#define MOUSE_WRAPRX 8
#define MOUSE_WRAPRY 16
#define MOUSE_WRAPRZ 32
#define MOUSE_WRAP 63
#define MOUSE_ROT_COORDS 196
#define MOUSE_ROT_INFINITESIMAL 0
#define MOUSE_ROT_RX_RY_RZ 64
#define MOUSE_ROT_ZXZ 128
#define MOUSE_ROT_YPR 196

void mouse_setdefaulteventhandler(void);
void mouse_setposition(int x, int y);
void mouse_setposition_6d(int x, int y, int z, int rx, int ry, int rz, int dim_mask);
void mouse_setxrange(int x1, int x2);
void mouse_setyrange(int y1, int y2);
void mouse_setrange_6d(int x1, int x2, int y1, int y2, int z1, int z2, int rx1, int rx2, int ry1, int ry2, int rz1,
                       int rz2, int dim_mask);
void mouse_setscale(int s);
void mouse_setwrap(int w);

int mouse_getx(void);
int mouse_gety(void);
void mouse_getposition_6d(int *x, int *y, int *z, int *rx, int *ry, int *rz);
int mouse_getbutton(void);

#define MOUSE_CAPS 1024
#define MOUSE_WANTCAPS 0x002b0042
#define MOUSE_GOTCAPS 0x0042ffd4

#define MOUSE_INFO_WHEEL 0x00000001

struct MouseCaps {
    int key;
    int buttons;
    int axes;
    int info;
    int reserved0;
    int reserved1;
};

static inline int
mouse_getcaps(struct MouseCaps *caps) {
    mouse_setposition_6d(MOUSE_WANTCAPS, 0, 0, 0, 0, 0, MOUSE_CAPS);
    mouse_getposition_6d(&(caps->key), &(caps->buttons), &(caps->axes), &(caps->info), &(caps->reserved0),
                         &(caps->reserved1));
    return ((caps->key == MOUSE_GOTCAPS) ? (0) : (-1));
}

#endif
