#ifndef __VIRTUAL_CONSOLE__
#define __VIRTUAL_CONSOLE__
#include <stddef.h>
#include <stdint.h>
#include "drivers/video/vga/VgaMode.h"
#include "lib/vitralKernel/SpinLock.h"

class VirtualConsole {
public:
    static constexpr unsigned COUNT = 12;
    static constexpr int COLS = 80;
    static constexpr int ROWS = 25;
    static constexpr int MAX_COLS = 240;
    static constexpr int MAX_ROWS = 67;

    static void init();
    static void switchTo(unsigned console);
    static unsigned active() { return activeIndex; }
    static void takeOver();

    static void putc(unsigned console, char c);
    // putc() for each character of a C string: for the kernel's own messages, which go to the screen whatever
    // terminal the process has.
    static void print(unsigned console, const char *text);
    static void clear(unsigned console, uint8_t attr);
    static void setAttr(unsigned console, uint8_t attr);

    static bool setMode(unsigned console, VgaMode::Id mode);
    static bool setTextMode(VgaMode::Id mode);
    static VgaMode::Id mode(unsigned console);
    static int columns(unsigned console);
    static int rows(unsigned console);
    static void putPixel(unsigned console, int x, int y, uint32_t color);
    // length bytes of a row from (x, y), packed as the mode stores them (pixel bytes each, least significant first).
    static void putSpan(unsigned console, int x, int y, const uint8_t *pixels, int length);
    // What the console shows, row after row and packed in the same way, which also brings it back after a console switch.
    // A program can draw on it directly and then send the area it changed with presentArea(), which costs nothing while
    // the console is not on the screen. Null in text mode, and setMode() replaces it.
    static uint8_t *pixels(unsigned console);
    static void presentArea(unsigned console, int left, int top, int right, int bottom);
    // Goes to the screen over the console's pixels and leaves them as they are: for a mouse pointer.
    static void overlaySpan(unsigned console, int x, int y, const uint8_t *pixels, int count);
    // The pointer that a console switch paints again over the console's pixels, as overlaySpan() drew it: rows of width
    // cells, ' ' transparent, '#' the outline colour and any other the fill colour. Null rows for none. The rows are not
    // copied, so they must stay valid, and setMode() forgets them.
    static void setOverlay(unsigned console, int left, int top, int width, int height, const char *const *rows,
                           uint32_t fill, uint32_t outline);
    static uint32_t getPixel(unsigned console, int x, int y);
    static void clearPixels(unsigned console, uint32_t color);
    // A rectangle of one colour, and a copy of a rectangle onto another one that may overlap it, clipped to the mode:
    // the console's pixels change, and the screen through the SVGA driver's primitives (its 2D engine when
    // acceleration is selected) or, when they cannot, from those pixels.
    static void fillRect(unsigned console, int x, int y, int width, int height, uint32_t color);
    static void copyRect(unsigned console, int fromX, int fromY, int toX, int toY, int width, int height);
    // Returns once what the console's primitives drew is on the screen.
    static void sync(unsigned console);
    static void setPalette(unsigned console, unsigned first, unsigned count, const uint8_t *rgb);
    // A picture of what a console shows, for screenshots: its size in pixels (text drawn with the 8x16 font, without
    // the mouse pointer), and then its rows one at a time as 8-bit red, green and blue. False when the console has no
    // pixels, or for a row once the console has changed to another size.
    static bool pictureSize(unsigned console, unsigned &width, unsigned &height);
    static bool pictureRow(unsigned console, unsigned y, unsigned width, uint8_t *rgb);

    static void putAt(unsigned console, int x, int y, char c, uint8_t attr);
    static void writeAt(unsigned console, int x, int y, const char *s, uint8_t attr);
    static void fillRow(unsigned console, int y, char c, uint8_t attr);
    static void setCursor(unsigned console, int x, int y);
    static void showCursor(unsigned console, bool visible);
    static int cursorX(unsigned console);
    static int cursorY(unsigned console);
    static void setPointer(unsigned console, int x, int y);

private:
    enum class Esc : uint8_t { None, Started, Csi, Charset };
    static constexpr int ESC_PARAMS = 8;

