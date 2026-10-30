#include "drivers/video/svgalib/vga.h"
#include "drivers/video/svgalib/SvgalibMouse.h"
#include "drivers/tty/Tty.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "syscalls/Syscalls.h"

static const class {
public:
    int mode;
    VgaMode::Id id;
} MODES[] = {
    {TEXT, VgaMode::Id::Text80x25},
    {G320x200x16, VgaMode::Id::G320x200x16},
    {G640x200x16, VgaMode::Id::G640x200x16},
    {G640x350x16, VgaMode::Id::G640x350x16},
    {G640x480x16, VgaMode::Id::G640x480x16},
    {G320x200x256, VgaMode::Id::G320x200x256},
    {G320x240x256, VgaMode::Id::G320x240x256},
    {G320x400x256, VgaMode::Id::G320x400x256},
    {G360x480x256, VgaMode::Id::G360x480x256},
    {G720x348x2, VgaMode::Id::G720x348x2},
    {G400x300x256X, VgaMode::Id::G400x300x256},
    {G640x480x2, VgaMode::Id::G640x480x2},
    {G800x600x16, VgaMode::Id::S800x600x4},
    {G800x600x256, VgaMode::Id::S800x600x8}, {G800x600x64K, VgaMode::Id::S800x600x16},
    {G800x600x16M, VgaMode::Id::S800x600x24},
    {G1024x768x16, VgaMode::Id::S1024x768x4},
    {G1024x768x256, VgaMode::Id::S1024x768x8}, {G1024x768x64K, VgaMode::Id::S1024x768x16},
    {G1024x768x16M, VgaMode::Id::S1024x768x24},
    {G1280x1024x16, VgaMode::Id::S1280x1024x4},
    {G1280x1024x256, VgaMode::Id::S1280x1024x8}, {G1280x1024x64K, VgaMode::Id::S1280x1024x16},
    {G1280x1024x16M, VgaMode::Id::S1280x1024x24},
    {G1600x1200x16, VgaMode::Id::S1600x1200x4},
    {G1600x1200x256, VgaMode::Id::S1600x1200x8}, {G1600x1200x64K, VgaMode::Id::S1600x1200x16},
    {G1600x1200x16M, VgaMode::Id::S1600x1200x24},
    {G1920x1080x256, VgaMode::Id::S1920x1080x8}, {G1920x1080x64K, VgaMode::Id::S1920x1080x16},
    {G1920x1080x16M, VgaMode::Id::S1920x1080x24},
#ifdef SVGALIB_KERNEL_MODES
    // Modes the kernel adds after svgalib's last one (vga.h): monochrome SVGA and 16-colour 1920x1080.
    {G800x600x2, VgaMode::Id::S800x600x1},
    {G1024x768x2, VgaMode::Id::S1024x768x1},
    {G1280x1024x2, VgaMode::Id::S1280x1024x1},
    {G1600x1200x2, VgaMode::Id::S1600x1200x1},
    {G1920x1080x2, VgaMode::Id::S1920x1080x1},
    {G1920x1080x16, VgaMode::Id::S1920x1080x4},
#endif
};

static constexpr int MODE_COUNT = sizeof(MODES) / sizeof(MODES[0]);

