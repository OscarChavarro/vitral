#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "drivers/video/vga/VgaText.h"
#include "kernel/KernelHeap.h"
#include "lib/vitralKernel/Memory.h"

VirtualConsole::Screen VirtualConsole::screens[VirtualConsole::COUNT];
unsigned VirtualConsole::activeIndex = 0;
VgaMode::Id VirtualConsole::hardwareMode = VgaMode::Id::Text80x25;
bool VirtualConsole::panicking = false;
SpinLock VirtualConsole::lock;

VirtualConsole::Locked::Locked() : flags(0), held(!panicking) {
    if (held) {
        flags = lock.lock();
    }
}
VirtualConsole::Locked::~Locked() {
    if (held) {
        lock.unlock(flags);
    }
}

void
VirtualConsole::init() {
    VgaText::init();
    VgaMode::saveFont();
    for (Screen &s : screens) {
        for (uint16_t &c : s.cells) {
            c = (uint16_t)(0x07 << 8) | ' ';
        }
        s.cx = s.cy = 0;
        s.attr = 0x07;
        s.cursorVisible = true;
        s.pointerX = s.pointerY = -1;
        s.esc = Esc::None;
        s.mode = VgaMode::Id::Text80x25;
        s.pixels = nullptr;
    }
    Screen &boot = screens[0];
    for (int y = 0; y < ROWS; y++) {
        for (int x = 0; x < COLS; x++) {
            cell(boot, x, y) = VgaText::cell(x, y);
        }
    }
    boot.cx = VgaText::cursorX();
    boot.cy = VgaText::cursorY();
    activeIndex = 0;
}

void
VirtualConsole::takeOver() {
    panicking = true;
    Screen &s = screens[activeIndex];
    if (s.mode != VgaMode::Id::Text80x25) {
        s.mode = VgaMode::Id::Text80x25;
        s.pixels = nullptr;
        show(activeIndex);
    }
}

void
VirtualConsole::show(unsigned console) {
    Screen &s = screens[console];
    if (s.mode != hardwareMode) {
        if (hardwareMode == VgaMode::Id::Text80x25) {
            VgaMode::saveFont();
        }
        VgaMode::program(s.mode);
        hardwareMode = s.mode;
    }
    if (VgaMode::info(s.mode).layout == VgaMode::Layout::Text) {
        VgaText::setGeometry(columns(s), rows(s));
        paintAll();
    } else if (framebufferText(s)) {
        paintAll();
    } else {
        if (VgaMode::info(s.mode).usesDac()) {
            VgaMode::loadPalette(s.palette);
        }
        VgaMode::blit(s.mode, s.pixels);
    }
    VgaMode::display();
}

void
VirtualConsole::leaveGraphics(unsigned console) {
    Screen &s = screens[console];
    uint8_t *old = s.pixels;
    s.pixels = nullptr;
    s.mode = VgaMode::Id::Text80x25;
    delete[] old;
    if (console == activeIndex) {
        show(console);
    }
}

bool
VirtualConsole::setMode(unsigned console, VgaMode::Id mode) {
    const VgaMode::Info &info = VgaMode::info(mode);
    uint8_t *pixels = nullptr;
    if (info.graphics()) {
        if (!VgaMode::available(mode)) {
            return false;
        }
        size_t bytes = (size_t)info.width * info.height * info.pixelBytes();
        if (KernelHeap::sizeBytes() - KernelHeap::usedBytes() < bytes + (256u << 10)) {
            return false;
        }
        pixels = new uint8_t[bytes];
        Memory::set(pixels, 0, bytes);
    }
    Locked guard;
    console = resolve(console);
    Screen &s = screens[console];
    if (info.layout == VgaMode::Layout::Text) {
        if (s.mode != mode) {
            leaveGraphics(console);
            s.mode = mode;
            if (console == activeIndex) {
                show(console);
            }
        }
        return true;
    }
    uint8_t *old = s.pixels;
    s.pixels = pixels;
    s.mode = mode;
    delete[] old;
    VgaMode::defaultPalette(s.palette);
    if (info.colors == 2) {
        s.palette[3] = s.palette[4] = s.palette[5] = 63;
    }
    if (console == activeIndex) {
        show(console);
    }
    return true;
}

bool
VirtualConsole::setTextMode(VgaMode::Id mode) {
    if (!VgaMode::isConsoleText(mode) || !VgaMode::available(mode)) {
        return false;
    }
    Locked guard;
    for (Screen &s : screens) {
        uint8_t *old = s.pixels;
        s.pixels = nullptr;
        s.mode = mode;
        delete[] old;
    }
    show(activeIndex);
    return true;
}

