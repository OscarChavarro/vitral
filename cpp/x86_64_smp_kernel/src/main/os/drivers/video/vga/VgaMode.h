#ifndef __VGA_MODE__
#define __VGA_MODE__
#include <stdint.h>

class VgaMode {
public:
    enum class Id : uint8_t {
        Text80x25,
        G320x200x16,
        G640x200x16,
        G640x350x16,
        G640x480x2,
        G640x480x16,
        G320x200x256,
        G320x240x256,
        G320x400x256,
        G360x480x256,
        G720x348x2,
        G400x300x256,
        S800x600x1, S800x600x4, S800x600x8, S800x600x16, S800x600x24,
        S1024x768x1, S1024x768x4, S1024x768x8, S1024x768x16, S1024x768x24,
        S1280x1024x1, S1280x1024x4, S1280x1024x8, S1280x1024x16, S1280x1024x24,
        S1600x1200x1, S1600x1200x4, S1600x1200x8, S1600x1200x16, S1600x1200x24,
        S1920x1080x1, S1920x1080x4, S1920x1080x8, S1920x1080x16, S1920x1080x24,
        Text80x40,
        Text80x50,
        Text80x43,
        Text80x30,
        Text80x60,
        Text90x25,
        Text90x30,
        Text90x43,
        Text90x50,
        Text90x60,
        FramebufferText,
        Count
    };
    static constexpr unsigned VGA_COUNT = (unsigned)Id::S800x600x1;

    enum class Layout : uint8_t {
        Text,
        Planar,
        Chain4,
        Unchained,
        Linear,
        FramebufferText
    };

    class Info {
    public:
        uint16_t width;
        uint16_t height;
        uint32_t colors;
        Layout layout;
        uint8_t planes;
        uint8_t bytes;
        bool graphics() const { return layout != Layout::Text && layout != Layout::FramebufferText; }
        unsigned pixelBytes() const { return layout == Layout::Linear ? bytes : 1; }
        bool usesDac() const {
            return layout == Layout::Chain4 || layout == Layout::Unchained || (layout == Layout::Linear && bytes == 1);
        }
    };

    static constexpr unsigned COUNT = (unsigned)Id::Count;

    static const Info &info(Id id) { return table[(unsigned)id]; }
    static bool isConsoleText(Id id) {
        return info(id).layout == Layout::Text || info(id).layout == Layout::FramebufferText;
    }
    static unsigned frameWidth(Id id);
    static unsigned frameHeight(Id id);

    static void program(Id id);
    static void display();
    static void saveFont();
    static constexpr unsigned FONT_GLYPHS = 256;
    static constexpr unsigned FONT_SLOT = 32;
    static constexpr unsigned FONT_BYTES = FONT_GLYPHS * FONT_SLOT;
    static bool font(uint8_t *out);
    static uint8_t fontRow(uint8_t glyph, unsigned row);

    static constexpr unsigned PALETTE_BYTES = 768;
    static void defaultPalette(uint8_t *rgb);
    static void loadPalette(const uint8_t *rgb);

    static void blit(Id id, const uint8_t *pixels);
    static void plot(Id id, int x, int y, uint32_t color);

    static bool available(Id id);

private:
    static constexpr uintptr_t VRAM = 0xA0000;
    static const Info table[COUNT];
    static uint8_t fontCopy[FONT_BYTES];
    static bool fontSaved;

    static void seq(uint8_t index, uint8_t value);
    static void crtc(uint8_t index, uint8_t value);
    static void gc(uint8_t index, uint8_t value);
    static void loadPalette16();
    static void restoreFont(unsigned glyphRows);
};

#endif
