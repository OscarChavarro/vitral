#include <alloc.h>
#include <graphics.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "vsdk/toolkit/gui/framebuffer/bgi/BgiModes.h"

// A row of the mode table: driver file (without .BGI), Turbo C++ driver number (0 for a user driver installed with
// installuserdriver()), driver mode, size and depth.
class BgiModeRow {
public:
    const char *name;
    const char *driver;
    int builtin;
    int mode;
    int width;
    int height;
    int bits;
};

static const BgiModeRow TABLE[] = {
    {"G640x200x2", "CGA", CGA, CGAHI, 640, 200, 1},
    {"G640x480x2", "CGA", MCGA, MCGAHI, 640, 480, 1},
    {"G640x200x16", "EGAVGA", VGA, VGALO, 640, 200, 4},
    {"G640x350x16", "EGAVGA", VGA, VGAMED, 640, 350, 4},
    {"G640x480x16", "EGAVGA", VGA, VGAHI, 640, 480, 4},
    {"G800x600x16", "SVGA16", 0, 4, 800, 600, 4},
    {"G1024x768x16", "SVGA16", 0, 5, 1024, 768, 4},
    {"G1280x1024x16", "SVGA16", 0, 6, 1280, 1024, 4},
    {"G320x200x256", "SVGA256", 0, 0, 320, 200, 8},
    {"G640x350x256", "SVGA256", 0, 5, 640, 350, 8},
    {"G640x400x256", "SVGA256", 0, 1, 640, 400, 8},
    {"G640x480x256", "SVGA256", 0, 2, 640, 480, 8},
    {"G800x600x256", "SVGA256", 0, 3, 800, 600, 8},
    {"G1024x768x256", "SVGA256", 0, 4, 1024, 768, 8},
    {"G1280x1024x256", "SVGA256", 0, 6, 1280, 1024, 8},
    {"G320x200x64K", "SVGA64K", 0, 0, 320, 200, 16},
    {"G640x350x64K", "SVGA64K", 0, 1, 640, 350, 16},
    {"G640x400x64K", "SVGA64K", 0, 2, 640, 400, 16},
    {"G640x480x64K", "SVGA64K", 0, 3, 640, 480, 16},
    {"G800x600x64K", "SVGA64K", 0, 4, 800, 600, 16},
    {"G1024x768x64K", "SVGA64K", 0, 5, 1024, 768, 16},
    {"G1280x1024x64K", "SVGA64K", 0, 6, 1280, 1024, 16},
    {"G320x200x16M", "SVGATC", 0, 0, 320, 200, 24},
    {"G640x350x16M", "SVGATC", 0, 1, 640, 350, 24},
    {"G640x400x16M", "SVGATC", 0, 2, 640, 400, 24},
    {"G640x480x16M", "SVGATC", 0, 3, 640, 480, 24},
    {"G800x600x16M", "SVGATC", 0, 4, 800, 600, 24},
    {"G1024x768x16M", "SVGATC", 0, 5, 1024, 768, 24},
    {"G1280x1024x16M", "SVGATC", 0, 6, 1280, 1024, 24},
};

#define TABLE_SIZE ((int)(sizeof(TABLE) / sizeof(TABLE[0])))

// The user drivers, installed once: installuserdriver() only adds to a table of the BGI kernel.
static const char *USER_DRIVERS[] = {"SVGA16", "SVGA256", "SVGA64K", "SVGATC"};
#define USER_DRIVER_COUNT 4
static int userDriverNumbers[USER_DRIVER_COUNT] = {-1, -1, -1, -1};
static int requestedMode = 0;

// The graphics buffer of the BGI kernel, through the allocation hooks of Turbo C++. A failed initgraph() keeps the
// buffer it allocated (the default 4 KB of setgraphbufsize()) and closegraph() does not release it either: the probe
// tries many modes that fail, and would lose it on each one. Those buffers are released here and remembered, as the
// kernel may free them itself later: that second release is ignored. The loaded driver is not released, as the
// kernel keeps using it.
enum { GRAPH_BLOCKS = 16, GRAPH_BUFFER_SIZE = 4096 };
static void far *graphBuffers[GRAPH_BLOCKS];
static void far *releasedBuffers[GRAPH_BLOCKS];

static void
forget(void far **blocks, void far *block)
{
    int i;
    for ( i = 0; i < GRAPH_BLOCKS; i++ ) {
        if ( blocks[i] == block ) {
            blocks[i] = NULL;
        }
    }
}

static void
remember(void far **blocks, void far *block)
{
    int i;
    for ( i = 0; i < GRAPH_BLOCKS; i++ ) {
        if ( blocks[i] == NULL ) {
            blocks[i] = block;
            return;
        }
    }
}

static BOOLEAN
known(void far **blocks, void far *block)
{
    int i;
    for ( i = 0; i < GRAPH_BLOCKS; i++ ) {
        if ( blocks[i] == block ) {
            return TRUE;
        }
    }
    return FALSE;
}