VgaMode::Id
VirtualConsole::mode(unsigned console) {
    Locked guard;
    return screens[resolve(console)].mode;
}

int
VirtualConsole::columns(unsigned console) {
    Locked guard;
    return columns(screens[resolve(console)]);
}

int
VirtualConsole::rows(unsigned console) {
    Locked guard;
    return rows(screens[resolve(console)]);
}

void
VirtualConsole::putPixel(unsigned console, int x, int y, uint32_t color) {
    Locked guard;
    console = resolve(console);
    Screen &s = screens[console];
    if (!s.pixels) {
        return;
    }
    const VgaMode::Info &info = VgaMode::info(s.mode);
    if (x < 0 || y < 0 || x >= info.width || y >= info.height) {
        return;
    }
    uint8_t *pixel = s.pixels + ((size_t)y * info.width + x) * info.pixelBytes();
    for (unsigned i = 0; i < info.pixelBytes(); i++) {
        pixel[i] = (uint8_t)(color >> (8 * i));
    }
    if (console == activeIndex) {
        VgaMode::plot(s.mode, x, y, color);
    }
}

uint32_t
VirtualConsole::getPixel(unsigned console, int x, int y) {
    Locked guard;
    const Screen &s = screens[resolve(console)];
    if (!s.pixels) {
        return 0;
    }
    const VgaMode::Info &info = VgaMode::info(s.mode);
    if (x < 0 || y < 0 || x >= info.width || y >= info.height) {
        return 0;
    }
    const uint8_t *pixel = s.pixels + ((size_t)y * info.width + x) * info.pixelBytes();
    uint32_t color = 0;
    for (unsigned i = 0; i < info.pixelBytes(); i++) {
        color |= (uint32_t)pixel[i] << (8 * i);
    }
    return color;
}

void
VirtualConsole::setPalette(unsigned console, unsigned first, unsigned count, const uint8_t *rgb) {
    if (first + count > 256) {
        return;
    }
    Locked guard;
    console = resolve(console);
    Screen &s = screens[console];
    if (!s.pixels || VgaMode::info(s.mode).colors != 256) {
        return;
    }
    Memory::copy(s.palette + first * 3, rgb, count * 3);
    if (console == activeIndex) {
        VgaMode::loadPalette(s.palette);
    }
}

void
VirtualConsole::clearPixels(unsigned console, uint32_t color) {
    Locked guard;
    console = resolve(console);
    Screen &s = screens[console];
    if (!s.pixels) {
        return;
    }
    const VgaMode::Info &info = VgaMode::info(s.mode);
    size_t count = (size_t)info.width * info.height;
    if (info.pixelBytes() == 1) {
        Memory::set(s.pixels, (uint8_t)color, count);
    } else {
        for (size_t p = 0; p < count; p++) {
            for (unsigned i = 0; i < info.pixelBytes(); i++) {
                s.pixels[p * info.pixelBytes() + i] = (uint8_t)(color >> (8 * i));
            }
        }
    }
    if (console == activeIndex) {
        show(console);
    }
}

void
VirtualConsole::paintCursor(const Screen &s) {
    if (framebufferText(s)) {
        if (s.cx >= 0 && s.cx < columns(s) && s.cy >= 0 && s.cy < rows(s)) {
            paintFramebufferCell(s, s.cx, s.cy);
        }
        return;
    }
    int width = columns(s);
    int height = rows(s);
    VgaText::setCursor(s.cx < width ? s.cx : width - 1, s.cy < height ? s.cy : height - 1);
    VgaText::showCursor(s.cursorVisible);
}

void
VirtualConsole::paintCell(const Screen &s, int x, int y) {
    if (framebufferText(s)) {
        paintFramebufferCell(s, x, y);
        return;
    }
    uint16_t c = cell(s, x, y);
    uint8_t attr = (uint8_t)(c >> 8);
    if (x == s.pointerX && y == s.pointerY) {
        attr ^= 0x77;
    }
    VgaText::putAt(x, y, (char)(c & 0xFF), attr);
}

