#ifndef __VIRTUAL_CONSOLE__
#define __VIRTUAL_CONSOLE__
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
    static void clear(unsigned console, uint8_t attr);
    static void setAttr(unsigned console, uint8_t attr);

    static bool setMode(unsigned console, VgaMode::Id mode);
    static bool setTextMode(VgaMode::Id mode);
    static VgaMode::Id mode(unsigned console);
    static int columns(unsigned console);
    static int rows(unsigned console);
    static void putPixel(unsigned console, int x, int y, uint32_t color);
    // count pixels of a row from (x, y), packed as the mode stores them (pixel bytes each, least significant first).
    static void putSpan(unsigned console, int x, int y, const uint8_t *pixels, int count);
    static uint32_t getPixel(unsigned console, int x, int y);
    static void clearPixels(unsigned console, uint32_t color);
    static void setPalette(unsigned console, unsigned first, unsigned count, const uint8_t *rgb);

    static void putAt(unsigned console, int x, int y, char c, uint8_t attr);
    static void writeAt(unsigned console, int x, int y, const char *s, uint8_t attr);
    static void fillRow(unsigned console, int y, char c, uint8_t attr);
    static void setCursor(unsigned console, int x, int y);
    static void showCursor(unsigned console, bool visible);
    static int cursorX(unsigned console);
    static int cursorY(unsigned console);
    static void setPointer(unsigned console, int x, int y);

private:
    enum class Esc : uint8_t { None, Started, Csi };

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
        int escParams[2];
        uint8_t escIndex;
        bool escPrivate;
        VgaMode::Id mode;
        uint8_t palette[VgaMode::PALETTE_BYTES];
        uint8_t *pixels;
    };

    class Locked {
    public:
        Locked();
        ~Locked();
    private:
        uint64_t flags;
        bool held;
    };

    static Screen screens[COUNT];
    static unsigned activeIndex;
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
    static void show(unsigned console);
    static void leaveGraphics(unsigned console);
    static void paintAll();
    static void paintCell(const Screen &s, int x, int y);
    static void paintFramebufferCell(const Screen &s, int x, int y);
    static void paintCursor(const Screen &s);
    static void scroll(unsigned console);
    static void escape(unsigned console, char c);
    static void csi(unsigned console, char final);
};

#endif