void far * far
_graphgetmem(unsigned size)
{
    void far *block = farmalloc((unsigned long)size);
    if ( block != NULL ) {
        // The heap may give an address released before to a new block of the kernel.
        forget(releasedBuffers, block);
        if ( size == GRAPH_BUFFER_SIZE ) {
            remember(graphBuffers, block);
        }
    }
    return block;
}

void far
_graphfreemem(void far *block, unsigned)
{
    if ( known(releasedBuffers, block) ) {
        forget(releasedBuffers, block);
        return;
    }
    forget(graphBuffers, block);
    farfree(block);
}

// Releases the graphics buffer that a failed initgraph() left allocated.
static void
releaseGraphMemory()
{
    int i;
    for ( i = 0; i < GRAPH_BLOCKS; i++ ) {
        if ( graphBuffers[i] != NULL ) {
            farfree(graphBuffers[i]);
            remember(releasedBuffers, graphBuffers[i]);
            graphBuffers[i] = NULL;
        }
    }
}

// The "detection" of the user drivers: the mode asked for, as the drivers do not detect the card themselves.
static int huge
requested(void)
{
    return requestedMode;
}

BgiModes::BgiModes(const char *driverPath) : total(0), graphics(FALSE), loadedDriver(-1)
{
    const char *fromEnvironment = getenv("BGIPATH");
    const char *chosen = driverPath ? driverPath : fromEnvironment ? fromEnvironment : "C:\\TURBOC\\BGI";
    strncpy(path, chosen, sizeof(path) - 1);
    path[sizeof(path) - 1] = 0;
    shown.mode = -1;
    shown.width = 0;
    shown.height = 0;
    shown.colors = 0;
    shown.bitsPerPixel = 0;
    int mode;
    for ( mode = 0; mode < TABLE_SIZE && total < MAX_MODES; mode++ ) {
        modes[total++] = mode;
    }
}

BgiModes::~BgiModes()
{
    if ( graphics ) {
        leave();
    }
}

int
BgiModes::tableSize()
{
    return TABLE_SIZE;
}

long
BgiModes::area(int mode)
{
    return (long)TABLE[mode].width * (long)TABLE[mode].height;
}

int
BgiModes::driverNumber(int mode)
{
    const BgiModeRow &row = TABLE[mode];
    if ( row.builtin ) {
        return row.builtin;
    }
    int i;
    for ( i = 0; i < USER_DRIVER_COUNT; i++ ) {
        if ( strcmp(USER_DRIVERS[i], row.driver) == 0 ) {
            if ( userDriverNumbers[i] < 0 ) {
                userDriverNumbers[i] = installuserdriver((char *)USER_DRIVERS[i], requested);
            }
            return userDriverNumbers[i];
        }
    }
    return -1;
}

// Sets a mode: with setgraphmode() when its driver is the one loaded, otherwise with initgraph(), which loads the
// driver file. The mode is taken only with the size of the table.
BOOLEAN
BgiModes::set(int mode)
{
    int driver = driverNumber(mode);
    if ( driver < 0 ) {
        return FALSE;
    }
    if ( graphics && driver == loadedDriver ) {
        setgraphmode(TABLE[mode].mode);
    }
    else {
        if ( graphics ) {
            closegraph();
            graphics = FALSE;
        }
        int graphDriver = driver;
        int graphMode = TABLE[mode].mode;
        requestedMode = graphMode;
        initgraph(&graphDriver, &graphMode, path);
        if ( graphresult() != grOk ) {
            releaseGraphMemory();
            loadedDriver = -1;
            return FALSE;
        }
        graphics = TRUE;
        loadedDriver = driver;
    }
    if ( graphresult() != grOk || getmaxx() + 1 != TABLE[mode].width || getmaxy() + 1 != TABLE[mode].height ) {
        return FALSE;
    }
    return TRUE;
}

void
BgiModes::probe()
{
    int kept = 0;
    int i;
    for ( i = 0; i < total; i++ ) {
        int mode = modes[i];
        // Each mode through initgraph(): the drivers check that the card has the mode when they are installed, not
        // in setgraphmode() (SVGA256 accepts 640x350 there on cards without it, and shows garbage).
        leave();
        if ( set(mode) ) {
            int bits = TABLE[mode].bits;
            long size = area(mode);
            int at = kept++;
            while ( at > 0 ) {
                int before = modes[at - 1];
                int beforeBits = TABLE[before].bits;
                long beforeSize = area(before);
                BOOLEAN sameBits = beforeBits == bits;
                BOOLEAN after = beforeBits < bits ||
                                (sameBits && (beforeSize < size ||
                                              (beforeSize == size && TABLE[before].width <= TABLE[mode].width)));
                if ( after ) {
                    break;
                }
                modes[at] = before;
                at--;
            }
            modes[at] = mode;
        }
    }
    total = kept;
    if ( graphics ) {
        closegraph();
        graphics = FALSE;
        loadedDriver = -1;
    }
    shown.mode = -1;
}