void
VirtualConsole::paintFramebufferCell(const Screen &s, int x, int y) {
    static const uint32_t COLORS[16] = {
        0x00000000, 0x000000AA, 0x0000AA00, 0x0000AAAA,
        0x00AA0000, 0x00AA00AA, 0x00AA5500, 0x00AAAAAA,
        0x00555555, 0x005555FF, 0x0055FF55, 0x0055FFFF,
        0x00FF5555, 0x00FF55FF, 0x00FFFF55, 0x00FFFFFF,
    };
    uint16_t c = cell(s, x, y);
    uint8_t attr = (uint8_t)(c >> 8);
    if (x == s.pointerX && y == s.pointerY) {
        attr ^= 0x77;
    }
    if (s.cursorVisible && x == s.cx && y == s.cy) {
        attr ^= 0x77;
    }
    uint32_t foreground = COLORS[attr & 0x0F];
    uint32_t background = COLORS[(attr >> 4) & 0x0F];
    uint8_t glyph = (uint8_t)c;
    unsigned frameWidth = VgaMode::frameWidth(s.mode);
    unsigned frameHeight = VgaMode::frameHeight(s.mode);
    int left = x * (int)frameWidth / columns(s);
    int right = (x + 1) * (int)frameWidth / columns(s);
    int top = y * (int)frameHeight / rows(s);
    int bottom = (y + 1) * (int)frameHeight / rows(s);
    for (int row = 0; row < 16; row++) {
        uint8_t bits = VgaMode::fontRow(glyph, (unsigned)row);
        for (int column = 0; column < 8; column++) {
            uint32_t color = bits & (uint8_t)(0x80 >> column) ? foreground : background;
            int pixelLeft = left + column * (right - left) / 8;
            int pixelRight = left + (column + 1) * (right - left) / 8;
            int pixelTop = top + row * (bottom - top) / 16;
            int pixelBottom = top + (row + 1) * (bottom - top) / 16;
            for (int py = pixelTop; py < pixelBottom; py++) {
                for (int px = pixelLeft; px < pixelRight; px++) {
                    VgaMode::plot(s.mode, px, py, color);
                }
            }
        }
    }
}

void
VirtualConsole::paintAll() {
    const Screen &s = screens[activeIndex];
    for (int y = 0; y < rows(s); y++) {
        for (int x = 0; x < columns(s); x++) {
            paintCell(s, x, y);
        }
    }
    paintCursor(s);
}

void
VirtualConsole::setPointer(unsigned console, int x, int y) {
    Locked guard;
    console = resolve(console);
    Screen &s = screens[console];
    if (x < 0 || y < 0 || x >= columns(s) || y >= rows(s)) {
        x = y = -1;
    }
    int oldX = s.pointerX;
    int oldY = s.pointerY;
    s.pointerX = x;
    s.pointerY = y;
    if (!visible(console)) {
        return;
    }
    if (oldX >= 0) {
        paintCell(s, oldX, oldY);
    }
    if (x >= 0) {
        paintCell(s, x, y);
    }
}

void
VirtualConsole::switchTo(unsigned console) {
    if (console >= COUNT) {
        return;
    }
    Locked guard;
    activeIndex = console;
    show(console);
}

void
VirtualConsole::scroll(unsigned console) {
    Screen &s = screens[console];
    int width = columns(s);
    int height = rows(s);
    for (int y = 0; y < height - 1; y++) {
        for (int x = 0; x < width; x++) {
            cell(s, x, y) = cell(s, x, y + 1);
        }
    }
    for (int x = 0; x < width; x++) {
        cell(s, x, height - 1) = (uint16_t)(s.attr << 8) | ' ';
    }
    s.cy = height - 1;
    if (visible(console)) {
        paintAll();
    }
}

void
VirtualConsole::csi(unsigned console, char final) {
    Screen &s = screens[console];
    int p0 = s.escParams[0];
    int p1 = s.escParams[1];
    if ((final == 'H' || final == 'f') && !s.escPrivate) {
        int row = p0 > 0 ? p0 : 1;
        int column = p1 > 0 ? p1 : 1;
        s.cy = row > rows(s) ? rows(s) - 1 : row - 1;
        s.cx = column > columns(s) ? columns(s) - 1 : column - 1;
    } else if (final == 'J' && p0 == 2) {
        for (uint16_t &c : s.cells) {
            c = (uint16_t)(s.attr << 8) | ' ';
        }
        if (visible(console)) {
            paintAll();
        }
    } else if (final == 'm') {
        for (int i = 0; i <= s.escIndex && i < 2; i++) {
            int p = s.escParams[i];
            if (p <= 0) {
                s.attr = 0x07;
            } else if (p == 1) {
                s.attr |= 0x08;
            } else if (p == 7) {
                s.attr = (uint8_t)((s.attr & 0x88) | (s.attr & 0x07) << 4 | (s.attr >> 4 & 0x07));
            }
        }
    } else if ((final == 'h' || final == 'l') && s.escPrivate && p0 == 25) {
        s.cursorVisible = final == 'h';
    }
    if (visible(console)) {
        paintCursor(s);
    }
}

