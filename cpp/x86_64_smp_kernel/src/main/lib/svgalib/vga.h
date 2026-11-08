#ifndef __VGA__
#define __VGA__

enum {
    TEXT = 0,
    G320x200x16 = 1, G640x200x16 = 2, G640x350x16 = 3, G640x480x16 = 4,
    G320x200x256 = 5, G320x240x256 = 6, G320x400x256 = 7, G360x480x256 = 8,
    G640x480x2 = 9, G640x480x256 = 10, G800x600x256 = 11, G1024x768x256 = 12, G1280x1024x256 = 13,
    G320x200x32K = 14, G320x200x64K = 15, G320x200x16M = 16,
    G640x480x32K = 17, G640x480x64K = 18, G640x480x16M = 19,
    G800x600x32K = 20, G800x600x64K = 21, G800x600x16M = 22,
    G1024x768x32K = 23, G1024x768x64K = 24, G1024x768x16M = 25,
    G1280x1024x32K = 26, G1280x1024x64K = 27, G1280x1024x16M = 28,
    G800x600x16 = 29, G1024x768x16 = 30, G1280x1024x16 = 31, G720x348x2 = 32,
    G320x200x16M32 = 33, G640x480x16M32 = 34, G800x600x16M32 = 35, G1024x768x16M32 = 36, G1280x1024x16M32 = 37,
    G1152x864x16 = 38, G1152x864x256 = 39, G1152x864x32K = 40, G1152x864x64K = 41, G1152x864x16M = 42,
    G1152x864x16M32 = 43,
    G1600x1200x16 = 44, G1600x1200x256 = 45, G1600x1200x32K = 46, G1600x1200x64K = 47, G1600x1200x16M = 48,
    G1600x1200x16M32 = 49,
    G320x240x256V = 50, G320x240x32K = 51, G320x240x64K = 52, G320x240x16M = 53, G320x240x16M32 = 54,
    G400x300x256 = 55, G400x300x32K = 56, G400x300x64K = 57, G400x300x16M = 58, G400x300x16M32 = 59,
    G512x384x256 = 60, G512x384x32K = 61, G512x384x64K = 62, G512x384x16M = 63, G512x384x16M32 = 64,
    G960x720x256 = 65, G960x720x32K = 66, G960x720x64K = 67, G960x720x16M = 68, G960x720x16M32 = 69,
    G1920x1440x256 = 70, G1920x1440x32K = 71, G1920x1440x64K = 72, G1920x1440x16M = 73, G1920x1440x16M32 = 74,
    G320x400x256V = 75, G320x400x32K = 76, G320x400x64K = 77, G320x400x16M = 78, G320x400x16M32 = 79,
    G640x400x256 = 80, G640x400x32K = 81, G640x400x64K = 82, G640x400x16M = 83, G640x400x16M32 = 84,
    G320x480x256 = 85, G320x480x32K = 86, G320x480x64K = 87, G320x480x16M = 88, G320x480x16M32 = 89,
    G720x540x256 = 90, G720x540x32K = 91, G720x540x64K = 92, G720x540x16M = 93, G720x540x16M32 = 94,
    G848x480x256 = 95, G848x480x32K = 96, G848x480x64K = 97, G848x480x16M = 98, G848x480x16M32 = 99,
    G1072x600x256 = 100, G1072x600x32K = 101, G1072x600x64K = 102, G1072x600x16M = 103, G1072x600x16M32 = 104,
    G1280x720x256 = 105, G1280x720x32K = 106, G1280x720x64K = 107, G1280x720x16M = 108, G1280x720x16M32 = 109,
    G1360x768x256 = 110, G1360x768x32K = 111, G1360x768x64K = 112, G1360x768x16M = 113, G1360x768x16M32 = 114,
    G1800x1012x256 = 115, G1800x1012x32K = 116, G1800x1012x64K = 117, G1800x1012x16M = 118, G1800x1012x16M32 = 119,
    G1920x1080x256 = 120, G1920x1080x32K = 121, G1920x1080x64K = 122, G1920x1080x16M = 123, G1920x1080x16M32 = 124,
    G2048x1152x256 = 125, G2048x1152x32K = 126, G2048x1152x64K = 127, G2048x1152x16M = 128, G2048x1152x16M32 = 129,
    G2048x1536x256 = 130, G2048x1536x32K = 131, G2048x1536x64K = 132, G2048x1536x16M = 133, G2048x1536x16M32 = 134,
    G512x480x256 = 135, G512x480x32K = 136, G512x480x64K = 137, G512x480x16M = 138, G512x480x16M32 = 139,
    G400x600x256 = 140, G400x600x32K = 141, G400x600x64K = 142, G400x600x16M = 143, G400x600x16M32 = 144,
    G400x300x256X = 145, G320x200x256V = 146,
    __GLASTMODE = G320x200x256V
};

#ifdef SVGALIB_KERNEL_MODES
enum {
    G800x600x2 = __GLASTMODE + 1,
    G1024x768x2,
    G1280x1024x2,
    G1600x1200x2,
    G1920x1080x2,
    G1920x1080x16
};
#endif

#define GLASTMODE vga_lastmodenumber()

enum { HAVE_BITBLIT = 1, HAVE_FILLBLIT = 2, HAVE_IMAGEBLIT = 4, HAVE_HLINELISTBLIT = 8, HAVE_BLITWAIT = 16 };