int
BgiModes::indexOf(int mode) const
{
    int i;
    for ( i = 0; i < total; i++ ) {
        if ( modes[i] == mode ) {
            return i;
        }
    }
    return -1;
}

void
BgiModes::keepDepths(const int *depths, int count)
{
    int kept = 0;
    int i;
    for ( i = 0; i < total; i++ ) {
        int bits = bitsPerPixel(modes[i]);
        BOOLEAN wanted = FALSE;
        int k;
        for ( k = 0; k < count && !wanted; k++ ) {
            wanted = depths[k] == bits;
        }
        if ( wanted ) {
            modes[kept++] = modes[i];
        }
    }
    total = kept;
}

void
BgiModes::header(char *out, int size)
{
    snprintf(out, size, "%3s  %-16s %3s %9s  %-10s", "#", "MODE", "BPP", "COLORS", "DRIVER");
}

void
BgiModes::row(int mode, char *out, int size)
{
    char driver[24];
    origin(mode, driver, sizeof(driver));
    long colors = TABLE[mode].bits >= 24 ? 16777216L : 1L << TABLE[mode].bits;
    snprintf(out, size, "%3d  %-16s %3d %9ld  %-10s", mode, name(mode), bitsPerPixel(mode), colors, driver);
}

void
BgiModes::print(const char *title) const
{
    char line[ROW_WIDTH + 1];
    printf("%s\n\n", title);
    header(line, sizeof(line));
    printf("%s\n", line);
    int i;
    for ( i = 0; i < total; i++ ) {
        row(modes[i], line, sizeof(line));
        printf("%s\n", line);
    }
}

const char *
BgiModes::name(int mode)
{
    return mode >= 0 && mode < TABLE_SIZE ? TABLE[mode].name : "?";
}

int
BgiModes::bitsPerPixel(int mode)
{
    return mode >= 0 && mode < TABLE_SIZE ? TABLE[mode].bits : 0;
}

int
BgiModes::width(int mode)
{
    return mode >= 0 && mode < TABLE_SIZE ? TABLE[mode].width : 0;
}

int
BgiModes::height(int mode)
{
    return mode >= 0 && mode < TABLE_SIZE ? TABLE[mode].height : 0;
}

void
BgiModes::origin(int mode, char *out, int size)
{
    if ( mode < 0 || mode >= TABLE_SIZE ) {
        snprintf(out, size, "-");
        return;
    }
    snprintf(out, size, "%s %d", TABLE[mode].driver, TABLE[mode].mode);
}

static char
lower(char c)
{
    return c >= 'A' && c <= 'Z' ? (char)(c - 'A' + 'a') : c;
}

static BOOLEAN
sameName(const char *a, const char *b)
{
    while ( *a && lower(*a) == lower(*b) ) {
        a++;
        b++;
    }
    return *a == 0 && *b == 0;
}

int
BgiModes::lookup(const char *text)
{
    const char *c = text;
    int number = 0;
    while ( *c >= '0' && *c <= '9' && number < TABLE_SIZE ) {
        number = number * 10 + (*c++ - '0');
    }
    if ( c != text && *c == 0 ) {
        return number < TABLE_SIZE ? number : -1;
    }
    BOOLEAN sizeFirst = c != text;
    int mode;
    for ( mode = 0; mode < TABLE_SIZE; mode++ ) {
        if ( sameName(text, sizeFirst ? TABLE[mode].name + 1 : TABLE[mode].name) ) {
            return mode;
        }
    }
    return -1;
}

// Sets a mode. When it cannot be set, the mode shown before is set again, so that the screen stays as it was.
BOOLEAN
BgiModes::enter(int mode)
{
    if ( mode < 0 || mode >= TABLE_SIZE ) {
        return FALSE;
    }
    if ( !set(mode) ) {
        if ( shown.mode >= 0 && !set(shown.mode) ) {
            leave();
        }
        return FALSE;
    }
    shown.mode = mode;
    shown.width = TABLE[mode].width;
    shown.height = TABLE[mode].height;
    shown.bitsPerPixel = TABLE[mode].bits;
    shown.colors = TABLE[mode].bits >= 24 ? 16777216L : 1L << TABLE[mode].bits;
    return TRUE;
}

BgiModes::Change
BgiModes::step(int step)
{
    if ( total == 0 ) {
        return ChangeNone;
    }
    int at = indexOf(shown.mode);
    if ( at < 0 ) {
        at = total - 1;
    }
    int next = modes[((at + step) % total + total) % total];
    if ( next == shown.mode ) {
        return ChangeNone;
    }
    return enter(next) ? ChangeDone : ChangeFailed;
}

void
BgiModes::leave()
{
    if ( graphics ) {
        closegraph();
    }
    graphics = FALSE;
    loadedDriver = -1;
    shown.mode = -1;
}