void
VirtualConsole::escape(unsigned console, char c) {
    Screen &s = screens[console];
    if (s.esc == Esc::Started) {
        if (c == '[') {
            s.esc = Esc::Csi;
            s.escParams[0] = s.escParams[1] = -1;
            s.escIndex = 0;
            s.escPrivate = false;
        } else {
            s.esc = Esc::None;
        }
    } else if (c >= '0' && c <= '9') {
        int &p = s.escParams[s.escIndex];
        p = (p < 0 ? 0 : p) * 10 + (c - '0');
        if (p > 9999) {
            p = 9999;
        }
    } else if (c == ';') {
        if (s.escIndex < 1) {
            s.escIndex++;
        }
    } else if (c == '?') {
        s.escPrivate = true;
    } else if (c >= 0x40 && c <= 0x7E) {
        s.esc = Esc::None;
        csi(console, c);
    }
}

void
VirtualConsole::putc(unsigned console, char c) {
    Locked guard;
    console = resolve(console);
    Screen &s = screens[console];
    if (s.esc != Esc::None) { escape(console, c); return; }
    if (c == 27) { s.esc = Esc::Started; return; }
    if (c == '\n') { s.cx = 0; s.cy++; }
    else if (c == '\r') { s.cx = 0; }
    else if (c == '\t') { s.cx = (s.cx + 8) & ~7; }
    else if (c == '\b') { s.cx = s.cx > 0 ? s.cx - 1 : 0; }
    else {
        cell(s, s.cx, s.cy) = (uint16_t)(s.attr << 8) | (uint8_t)c;
        if (visible(console)) {
            paintCell(s, s.cx, s.cy);
        }
        s.cx++;
    }
    if (s.cx >= columns(s)) {
        s.cx = 0;
        s.cy++;
    }
    if (s.cy >= rows(s)) {
        scroll(console);
    }
    if (visible(console)) {
        paintCursor(s);
    }
}

void
VirtualConsole::clear(unsigned console, uint8_t attr) {
    Locked guard;
    console = resolve(console);
    Screen &s = screens[console];
    if (!VgaMode::isConsoleText(s.mode)) {
        leaveGraphics(console);
    }
    s.attr = attr;
    for (uint16_t &c : s.cells) {
        c = (uint16_t)(attr << 8) | ' ';
    }
    s.cx = s.cy = 0;
    s.cursorVisible = true;
    if (visible(console)) {
        paintAll();
    }
}

void
VirtualConsole::setAttr(unsigned console, uint8_t attr) {
    Locked guard;
    screens[resolve(console)].attr = attr;
}

void
VirtualConsole::putAt(unsigned console, int x, int y, char c, uint8_t attr) {
    Locked guard;
    console = resolve(console);
    Screen &s = screens[console];
    if (x < 0 || x >= columns(s) || y < 0 || y >= rows(s)) {
        return;
    }
    cell(s, x, y) = (uint16_t)(attr << 8) | (uint8_t)c;
    if (visible(console)) {
        paintCell(screens[console], x, y);
    }
}

void
VirtualConsole::writeAt(unsigned console, int x, int y, const char *str, uint8_t attr) {
    Locked guard;
    console = resolve(console);
    Screen &screen = screens[console];
    if (y < 0 || y >= rows(screen)) {
        return;
    }
    for (; *str && x < columns(screen); str++, x++) {
        if (x < 0) {
            continue;
        }
        cell(screen, x, y) = (uint16_t)(attr << 8) | (uint8_t)*str;
        if (visible(console)) {
            paintCell(screens[console], x, y);
        }
    }
}

void
VirtualConsole::fillRow(unsigned console, int y, char c, uint8_t attr) {
    Locked guard;
    console = resolve(console);
    Screen &s = screens[console];
    if (y < 0 || y >= rows(s)) {
        return;
    }
    for (int x = 0; x < columns(s); x++) {
        cell(s, x, y) = (uint16_t)(attr << 8) | (uint8_t)c;
        if (visible(console)) {
            paintCell(screens[console], x, y);
        }
    }
}

void
VirtualConsole::setCursor(unsigned console, int x, int y) {
    Locked guard;
    console = resolve(console);
    Screen &s = screens[console];
    s.cx = x;
    s.cy = y;
    if (visible(console)) {
        paintCursor(s);
    }
}

void
VirtualConsole::showCursor(unsigned console, bool visibleFlag) {
    Locked guard;
    console = resolve(console);
    Screen &s = screens[console];
    s.cursorVisible = visibleFlag;
    if (visible(console)) {
        paintCursor(s);
    }
}

int
VirtualConsole::cursorX(unsigned console) {
    Locked guard;
    return screens[resolve(console)].cx;
}

int
VirtualConsole::cursorY(unsigned console) {
    Locked guard;
    return screens[resolve(console)].cy;
}
