#include "java/util/TreeMap.txx"
#include <X11/Xatom.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "vsdk/toolkit/gui/framebuffer/xlib/XlibFramebuffer.h"

// The fixed width fonts of every X11R6 installation, from the largest.
const char *const XlibFramebuffer::DEFAULT_FONTS[] = {"10x20", "9x15", "8x13", "fixed", nullptr};

XlibFramebuffer::XlibFramebuffer()
    : display(nullptr), screen(0), window(0), visual(nullptr), colormap(0), depth(0), depthBits(0), trueColor(false),
      redMask(0), greenMask(0), blueMask(0), back(0), gc(nullptr), font(nullptr), fontName{}, advance(8),
      lineHeight(16), surfaceWidth(0), surfaceHeight(0), clipLeft(0), clipTop(0), clipRight(0), clipBottom(0),
      foreground(0), drawing(0), mapped(false), mirror(nullptr), mirrorValid(false), pendingLeft(0), pendingTop(0),
      pendingRight(0), pendingBottom(0), damageLeft(0), damageTop(0), damageRight(0), damageBottom(0) {}

XlibFramebuffer::~XlibFramebuffer() {
    close();
}

bool
XlibFramebuffer::loadFont(const char *name) {
    XFontStruct *loaded = XLoadQueryFont(display, name);
    if (!loaded) {
        return false;
    }
    if (font) {
        XFreeFont(display, font);
    }
    font = loaded;
    snprintf(fontName, sizeof(fontName), "%s", name);
    advance = font->max_bounds.width > 0 ? font->max_bounds.width : 8;
    lineHeight = font->ascent + font->descent;
    return true;
}

// The work area of the desktop (_NET_WORKAREA of EWMH window managers: the screen without panels), or the screen.
void
XlibFramebuffer::maximizedArea(int &x, int &y, int &w, int &h) const {
    x = 0;
    y = 0;
    w = DisplayWidth(display, screen);
    h = DisplayHeight(display, screen);
    Atom workArea = XInternAtom(display, "_NET_WORKAREA", True);
    if (workArea == None) {
        return;
    }
    Atom type;
    int format;
    unsigned long count;
    unsigned long after;
    unsigned char *data = nullptr;
    if (XGetWindowProperty(display, RootWindow(display, screen), workArea, 0, 4, False, XA_CARDINAL, &type, &format,
                           &count, &after, &data) == Success &&
        data && format == 32 && count >= 4) {
        long *values = (long *)data;
        if (values[2] > 0 && values[3] > 0) {
            x = (int)values[0];
            y = (int)values[1];
            w = (int)values[2];
            h = (int)values[3];
        }
    }
    if (data) {
        XFree(data);
    }
}