static const char *const MODE_NAMES[] = {
    "TEXT", "G320x200x16", "G640x200x16", "G640x350x16", "G640x480x16", "G320x200x256", "G320x240x256",
    "G320x400x256", "G360x480x256", "G640x480x2", "G640x480x256", "G800x600x256", "G1024x768x256",
    "G1280x1024x256", "G320x200x32K", "G320x200x64K", "G320x200x16M", "G640x480x32K", "G640x480x64K",
    "G640x480x16M", "G800x600x32K", "G800x600x64K", "G800x600x16M", "G1024x768x32K", "G1024x768x64K",
    "G1024x768x16M", "G1280x1024x32K", "G1280x1024x64K", "G1280x1024x16M", "G800x600x16", "G1024x768x16",
    "G1280x1024x16", "G720x348x2", "G320x200x16M32", "G640x480x16M32", "G800x600x16M32", "G1024x768x16M32",
    "G1280x1024x16M32", "G1152x864x16", "G1152x864x256", "G1152x864x32K", "G1152x864x64K", "G1152x864x16M",
    "G1152x864x16M32", "G1600x1200x16", "G1600x1200x256", "G1600x1200x32K", "G1600x1200x64K", "G1600x1200x16M",
    "G1600x1200x16M32", "G320x240x256V", "G320x240x32K", "G320x240x64K", "G320x240x16M", "G320x240x16M32",
    "G400x300x256", "G400x300x32K", "G400x300x64K", "G400x300x16M", "G400x300x16M32", "G512x384x256",
    "G512x384x32K", "G512x384x64K", "G512x384x16M", "G512x384x16M32", "G960x720x256", "G960x720x32K",
    "G960x720x64K", "G960x720x16M", "G960x720x16M32", "G1920x1440x256", "G1920x1440x32K", "G1920x1440x64K",
    "G1920x1440x16M", "G1920x1440x16M32", "G320x400x256V", "G320x400x32K", "G320x400x64K", "G320x400x16M",
    "G320x400x16M32", "G640x400x256", "G640x400x32K", "G640x400x64K", "G640x400x16M", "G640x400x16M32",
    "G320x480x256", "G320x480x32K", "G320x480x64K", "G320x480x16M", "G320x480x16M32", "G720x540x256",
    "G720x540x32K", "G720x540x64K", "G720x540x16M", "G720x540x16M32", "G848x480x256", "G848x480x32K",
    "G848x480x64K", "G848x480x16M", "G848x480x16M32", "G1072x600x256", "G1072x600x32K", "G1072x600x64K",
    "G1072x600x16M", "G1072x600x16M32", "G1280x720x256", "G1280x720x32K", "G1280x720x64K", "G1280x720x16M",
    "G1280x720x16M32", "G1360x768x256", "G1360x768x32K", "G1360x768x64K", "G1360x768x16M", "G1360x768x16M32",
    "G1800x1012x256", "G1800x1012x32K", "G1800x1012x64K", "G1800x1012x16M", "G1800x1012x16M32", "G1920x1080x256",
    "G1920x1080x32K", "G1920x1080x64K", "G1920x1080x16M", "G1920x1080x16M32", "G2048x1152x256", "G2048x1152x32K",
    "G2048x1152x64K", "G2048x1152x16M", "G2048x1152x16M32", "G2048x1536x256", "G2048x1536x32K", "G2048x1536x64K",
    "G2048x1536x16M", "G2048x1536x16M32", "G512x480x256", "G512x480x32K", "G512x480x64K", "G512x480x16M",
    "G512x480x16M32", "G400x600x256", "G400x600x32K", "G400x600x64K", "G400x600x16M", "G400x600x16M32",
    "G400x300x256X", "G320x200x256V",
#ifdef SVGALIB_KERNEL_MODES
    "G800x600x2", "G1024x768x2", "G1280x1024x2", "G1600x1200x2", "G1920x1080x2", "G1920x1080x16",
#endif
};

static constexpr int LAST_MODE = sizeof(MODE_NAMES) / sizeof(MODE_NAMES[0]) - 1;
#ifdef SVGALIB_KERNEL_MODES
static_assert(LAST_MODE == G1920x1080x16, "a name for every mode number");
#else
static_assert(LAST_MODE == __GLASTMODE, "a name for every mode number");
#endif

static constexpr int SVGALIB_VERSION = 0x1925;
static constexpr int MODE_NAME_LENGTH = 20;

static uint32_t currentColor[VirtualConsole::COUNT];
// Like svgalib, one buffer that every vga_getmodeinfo() call overwrites (one per console: per-process state).
static vga_modeinfo modeInfos[VirtualConsole::COUNT];

// The terminal modes svgalib switches between: the one found at the first vga_setmode() (restored in TEXT) and the
// one it derives for graphics (no line editing, no echo, one byte at a time).
static bool termiosSaved[VirtualConsole::COUNT];
static Tty::Mode textTermios[VirtualConsole::COUNT];
static Tty::Mode graphTermios[VirtualConsole::COUNT];

static bool
lookup(int mode, VgaMode::Id &id) {
    for (int i = 0; i < MODE_COUNT; i++) {
        if (MODES[i].mode == mode) {
            id = MODES[i].id;
            return true;
        }
    }
    return false;
}

static unsigned
myConsole() { return Syscalls::console() % VirtualConsole::COUNT; }

static int
parseNumber(const char *&text) {
    int n = 0;
    while (*text >= '0' && *text <= '9') {
        n = n * 10 + (*text++ - '0');
    }
    return n;
}