    class Screen {
    public:
        uint16_t cells[MAX_COLS * MAX_ROWS];
        int cx;
        int cy;
        uint8_t attr;
        bool cursorVisible;
        int pointerX;
        int pointerY;
        Esc esc;
        int escParams[ESC_PARAMS];
        uint8_t escCount;
        bool escPrivate;
        bool reversed;
        int savedX;
        int savedY;
        VgaMode::Id mode;
        uint8_t palette[VgaMode::PALETTE_BYTES];
        uint8_t *pixels;
        const char *const *overlayRows;
        int overlayLeft;
        int overlayTop;
        int overlayWidth;
        int overlayHeight;
        uint32_t overlayFill;
        uint32_t overlayOutline;
    };

    class Locked {
    public:
        Locked();
        ~Locked();
    private:
        uint64_t flags;
        bool held;
    };

    // A linear (SVGA) frame buffer is too large to copy with the lock held, which keeps interrupts waiting: show()
    // leaves the copy of the console's pixels to the screen pending, and copyPending() makes it once the lock is
    // released (~Locked), a band of rows per lock. Whatever else draws meanwhile comes from the same pixels.
    static constexpr size_t PENDING_BAND_BYTES = 32 * 1024;

    static Screen screens[COUNT];
    static unsigned activeIndex;
    // The console whose pixels show() has still to copy to the screen, or -1, and the first row not copied yet.
    static int pendingConsole;
    static int pendingRow;
    static VgaMode::Id hardwareMode;
    static bool panicking;
    static SpinLock lock;

    static unsigned resolve(unsigned console) { return panicking ? activeIndex : console % COUNT; }
    static bool visible(unsigned console) {
        return console == activeIndex && VgaMode::isConsoleText(screens[console].mode);
    }
    static int columns(const Screen &screen) {
        return VgaMode::isConsoleText(screen.mode) ? VgaMode::info(screen.mode).width : COLS;
    }
    static int rows(const Screen &screen) {
        return VgaMode::isConsoleText(screen.mode) ? VgaMode::info(screen.mode).height : ROWS;
    }
    static uint16_t &cell(Screen &screen, int x, int y) { return screen.cells[y * MAX_COLS + x]; }
    static const uint16_t &cell(const Screen &screen, int x, int y) { return screen.cells[y * MAX_COLS + x]; }
    static bool framebufferText(const Screen &screen) {
        return VgaMode::info(screen.mode).layout == VgaMode::Layout::FramebufferText;
    }
    static bool clipSpan(const VgaMode::Info &info, int &x, int y, const uint8_t *&pixels, int &count);
    static void show(unsigned console);
    static void copyPending();
    static void paintOverlay(const Screen &s);
    static void leaveGraphics(unsigned console);
    static void paintAll();
    static void paintCell(const Screen &s, int x, int y);
    static void paintFramebufferCell(const Screen &s, int x, int y);
    static bool pictureSizeOf(const Screen &s, unsigned &width, unsigned &height);
    static void pictureTextRow(const Screen &s, unsigned y, uint8_t *rgb);
    static void pictureGraphicsRow(const Screen &s, unsigned y, unsigned width, uint8_t *rgb);
    static void paintCursor(const Screen &s);
    static void scroll(unsigned console);
    static void escape(unsigned console, char c);
    static void csi(unsigned console, char final);
    static void rendition(Screen &s);
    static void eraseCells(Screen &s, int y, int from, int to);
    static void paintRow(unsigned console, int y);
    static void presentRows(const Screen &s, int left, int top, int width, int height);
    static void cursorMoved(unsigned console, int oldX, int oldY);
};

#endif