bool
XlibFramebuffer::open(const char *displayName, const char *geometry, const char *requestedFont, const char *title) {
    close();
    display = XOpenDisplay(displayName);
    if (!display) {
        fprintf(stderr, "Cannot open the X display %s\n", displayName ? displayName : XDisplayName(nullptr));
        return false;
    }
    screen = DefaultScreen(display);
    visual = DefaultVisual(display, screen);
    colormap = DefaultColormap(display, screen);
    depth = DefaultDepth(display, screen);
    depthBits = depth >= 24 ? 24 : depth >= 15 ? 16 : depth >= 8 ? 8 : depth >= 4 ? 4 : 1;
    trueColor = visual->c_class == TrueColor || visual->c_class == DirectColor;
    redMask = visual->red_mask;
    greenMask = visual->green_mask;
    blueMask = visual->blue_mask;

    bool fontLoaded = requestedFont && loadFont(requestedFont);
    if (requestedFont && !fontLoaded) {
        fprintf(stderr, "Font %s not found, using a default one\n", requestedFont);
    }
    for (int i = 0; !fontLoaded && DEFAULT_FONTS[i]; i++) {
        fontLoaded = loadFont(DEFAULT_FONTS[i]);
    }
    if (!fontLoaded) {
        fprintf(stderr, "No fixed width font found\n");
        close();
        return false;
    }

    int x;
    int y;
    int w;
    int h;
    maximizedArea(x, y, w, h);
    XSizeHints hints;
    memset(&hints, 0, sizeof(hints));
    hints.flags = PPosition | PSize | PMinSize;
    hints.min_width = 40 * advance;
    hints.min_height = 12 * lineHeight;
    bool maximize = !geometry || !*geometry;
    if (!maximize) {
        int gx = 0;
        int gy = 0;
        unsigned gw = 0;
        unsigned gh = 0;
        int parsed = XParseGeometry(geometry, &gx, &gy, &gw, &gh);
        if (parsed & WidthValue) {
            w = (int)gw;
            hints.flags |= USSize;
        }
        if (parsed & HeightValue) {
            h = (int)gh;
            hints.flags |= USSize;
        }
        if (parsed & XValue) {
            x = parsed & XNegative ? DisplayWidth(display, screen) + gx - w : gx;
            hints.flags |= USPosition;
        }
        if (parsed & YValue) {
            y = parsed & YNegative ? DisplayHeight(display, screen) + gy - h : gy;
            hints.flags |= USPosition;
        }
        if (parsed & (XNegative | YNegative)) {
            hints.flags |= PWinGravity;
            hints.win_gravity = parsed & XNegative ? (parsed & YNegative ? SouthEastGravity : NorthEastGravity)
                                                   : SouthWestGravity;
        }
    }
    w = w < 1 ? 1 : w;
    h = h < 1 ? 1 : h;
    hints.x = x;
    hints.y = y;
    hints.width = w;
    hints.height = h;

    XSetWindowAttributes attributes;
    attributes.background_pixmap = None;
    attributes.border_pixel = BlackPixel(display, screen);
    attributes.bit_gravity = NorthWestGravity;
    attributes.event_mask = KeyPressMask | KeyReleaseMask | ButtonPressMask | ButtonReleaseMask | PointerMotionMask |
                            EnterWindowMask | ExposureMask | StructureNotifyMask | FocusChangeMask;
    window = XCreateWindow(display, RootWindow(display, screen), x, y, (unsigned)w, (unsigned)h, 0, depth,
                           InputOutput, visual, CWBackPixmap | CWBorderPixel | CWBitGravity | CWEventMask,
                           &attributes);
    const char *name = title ? title : "Vitral";
    XStoreName(display, window, name);
    XSetIconName(display, window, name);
    Atom utf8 = XInternAtom(display, "UTF8_STRING", False);
    XChangeProperty(display, window, XInternAtom(display, "_NET_WM_NAME", False), utf8, 8, PropModeReplace,
                    (const unsigned char *)name, (int)strlen(name));
    XSetWMNormalHints(display, window, &hints);
    XClassHint classHint;
    classHint.res_name = const_cast<char *>("vitral");
    classHint.res_class = const_cast<char *>("Vitral");
    XSetClassHint(display, window, &classHint);
    XWMHints wmHints;
    wmHints.flags = InputHint | StateHint;
    wmHints.input = True;
    wmHints.initial_state = NormalState;
    XSetWMHints(display, window, &wmHints);
    Atom deleteWindow = XInternAtom(display, "WM_DELETE_WINDOW", False);
    XSetWMProtocols(display, window, &deleteWindow, 1);
    if (maximize) {
        // EWMH window managers maximize the window when it is mapped; others leave it on the work area.
        Atom states[2] = {XInternAtom(display, "_NET_WM_STATE_MAXIMIZED_VERT", False),
                          XInternAtom(display, "_NET_WM_STATE_MAXIMIZED_HORZ", False)};
        XChangeProperty(display, window, XInternAtom(display, "_NET_WM_STATE", False), XA_ATOM, 32,
                        PropModeReplace, (const unsigned char *)states, 2);
    }

    gc = XCreateGC(display, window, 0, nullptr);
    XSetFont(display, gc, font->fid);
    XSetGraphicsExposures(display, gc, False);
    createBackBuffer(w, h);
    XMapWindow(display, window);
    XFlush(display);
    return true;
}

bool
XlibFramebuffer::waitMapped() {
    while (display && !mapped) {
        XEvent event;
        XWindowEvent(display, window, StructureNotifyMask, &event);
        if (event.type == ConfigureNotify) {
            resize(event.xconfigure.width, event.xconfigure.height);
        } else if (event.type == MapNotify) {
            mapped = true;
        }
    }
    return display != nullptr;
}