static void
describeByName(int mode, vga_modeinfo &out) {
    out = vga_modeinfo{};
    if (mode == TEXT) {
        out.width = 80;
        out.height = 25;
        out.colors = 16;
        out.linewidth = 160;
        return;
    }
    const char *c = MODE_NAMES[mode] + 1;
    out.width = parseNumber(c);
    c++;
    out.height = parseNumber(c);
    c++;
    int colors = parseNumber(c);
    if (*c == 'K') {
        out.colors = colors * 1024;
        out.bytesperpixel = 2;
    } else if (*c == 'M') {
        out.colors = 1 << 24;
        out.bytesperpixel = c[1] == '3' ? 4 : 3;
    } else if (colors == 256) {
        out.colors = 256;
        bool modeX = mode == G320x240x256 || mode == G320x400x256 || mode == G360x480x256 || mode == G400x300x256X;
        if (modeX) {
            out.flags = IS_MODEX;
        }
        out.bytesperpixel = modeX ? 0 : 1;
        out.linewidth = modeX ? out.width / 4 : out.width;
    } else {
        out.colors = colors;
        out.linewidth = out.width / 8;
    }
    if (out.bytesperpixel > 1) {
        out.linewidth = out.width * out.bytesperpixel;
    }
    if (mode > __GLASTMODE) {
        out.flags |= IS_DYNAMICMODE;
    }
}

static void
saveTermios(unsigned console) {
    Tty::Mode &text = textTermios[console];
    Tty::getMode(text);
    Tty::Mode &graph = graphTermios[console];
    graph = text;
    graph.inputFlags &= ~(Tty::BREAK_INTERRUPT | Tty::MARK_PARITY | Tty::PARITY_CHECK | Tty::UPPER_TO_LOWER |
                          Tty::OUTPUT_FLOW_CONTROL | Tty::INPUT_FLOW_CONTROL);
    graph.inputFlags |= Tty::IGNORE_BREAK | Tty::IGNORE_PARITY;
    graph.outputFlags &= ~Tty::NO_CR_AT_COLUMN_0;
    graph.localFlags &= ~(Tty::CANONICAL | Tty::ECHO_INPUT | Tty::ECHO_ERASE | Tty::ECHO_KILL | Tty::ECHO_NL |
                          Tty::NO_FLUSH);
    graph.localFlags |= Tty::SIGNALS;
    graph.controlChars[Tty::CC_MIN] = 1;
    graph.controlChars[Tty::CC_TIME] = 0;
    graph.controlChars[Tty::CC_SUSPEND] = 0;
    termiosSaved[console] = true;
}

int
vga_init() {
    termiosSaved[myConsole()] = false;
    SvgalibMouse::initialize();
    return 0;
}

int
vga_lastmodenumber() { return LAST_MODE; }

int
vga_hasmode(int mode) {
    VgaMode::Id id;
    return lookup(mode, id) && VgaMode::available(id) ? 1 : 0;
}

int
vga_setmode(int mode) {
    if (mode == -1) {
        return SVGALIB_VERSION;
    }
    unsigned console = myConsole();
    if (!termiosSaved[console]) {
        saveTermios(console);
    }
    VgaMode::Id id;
    if (!lookup(mode, id) || !VgaMode::available(id)) {
        return -1;
    }
    if (!VirtualConsole::setMode(Syscalls::console(), id)) {
        return -1;
    }
    if (mode != TEXT) {
        const VgaMode::Info &info = VgaMode::info(id);
        SvgalibMouse::fitToMode(info.width, info.height);
    }
    Tty::setMode(mode == TEXT ? textTermios[console] : graphTermios[console], false);
    return 0;
}

vga_modeinfo *
vga_getmodeinfo(int mode) {
    if (mode < 0 || mode > LAST_MODE) {
        return nullptr;
    }
    vga_modeinfo &out = modeInfos[myConsole()];
    describeByName(mode, out);
    VgaMode::Id id;
    if (lookup(mode, id)) {
        const VgaMode::Info &info = VgaMode::info(id);
        if (info.layout == VgaMode::Layout::Linear) {
            out.bytesperpixel = info.bytes;
            out.linewidth = info.width * info.pixelBytes();
            out.flags |= CAPABLE_LINEAR | IS_LINEAR;
        }
        if (info.graphics()) {
            out.maxpixels = info.width * info.height;
        }
    }
    out.maxlogicalwidth = out.linewidth;
    return &out;
}

// svgalib builds the name from the mode's size and colours, so the variants that only differ in a final V or X
// (G320x240x256V, G320x400x256V, G400x300x256X, G320x200x256V) get the name of the first mode they match.
char *
vga_getmodename(int mode) {
    static char names[VirtualConsole::COUNT][MODE_NAME_LENGTH];
    if (mode <= TEXT || mode > LAST_MODE) {
        return const_cast<char *>("");
    }
    char *out = names[myConsole()];
    int length = 0;
    for (const char *c = MODE_NAMES[mode]; *c && length < MODE_NAME_LENGTH - 1; c++) {
        out[length++] = *c;
    }
    if (mode <= __GLASTMODE && (out[length - 1] == 'V' || out[length - 1] == 'X')) {
        length--;
    }
    out[length] = 0;
    return out;
}