enum {
    HAVE_RWPAGE = 1,
    IS_INTERLACED = 2,
    IS_MODEX = 4,
    IS_DYNAMICMODE = 8,
    CAPABLE_LINEAR = 16,
    IS_LINEAR = 32,
    EXT_INFO_AVAILABLE = 64,
    RGB_MISORDERED = 128,
    HAVE_EXT_SET = 256
};

typedef struct {
    int width;
    int height;
    int bytesperpixel;
    int colors;
    int linewidth;
    int maxlogicalwidth;
    int startaddressrange;
    int maxpixels;
    int haveblit;
    int flags;
    int chiptype;
    int memory;
    int linewidth_unit;
    char *linear_aperture;
    int aperture_size;
    void (*set_aperture_page)(int page);
    void *extensions;
} vga_modeinfo;

int
vga_init();
int
vga_setmode(int mode);
int
vga_hasmode(int mode);
vga_modeinfo *
vga_getmodeinfo(int mode);
int
vga_lastmodenumber();
char *
vga_getmodename(int mode);
int
vga_getmodenumber(char *name);
int
vga_getcurrentmode();
int
vga_getdefaultmode();
int
vga_getxdim();
int
vga_getydim();
int
vga_getcolors();
int
vga_white();
int
vga_setcolor(int color);
int
vga_setrgbcolor(int r, int g, int b);
int
vga_drawpixel(int x, int y);
int
vga_drawline(int x1, int y1, int x2, int y2);
int
vga_drawscansegment(unsigned char *colors, int x, int y, int length);
int
vga_getpixel(int x, int y);
int
vga_setpalvec(int start, int num, int *pal);
int
vga_getkey();
void
vga_gettextfont(void *font);

// Mouse support: the mouse_* functions are in <vgamouse.h>. vga_setmousesupport(1) before vga_init() makes vga_init()
// open the configured mouse and vga_setmode() fit its range to every graphics mode.
int
vga_getmousetype();
void
vga_setmousesupport(int s);

// svgalib's extension and acceleration calls. This kernel offers solid fills and screen to screen copies in every
// graphics mode: done by the 2D engine when the SVGA driver has one and the `nvidia` program selects it, by the CPU
// otherwise.
#define VGA_EXT_AVAILABLE 0
#define VGA_EXT_SET 1
#define VGA_EXT_CLEAR 2
#define VGA_EXT_RESET 3
#define VGA_AVAIL_SET 0
#define VGA_AVAIL_ACCEL 1
#define VGA_AVAIL_FLAGS 2

#define ACCEL_FILLBOX 1
#define ACCEL_SCREENCOPY 2
#define ACCEL_PUTIMAGE 3
#define ACCEL_DRAWLINE 4
#define ACCEL_SETFGCOLOR 5
#define ACCEL_SETBGCOLOR 6
#define ACCEL_SETTRANSPARENCY 7
#define ACCEL_SETRASTEROP 8
#define ACCEL_PUTBITMAP 9
#define ACCEL_SCREENCOPYBITMAP 10
#define ACCEL_DRAWHLINELIST 11
#define ACCEL_SETMODE 12
#define ACCEL_SYNC 13

#define ACCELFLAG_FILLBOX 0x1
#define ACCELFLAG_SCREENCOPY 0x2
#define ACCELFLAG_PUTIMAGE 0x4
#define ACCELFLAG_DRAWLINE 0x8
#define ACCELFLAG_SETFGCOLOR 0x10
#define ACCELFLAG_SETBGCOLOR 0x20
#define ACCELFLAG_SETTRANSPARENCY 0x40
#define ACCELFLAG_SETRASTEROP 0x80
#define ACCELFLAG_PUTBITMAP 0x100
#define ACCELFLAG_SCREENCOPYBITMAP 0x200
#define ACCELFLAG_DRAWHLINELIST 0x400
#define ACCELFLAG_SETMODE 0x800
#define ACCELFLAG_SYNC 0x1000

int
vga_ext_set(unsigned what, ...);
// ACCEL_FILLBOX x, y, width, height in the ACCEL_SETFGCOLOR colour; ACCEL_SCREENCOPY fromX, fromY, toX, toY, width,
// height; ACCEL_SETFGCOLOR colour; ACCEL_SYNC. 0, or -1 for an operation it lacks.
int
vga_accel(unsigned operation, ...);

struct fd_set;
struct timeval;

// Only in this kernel, where programs share the memory of the kernel (the Linux svgalib lacks them). The pixels of the
// console, one or more bytes each and row after row: what is drawn on them reaches the screen with vga_presentarea(),
// without another copy. The pointer changes with vga_setmode(), and is null in text mode.
unsigned char *
vga_getscreenpixels();
void
vga_presentarea(int left, int top, int right, int bottom);
// count pixels on the screen only: the console's pixels stay as they are.
void
vga_drawoverlaysegment(const unsigned char *colors, int x, int y, int count);
// What a console switch paints over the console's pixels: see VirtualConsole::setOverlay(). Null rows for nothing.
void
vga_setoverlay(int left, int top, int width, int height, const char *const *rows, unsigned fill, unsigned outline);

#define VGA_MOUSEEVENT 1
#define VGA_KEYEVENT 2

// Waits (select(2) style, any pointer may be null) for the mouse and/or a key; mouse events are already handled
// (mouse_update()) when it returns. Returns the VGA_*EVENT bits that happened, or -1 with errno set.
int
vga_waitevent(int which, fd_set *in, fd_set *out, fd_set *except, timeval *timeout);

#endif