void
XlibFramebuffer::close() {
    if (!display) {
        return;
    }
    if (mirror) {
        XDestroyImage(mirror);
        mirror = nullptr;
    }
    if (back) {
        XFreePixmap(display, back);
        back = 0;
    }
    if (gc) {
        XFreeGC(display, gc);
        gc = nullptr;
    }
    if (font) {
        XFreeFont(display, font);
        font = nullptr;
    }
    if (window) {
        XDestroyWindow(display, window);
        window = 0;
    }
    XCloseDisplay(display);
    display = nullptr;
    allocated.clear();
    mapped = false;
}

void
XlibFramebuffer::createBackBuffer(int w, int h) {
    Pixmap old = back;
    back = XCreatePixmap(display, window, (unsigned)w, (unsigned)h, (unsigned)depth);
    XSetClipMask(display, gc, None);
    XSetForeground(display, gc, BlackPixel(display, screen));
    foreground = BlackPixel(display, screen);
    XFillRectangle(display, back, gc, 0, 0, (unsigned)w, (unsigned)h);
    if (old) {
        int copyWidth = w < surfaceWidth ? w : surfaceWidth;
        int copyHeight = h < surfaceHeight ? h : surfaceHeight;
        XCopyArea(display, old, back, gc, 0, 0, (unsigned)copyWidth, (unsigned)copyHeight, 0, 0);
        XFreePixmap(display, old);
    }
    if (mirror) {
        XDestroyImage(mirror);
        mirror = nullptr;
    }
    mirrorValid = false;
    pendingRight = pendingLeft = 0;
    surfaceWidth = w;
    surfaceHeight = h;
    resetClip();
    damageAll();
}

bool
XlibFramebuffer::resize(int w, int h) {
    if (!display || w <= 0 || h <= 0 || (w == surfaceWidth && h == surfaceHeight)) {
        return false;
    }
    sendPending();
    createBackBuffer(w, h);
    return true;
}

void
XlibFramebuffer::expose(int x, int y, int w, int h) {
    if (!display) {
        return;
    }
    damage(x, y, w, h);
    if (drawing == 0) {
        flush();
    }
}

void
XlibFramebuffer::damage(int x, int y, int w, int h) {
    if (w <= 0 || h <= 0) {
        return;
    }
    if (damageRight <= damageLeft || damageBottom <= damageTop) {
        damageLeft = x;
        damageTop = y;
        damageRight = x + w;
        damageBottom = y + h;
        return;
    }
    damageLeft = x < damageLeft ? x : damageLeft;
    damageTop = y < damageTop ? y : damageTop;
    damageRight = x + w > damageRight ? x + w : damageRight;
    damageBottom = y + h > damageBottom ? y + h : damageBottom;
}

void
XlibFramebuffer::flush() {
    if (!display) {
        return;
    }
    sendPending();
    int left = damageLeft < 0 ? 0 : damageLeft;
    int top = damageTop < 0 ? 0 : damageTop;
    int right = damageRight > surfaceWidth ? surfaceWidth : damageRight;
    int bottom = damageBottom > surfaceHeight ? surfaceHeight : damageBottom;
    if (right > left && bottom > top) {
        XSetClipMask(display, gc, None);
        XCopyArea(display, back, window, gc, left, top, (unsigned)(right - left), (unsigned)(bottom - top), left,
                  top);
        setClip(clipLeft, clipTop, clipRight - clipLeft, clipBottom - clipTop);
    }
    damageLeft = damageTop = damageRight = damageBottom = 0;
    XFlush(display);
}

void
XlibFramebuffer::beginDrawing() {
    drawing++;
}

void
XlibFramebuffer::endDrawing() {
    if (drawing > 0 && --drawing == 0) {
        flush();
    }
}

int
XlibFramebuffer::channelShift(unsigned long mask) {
    int shift = 0;
    while (mask && !(mask & 1)) {
        mask >>= 1;
        shift++;
    }
    return shift;
}

// An 8 bit channel scaled to the bits of a visual's mask.
unsigned long
XlibFramebuffer::channel(uint32_t value, unsigned long mask) {
    int shift = channelShift(mask);
    unsigned long bits = mask >> shift;
    return ((unsigned long)value * bits / 255) << shift;
}