static char
lower(char c) { return c >= 'A' && c <= 'Z' ? (char)(c - 'A' + 'a') : c; }

static bool
sameText(const char *a, const char *b) {
    while (*a && lower(*a) == lower(*b)) {
        a++;
        b++;
    }
    return *a == 0 && *b == 0;
}

static bool
sameNumber(const char *text, int n) {
    char digits[12];
    int count = 0;
    do {
        digits[count++] = (char)('0' + n % 10);
        n /= 10;
    } while (n > 0);
    while (count > 0 && *text == digits[count - 1]) {
        text++;
        count--;
    }
    return count == 0 && *text == 0;
}

static void
writeError(const char *text) {
    size_t length = 0;
    while (text[length]) {
        length++;
    }
    Syscalls::write(2, text, length);
}

// As svgalib: a name of a graphics mode (any case) or its number, never TEXT; anything else but "PROMPT" is reported.
int
vga_getmodenumber(char *name) {
    if (!name) {
        return -1;
    }
    for (int mode = TEXT + 1; mode <= LAST_MODE; mode++) {
        if (sameText(name, vga_getmodename(mode))) {
            return mode;
        }
    }
    for (int mode = TEXT + 1; mode <= LAST_MODE; mode++) {
        if (sameNumber(name, mode)) {
            return mode;
        }
    }
    if (sameText(name, "PROMPT")) {
        return -1;
    }
    writeError("Invalid graphics mode '");
    writeError(name);
    writeError("'.\n");
    return -1;
}

int
vga_getcurrentmode() {
    VgaMode::Id id = VirtualConsole::mode(Syscalls::console());
    for (int i = 0; i < MODE_COUNT; i++) {
        if (MODES[i].id == id) {
            return MODES[i].mode;
        }
    }
    return TEXT;
}

int
vga_getdefaultmode() { return -1; }

int
vga_getxdim() { return VgaMode::info(VirtualConsole::mode(Syscalls::console())).width; }

int
vga_getydim() { return VgaMode::info(VirtualConsole::mode(Syscalls::console())).height; }

int
vga_getcolors() { return (int)VgaMode::info(VirtualConsole::mode(Syscalls::console())).colors; }

int
vga_white() {
    int colors = vga_getcolors();
    return colors <= 256 ? 15 : colors - 1;
}

// As svgalib: in 2 colour modes any colour but 0 is 15, and 16 colour modes keep the low 4 bits.
int
vga_setcolor(int color) {
    int colors = vga_getcolors();
    if (colors == 2 && color != 0) {
        color = 15;
    }
    if (colors <= 16) {
        color &= 0x0F;
    }
    currentColor[myConsole()] = (uint32_t)color;
    return 0;
}

// As svgalib: packs the colour for the 32K (RGB555), 64K (RGB565) and 16M modes and returns it; 0 in palette modes.
int
vga_setrgbcolor(int r, int g, int b) {
    int color;
    switch (VgaMode::info(VirtualConsole::mode(Syscalls::console())).colors) {
    case 1 << 15:
        color = (b >> 3) + ((g >> 3) << 5) + ((r >> 3) << 10);
        break;
    case 1 << 16:
        color = (b >> 3) + ((g >> 2) << 5) + ((r >> 3) << 11);
        break;
    case 1 << 24:
        color = b + (g << 8) + (r << 16);
        break;
    default:
        return 0;
    }
    currentColor[myConsole()] = (uint32_t)color;
    return color;
}

int
vga_drawpixel(int x, int y) {
    unsigned console = Syscalls::console();
    VirtualConsole::putPixel(console, x, y, currentColor[console % VirtualConsole::COUNT]);
    return 0;
}

int
vga_getpixel(int x, int y) { return (int)VirtualConsole::getPixel(Syscalls::console(), x, y); }

int
vga_setpalvec(int start, int num, int *pal) {
    if (start < 0 || num < 0 || start + num > 256) {
        return -1;
    }
    uint8_t rgb[VgaMode::PALETTE_BYTES];
    for (int i = 0; i < num * 3; i++) {
        rgb[i] = (uint8_t)(pal[i] & 63);
    }
    VirtualConsole::setPalette(Syscalls::console(), (unsigned)start, (unsigned)num, rgb);
    return 0;
}

int
vga_getkey() {
    uint8_t c;
    if (!Tty::readable() || Tty::read(&c, 1) != 1) {
        return 0;
    }
    return (char)c;
}

void
vga_gettextfont(void *font) {
    if (!VgaMode::font((uint8_t *)font)) {
        for (unsigned i = 0; i < VgaMode::FONT_BYTES; i++) {
            ((uint8_t *)font)[i] = 0;
        }
    }
}