unsigned long
XlibFramebuffer::native(uint32_t color) {
    color &= 0xFFFFFF;
    uint32_t r = color >> 16;
    uint32_t g = color >> 8 & 0xFF;
    uint32_t b = color & 0xFF;
    if (trueColor) {
        return channel(r, redMask) | channel(g, greenMask) | channel(b, blueMask);
    }
    if (depthBits == 1) {
        return r * 3 + g * 4 + b * 2 >= 128 * 9 ? WhitePixel(display, screen) : BlackPixel(display, screen);
    }
    unsigned long* known = allocated.get(color);
    if (known != nullptr) {
        return (*known);
    }
    XColor wanted;
    wanted.red = (unsigned short)(r * 257);
    wanted.green = (unsigned short)(g * 257);
    wanted.blue = (unsigned short)(b * 257);
    wanted.flags = DoRed | DoGreen | DoBlue;
    unsigned long value;
    if (XAllocColor(display, colormap, &wanted)) {
        value = wanted.pixel;
    } else {
        // The colormap is full: the nearest of its cells.
        int cells = visual->map_entries < 256 ? visual->map_entries : 256;
        XColor table[256];
        for (int i = 0; i < cells; i++) {
            table[i].pixel = (unsigned long)i;
        }
        XQueryColors(display, colormap, table, cells);
        long best = -1;
        value = BlackPixel(display, screen);
        for (int i = 0; i < cells; i++) {
            long dr = (long)(table[i].red >> 8) - (long)r;
            long dg = (long)(table[i].green >> 8) - (long)g;
            long db = (long)(table[i].blue >> 8) - (long)b;
            long distance = dr * dr * 3 + dg * dg * 4 + db * db * 2;
            if (best < 0 || distance < best) {
                best = distance;
                value = table[i].pixel;
            }
        }
    }
    allocated.put(color, value);
    return value;
}

uint32_t
XlibFramebuffer::rgbOf(unsigned long value) const {
    if (trueColor) {
        unsigned long masks[3] = {redMask, greenMask, blueMask};
        uint32_t rgb = 0;
        for (unsigned long mask : masks) {
            int shift = channelShift(mask);
            unsigned long bits = mask >> shift;
            rgb = rgb << 8 | (uint32_t)(bits ? ((value & mask) >> shift) * 255 / bits : 0);
        }
        return rgb;
    }
    for (long i = 0; i < allocated.size(); i++) {
        if (allocated.getValue(i) == value) {
            return allocated.getKey(i);
        }
    }
    XColor cell;
    cell.pixel = value;
    XQueryColor(display, colormap, &cell);
    return (uint32_t)(cell.red >> 8) << 16 | (uint32_t)(cell.green >> 8) << 8 | (uint32_t)(cell.blue >> 8);
}

void
XlibFramebuffer::setForeground(uint32_t color) {
    unsigned long value = native(color);
    if (value != foreground) {
        foreground = value;
        XSetForeground(display, gc, value);
    }
}

// Xlib is about to draw on the back buffer: the pixels written to the mirror go first, and the mirror stops
// matching the back buffer.
void
XlibFramebuffer::beforeDrawing() {
    sendPending();
    mirrorValid = false;
}

void
XlibFramebuffer::sendPending() {
    if (!mirror || pendingRight <= pendingLeft || pendingBottom <= pendingTop) {
        pendingRight = pendingLeft = 0;
        return;
    }
    XSetClipMask(display, gc, None);
    XPutImage(display, back, gc, mirror, pendingLeft, pendingTop, pendingLeft, pendingTop,
              (unsigned)(pendingRight - pendingLeft), (unsigned)(pendingBottom - pendingTop));
    setClip(clipLeft, clipTop, clipRight - clipLeft, clipBottom - clipTop);
    damage(pendingLeft, pendingTop, pendingRight - pendingLeft, pendingBottom - pendingTop);
    pendingRight = pendingLeft = 0;
}

bool
XlibFramebuffer::fetchMirror() const {
    if (mirrorValid) {
        return true;
    }
    if (!display || surfaceWidth <= 0 || surfaceHeight <= 0) {
        return false;
    }
    if (mirror) {
        XGetSubImage(display, back, 0, 0, (unsigned)surfaceWidth, (unsigned)surfaceHeight, AllPlanes, ZPixmap, mirror,
                     0, 0);
    } else {
        mirror = XGetImage(display, back, 0, 0, (unsigned)surfaceWidth, (unsigned)surfaceHeight, AllPlanes, ZPixmap);
    }
    mirrorValid = mirror != nullptr;
    return mirrorValid;
}

void
XlibFramebuffer::setClip(int x, int y, int w, int h) {
    clipLeft = x < 0 ? 0 : x;
    clipTop = y < 0 ? 0 : y;
    clipRight = x + w > surfaceWidth ? surfaceWidth : x + w;
    clipBottom = y + h > surfaceHeight ? surfaceHeight : y + h;
    if (!display) {
        return;
    }
    XRectangle rectangle;
    rectangle.x = (short)clipLeft;
    rectangle.y = (short)clipTop;
    rectangle.width = (unsigned short)(clipRight > clipLeft ? clipRight - clipLeft : 0);
    rectangle.height = (unsigned short)(clipBottom > clipTop ? clipBottom - clipTop : 0);
    XSetClipRectangles(display, gc, 0, 0, &rectangle, 1, YXBanded);
}

void
XlibFramebuffer::resetClip() {
    setClip(0, 0, surfaceWidth, surfaceHeight);
}

void
XlibFramebuffer::pixel(int x, int y, uint32_t color) {
    if (x < clipLeft || y < clipTop || x >= clipRight || y >= clipBottom) {
        return;
    }
    beforeDrawing();
    setForeground(color);
    XDrawPoint(display, back, gc, x, y);
    damage(x, y, 1, 1);
}

uint32_t
XlibFramebuffer::nativePixel(int x, int y) const {
    if (x < 0 || y < 0 || x >= surfaceWidth || y >= surfaceHeight || !fetchMirror()) {
        return 0;
    }
    return (uint32_t)XGetPixel(mirror, x, y);
}

void
XlibFramebuffer::putNativePixel(int x, int y, uint32_t value) {
    if (x < 0 || y < 0 || x >= surfaceWidth || y >= surfaceHeight || !fetchMirror()) {
        return;
    }
    XPutPixel(mirror, x, y, value);
    if (pendingRight <= pendingLeft) {
        pendingLeft = x;
        pendingTop = y;
        pendingRight = x + 1;
        pendingBottom = y + 1;
        return;
    }
    pendingLeft = x < pendingLeft ? x : pendingLeft;
    pendingTop = y < pendingTop ? y : pendingTop;
    pendingRight = x + 1 > pendingRight ? x + 1 : pendingRight;
    pendingBottom = y + 1 > pendingBottom ? y + 1 : pendingBottom;
}

void
XlibFramebuffer::line(int x0, int y0, int x1, int y1, uint32_t color) {
    beforeDrawing();
    setForeground(color);
    XDrawLine(display, back, gc, x0, y0, x1, y1);
    int left = x0 < x1 ? x0 : x1;
    int top = y0 < y1 ? y0 : y1;
    damage(left, top, (x0 < x1 ? x1 - x0 : x0 - x1) + 1, (y0 < y1 ? y1 - y0 : y0 - y1) + 1);
}

void
XlibFramebuffer::fillRectangle(int x, int y, int w, int h, uint32_t color) {
    if (w <= 0 || h <= 0) {
        return;
    }
    beforeDrawing();
    setForeground(color);
    XFillRectangle(display, back, gc, x, y, (unsigned)w, (unsigned)h);
    damage(x, y, w, h);
}

void
XlibFramebuffer::rectangle(int x, int y, int w, int h, uint32_t color) {
    if (w <= 0 || h <= 0) {
        return;
    }
    beforeDrawing();
    setForeground(color);
    XDrawRectangle(display, back, gc, x, y, (unsigned)(w - 1), (unsigned)(h - 1));
    damage(x, y, w, h);
}

void
XlibFramebuffer::fillPolygon(const int *xy, int count, uint32_t color) {
    if (!xy || count < 2) {
        return;
    }
    XPoint points[64];
    XPoint *vertices = count + 1 <= 64 ? points : new XPoint[count + 1];
    int left = xy[0];
    int top = xy[1];
    int right = xy[0];
    int bottom = xy[1];
    for (int i = 0; i < count; i++) {
        vertices[i].x = (short)xy[2 * i];
        vertices[i].y = (short)xy[2 * i + 1];
        left = xy[2 * i] < left ? xy[2 * i] : left;
        right = xy[2 * i] > right ? xy[2 * i] : right;
        top = xy[2 * i + 1] < top ? xy[2 * i + 1] : top;
        bottom = xy[2 * i + 1] > bottom ? xy[2 * i + 1] : bottom;
    }
    vertices[count] = vertices[0];
    beforeDrawing();
    setForeground(color);
    if (count >= 3) {
        XFillPolygon(display, back, gc, vertices, count, Complex, CoordModeOrigin);
    }
    // X fills leave out the right and bottom edges; the contract includes them.
    XDrawLines(display, back, gc, vertices, count + 1, CoordModeOrigin);
    if (vertices != points) {
        delete[] vertices;
    }
    damage(left, top, right - left + 1, bottom - top + 1);
}

void
XlibFramebuffer::putImage(int x, int y, int w, int h, const uint32_t *rgb) {
    if (!rgb || w <= 0 || h <= 0) {
        return;
    }
    XImage *image = XCreateImage(display, visual, (unsigned)depth, ZPixmap, 0, nullptr, (unsigned)w, (unsigned)h, 32, 0);
    if (!image) {
        return;
    }
    image->data = (char *)malloc((size_t)image->bytes_per_line * (size_t)h);
    if (!image->data) {
        XDestroyImage(image);
        return;
    }
    uint32_t lastColor = ~rgb[0];
    unsigned long lastValue = 0;
    for (int row = 0; row < h; row++) {
        for (int column = 0; column < w; column++) {
            uint32_t color = rgb[(long)row * w + column] & 0xFFFFFF;
            if (color != lastColor) {
                lastColor = color;
                lastValue = native(color);
            }
            XPutPixel(image, column, row, lastValue);
        }
    }
    beforeDrawing();
    XPutImage(display, back, gc, image, 0, 0, x, y, (unsigned)w, (unsigned)h);
    XDestroyImage(image);
    damage(x, y, w, h);
}

bool
XlibFramebuffer::readImage(int x, int y, int w, int h, uint32_t *rgb) const {
    if (!rgb || !fetchMirror()) {
        return false;
    }
    for (int row = 0; row < h; row++) {
        for (int column = 0; column < w; column++) {
            int px = x + column;
            int py = y + row;
            bool inside = px >= 0 && py >= 0 && px < surfaceWidth && py < surfaceHeight;
            rgb[(long)row * w + column] = inside ? rgbOf(XGetPixel(mirror, px, py)) : 0;
        }
    }
    return true;
}

// The next code point of a UTF-8 text; malformed bytes are taken one at a time as U+FFFD.
unsigned
XlibFramebuffer::decode(const char *&text) {
    unsigned char c = (unsigned char)*text++;
    if (c < 0x80) {
        return c;
    }
    int more = c >= 0xF0 ? 3 : c >= 0xE0 ? 2 : c >= 0xC0 ? 1 : -1;
    if (more < 0) {
        return 0xFFFD;
    }
    unsigned codePoint = c & (0x3F >> more);
    for (int i = 0; i < more; i++) {
        if (((unsigned char)*text & 0xC0) != 0x80) {
            return 0xFFFD;
        }
        codePoint = codePoint << 6 | ((unsigned char)*text++ & 0x3F);
    }
    return codePoint;
}

int
XlibFramebuffer::textWidth(const char *text, int byteCount) const {
    const char *end = text;
    while (*end && (byteCount < 0 || end - text < byteCount)) {
        end++;
    }
    int characters = 0;
    while (text < end) {
        decode(text);
        characters++;
    }
    return characters * advance;
}

// Core fonts are indexed by Latin-1 bytes (iso8859-1 and the first page of iso10646-1 fonts): other characters,
// and those the font lacks, are shown as '?'.
void
XlibFramebuffer::text(int x, int y, const char *text, uint32_t color) {
    if (!text || !*text) {
        return;
    }
    char buffer[512];
    int count = 0;
    while (*text && count < (int)sizeof(buffer)) {
        unsigned codePoint = decode(text);
        bool present = codePoint >= font->min_char_or_byte2 && codePoint <= font->max_char_or_byte2 &&
                       codePoint <= 0xFF && codePoint >= 0x20 && codePoint != 0x7F;
        if (present && font->per_char) {
            const XCharStruct &glyph = font->per_char[codePoint - font->min_char_or_byte2];
            present = glyph.width != 0 || glyph.ascent != 0 || glyph.descent != 0;
        }
        buffer[count++] = present ? (char)codePoint : '?';
    }
    beforeDrawing();
    setForeground(color);
    XDrawString(display, back, gc, x, y + font->ascent, buffer, count);
    damage(x, y, count * advance, lineHeight);
}
