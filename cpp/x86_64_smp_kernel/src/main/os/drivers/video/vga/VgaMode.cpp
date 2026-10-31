#include "drivers/video/vga/VgaMode.h"
#include "drivers/video/svga/Svga.h"
#include "lib/vitralKernel/Ports.h"
#include "drivers/video/vga/ModeRegisters.h"

uint8_t VgaMode::fontCopy[VgaMode::FONT_BYTES];
bool VgaMode::fontSaved = false;

using L = VgaMode::Layout;

const VgaMode::Info VgaMode::table[VgaMode::COUNT] = {
    {80,  25,  16,  L::Text,      0, 0},
    {320, 200, 16,  L::Planar,    4, 0},
    {640, 200, 16,  L::Planar,    4, 0},
    {640, 350, 16,  L::Planar,    4, 0},
    {640, 480, 2,   L::Planar,    1, 0},
    {640, 480, 16,  L::Planar,    4, 0},
    {320, 200, 256, L::Chain4,    0, 0},
    {320, 240, 256, L::Unchained, 0, 0},
    {320, 400, 256, L::Unchained, 0, 0},
    {360, 480, 256, L::Unchained, 0, 0},
    {720, 348, 2,   L::Planar,    1, 0},
    {400, 300, 256, L::Unchained, 0, 0},
    {800, 600, 2, L::Linear, 0, 1},
    {800, 600, 16, L::Linear, 0, 1},
    {800, 600, 256, L::Linear, 0, 1},
    {800, 600, 65536, L::Linear, 0, 2},
    {800, 600, 16777216, L::Linear, 0, 3},
    {1024, 768, 2, L::Linear, 0, 1},
    {1024, 768, 16, L::Linear, 0, 1},
    {1024, 768, 256, L::Linear, 0, 1},
    {1024, 768, 65536, L::Linear, 0, 2},
    {1024, 768, 16777216, L::Linear, 0, 3},
    {1280, 1024, 2, L::Linear, 0, 1},
    {1280, 1024, 16, L::Linear, 0, 1},
    {1280, 1024, 256, L::Linear, 0, 1},
    {1280, 1024, 65536, L::Linear, 0, 2},
    {1280, 1024, 16777216, L::Linear, 0, 3},
    {1600, 1200, 2, L::Linear, 0, 1},
    {1600, 1200, 16, L::Linear, 0, 1},
    {1600, 1200, 256, L::Linear, 0, 1},
    {1600, 1200, 65536, L::Linear, 0, 2},
    {1600, 1200, 16777216, L::Linear, 0, 3},
    {1920, 1080, 2, L::Linear, 0, 1},
    {1920, 1080, 16, L::Linear, 0, 1},
    {1920, 1080, 256, L::Linear, 0, 1},
    {1920, 1080, 65536, L::Linear, 0, 2},
    {1920, 1080, 16777216, L::Linear, 0, 3},
    {80,  40,  16, L::Text, 0, 0},
    {80,  50,  16, L::Text, 0, 0},
    {80,  43,  16, L::Text, 0, 0},
    {80,  30,  16, L::Text, 0, 0},
    {80,  60,  16, L::Text, 0, 0},
    {90,  25,  16, L::Text, 0, 0},
    {90,  30,  16, L::Text, 0, 0},
    {90,  43,  16, L::Text, 0, 0},
    {90,  50,  16, L::Text, 0, 0},
    {90,  60,  16, L::Text, 0, 0},
    {80, 25, 16777216, L::FramebufferText, 0, 3},
};

static const ModeRegisters REGISTERS[VgaMode::VGA_COUNT] = {
    {0x67, {0x03, 0x00, 0x03, 0x00, 0x02},
     {0x5F, 0x4F, 0x50, 0x82, 0x55, 0x81, 0xBF, 0x1F, 0x00, 0x4F, 0x0D, 0x0E, 0x00, 0x00, 0x00, 0x00,
      0x9C, 0x0E, 0x8F, 0x28, 0x1F, 0x96, 0xB9, 0xA3, 0xFF},
     {0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x0E, 0x00, 0xFF},
     {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x14, 0x07, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,
      0x0C, 0x00, 0x0F, 0x08, 0x00}},
    {0x63, {0x03, 0x09, 0x0F, 0x00, 0x06},
     {0x2D, 0x27, 0x28, 0x90, 0x2B, 0x80, 0xBF, 0x1F, 0x00, 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x9C, 0x8E, 0x8F, 0x14, 0x00, 0x96, 0xB9, 0xE3, 0xFF},
     {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x0F, 0xFF},
     {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x14, 0x07, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,
      0x01, 0x00, 0x0F, 0x00, 0x00}},
    {0x63, {0x03, 0x01, 0x0F, 0x00, 0x06},
     {0x5F, 0x4F, 0x50, 0x82, 0x54, 0x80, 0xBF, 0x1F, 0x00, 0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x9C, 0x8E, 0x8F, 0x28, 0x00, 0x96, 0xB9, 0xE3, 0xFF},
     {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x0F, 0xFF},
     {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x14, 0x07, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,
      0x01, 0x00, 0x0F, 0x00, 0x00}},
    {0xA3, {0x03, 0x01, 0x0F, 0x00, 0x06},
     {0x5B, 0x4F, 0x50, 0x82, 0x54, 0x80, 0xBF, 0x1F, 0x00, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x83, 0x85, 0x5D, 0x28, 0x0F, 0x63, 0xBA, 0xE3, 0xFF},
     {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x0F, 0xFF},
     {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x14, 0x07, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,
      0x01, 0x00, 0x0F, 0x00, 0x00}},
    {0xE3, {0x03, 0x01, 0x0F, 0x00, 0x06},
     {0x5F, 0x4F, 0x50, 0x82, 0x54, 0x80, 0x0B, 0x3E, 0x00, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0xEA, 0x0C, 0xDF, 0x28, 0x00, 0xE7, 0x04, 0xE3, 0xFF},
     {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x0F, 0xFF},
     {0x00, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F,
      0x01, 0x00, 0x01, 0x00, 0x00}},
    {0xE3, {0x03, 0x01, 0x0F, 0x00, 0x06},
     {0x5F, 0x4F, 0x50, 0x82, 0x54, 0x80, 0x0B, 0x3E, 0x00, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0xEA, 0x0C, 0xDF, 0x28, 0x00, 0xE7, 0x04, 0xE3, 0xFF},
     {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x0F, 0xFF},
     {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x14, 0x07, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,
      0x01, 0x00, 0x0F, 0x00, 0x00}},
    {0x63, {0x03, 0x01, 0x0F, 0x00, 0x0E},
     {0x5F, 0x4F, 0x50, 0x82, 0x54, 0x80, 0xBF, 0x1F, 0x00, 0x41, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x9C, 0x0E, 0x8F, 0x28, 0x40, 0x96, 0xB9, 0xA3, 0xFF},
     {0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x05, 0x0F, 0xFF},
     {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
      0x41, 0x00, 0x0F, 0x00, 0x00}},
    {0xE3, {0x03, 0x01, 0x0F, 0x00, 0x06},
     {0x5F, 0x4F, 0x50, 0x82, 0x54, 0x80, 0x0D, 0x3E, 0x00, 0x41, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0xEA, 0xAC, 0xDF, 0x28, 0x00, 0xE7, 0x06, 0xE3, 0xFF},
     {0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x05, 0x0F, 0xFF},
     {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
      0x41, 0x00, 0x0F, 0x00, 0x00}},
    {0x63, {0x03, 0x01, 0x0F, 0x00, 0x06},
     {0x5F, 0x4F, 0x50, 0x82, 0x54, 0x80, 0xBF, 0x1F, 0x00, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x9C, 0x8E, 0x8F, 0x28, 0x00, 0x96, 0xB9, 0xE3, 0xFF},
     {0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x05, 0x0F, 0xFF},
     {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
      0x41, 0x00, 0x0F, 0x00, 0x00}},
    // The modes only svgalib's generic VGA driver sets (no BIOS mode): its timings, with this file's 256 / 2 colour
    // attribute and graphics registers.
    {0xE7, {0x03, 0x01, 0x0F, 0x00, 0x06},
     {0x6B, 0x59, 0x5A, 0x8E, 0x5E, 0x8A, 0x0D, 0x3E, 0x00, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0xEA, 0xAC, 0xDF, 0x2D, 0x00, 0xE7, 0x06, 0xE3, 0xFF},
     {0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x05, 0x0F, 0xFF},
     {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
      0x41, 0x00, 0x0F, 0x00, 0x00}},
    {0xA7, {0x03, 0x01, 0x0F, 0x00, 0x06},
     {0x6B, 0x59, 0x5A, 0x8E, 0x5E, 0x8A, 0xBF, 0x1F, 0x00, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x83, 0x85, 0x5D, 0x2D, 0x0F, 0x63, 0xBA, 0xE3, 0xFF},
     {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x0F, 0xFF},
     {0x00, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F, 0x3F,
      0x01, 0x00, 0x01, 0x00, 0x00}},
    {0xA7, {0x03, 0x01, 0x0F, 0x00, 0x06},
     {0x71, 0x63, 0x64, 0x92, 0x65, 0x82, 0x46, 0x1F, 0x00, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x31, 0x80, 0x2B, 0x32, 0x00, 0x2F, 0x44, 0xE3, 0xFF},
     {0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x05, 0x0F, 0xFF},
     {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
      0x41, 0x00, 0x0F, 0x00, 0x00}},
};

static constexpr uint16_t SEQ = 0x3C4;
static constexpr uint16_t GC = 0x3CE;
static constexpr uint16_t CRTC = 0x3D4;
static constexpr uint16_t ATTR = 0x3C0;
static constexpr uint16_t MISC = 0x3C2;
static constexpr uint16_t STATUS = 0x3DA;
static constexpr uint16_t DAC_INDEX = 0x3C8;
static constexpr uint16_t DAC_DATA = 0x3C9;
static constexpr uint16_t PEL_MASK = 0x3C6;

void
VgaMode::seq(uint8_t index, uint8_t value) {
    Ports::outb(SEQ, index);
    Ports::outb(SEQ + 1, value);
}
void
VgaMode::crtc(uint8_t index, uint8_t value) {
    Ports::outb(CRTC, index);
    Ports::outb(CRTC + 1, value);
}
void
VgaMode::gc(uint8_t index, uint8_t value) {
    Ports::outb(GC, index);
    Ports::outb(GC + 1, value);
}

static void
dac(unsigned index, unsigned r, unsigned g, unsigned b) {
    Ports::outb(DAC_INDEX, (uint8_t)index);
    Ports::outb(DAC_DATA, (uint8_t)r);
    Ports::outb(DAC_DATA, (uint8_t)g);
    Ports::outb(DAC_DATA, (uint8_t)b);
}

static void
egaColors() {
    for (unsigned i = 0; i < 8; i++) {
        unsigned r = i & 4 ? 42 : 0;
        unsigned g = i & 2 ? 42 : 0;
        unsigned b = i & 1 ? 42 : 0;
        if (i == 6) {
            g = 21;
        }
        dac(i, r, g, b);
        dac(0x38 + i, r ? 63 : 21, g ? 63 : 21, b ? 63 : 21);
        dac(8 + i, r ? 63 : 21, g ? 63 : 21, b ? 63 : 21);
    }
    dac(0x14, 42, 21, 0);
}

void
VgaMode::loadPalette16() {
    Ports::outb(PEL_MASK, 0xFF);
    egaColors();
}

void
VgaMode::defaultPalette(uint8_t *rgb) {
    for (unsigned i = 0; i < 256; i++) {
        unsigned r = 0;
        unsigned g = 0;
        unsigned b = 0;
        if (i < 16) {
            r = i & 4 ? 42 : 0;
            g = i & 2 ? 42 : 0;
            b = i & 1 ? 42 : 0;
            if (i == 6) {
                g = 21;
            }
            if (i >= 8) {
                r = r ? 63 : 21;
                g = g ? 63 : 21;
                b = b ? 63 : 21;
            }
            if (i == 8) {
                r = g = b = 21;
            }
        } else if (i < 32) {
            r = g = b = (i - 16) * 63 / 15;
        } else if (i < 248) {
            unsigned k = i - 32;
            r = k / 36 * 63 / 5;
            g = k / 6 % 6 * 63 / 5;
            b = k % 6 * 63 / 5;
        } else if (i == 255) {
            r = g = b = 63;
        }
        rgb[i * 3] = (uint8_t)r;
        rgb[i * 3 + 1] = (uint8_t)g;
        rgb[i * 3 + 2] = (uint8_t)b;
    }
}

void
VgaMode::loadPalette(const uint8_t *rgb) {
    Ports::outb(PEL_MASK, 0xFF);
    Ports::outb(DAC_INDEX, 0);
    for (unsigned i = 0; i < PALETTE_BYTES; i++) {
        Ports::outb(DAC_DATA, rgb[i] & 0x3F);
    }
}

void
VgaMode::loadPalette(Id id, const uint8_t *rgb) {
    if (info(id).usesDac()) {
        loadPalette(rgb);
        return;
    }
    if (!hasPalette(id)) {
        return;
    }
    const ModeRegisters &r = REGISTERS[(unsigned)id];
    Ports::outb(PEL_MASK, 0xFF);
    for (unsigned i = 0; i < 16; i++) {
        dac(r.ac[i], rgb[i * 3] & 0x3F, rgb[i * 3 + 1] & 0x3F, rgb[i * 3 + 2] & 0x3F);
    }
}

void
VgaMode::saveFont() {
    if (fontSaved) {
        return;
    }
    gc(4, 2);
    gc(5, 0);
    gc(6, 4);
    volatile const uint8_t *vram = (volatile const uint8_t *)VRAM;
    for (unsigned i = 0; i < FONT_BYTES; i++) {
        fontCopy[i] = vram[i];
    }
    gc(4, 0);
    gc(5, 0x10);
    gc(6, 0x0E);
    fontSaved = true;
}

bool
VgaMode::font(uint8_t *out) {
    if (!fontSaved) {
        return false;
    }
    for (unsigned i = 0; i < FONT_BYTES; i++) {
        out[i] = fontCopy[i];
    }
    return true;
}

uint8_t
VgaMode::fontRow(uint8_t glyph, unsigned row) {
    if (!fontSaved || row >= 16) {
        return 0;
    }
    return fontCopy[(unsigned)glyph * FONT_SLOT + row];
}

unsigned
VgaMode::frameWidth(Id id) {
    if (id == Id::FramebufferText) {
        return Svga::framebufferWidth();
    }
    return info(id).width;
}

unsigned
VgaMode::frameHeight(Id id) {
    if (id == Id::FramebufferText) {
        return Svga::framebufferHeight();
    }
    return info(id).height;
}

void
VgaMode::restoreFont(unsigned glyphRows) {
    if (!fontSaved) {
        return;
    }
    seq(2, 0x04);
    seq(4, 0x07);
    gc(4, 2);
    gc(5, 0);
    gc(6, 4);
    volatile uint8_t *vram = (volatile uint8_t *)VRAM;
    for (unsigned glyph = 0; glyph < FONT_GLYPHS; glyph++) {
        for (unsigned row = 0; row < FONT_SLOT; row++) {
            unsigned source = row < glyphRows ? row * 16 / glyphRows : 0;
            vram[glyph * FONT_SLOT + row] = row < glyphRows ? fontCopy[glyph * FONT_SLOT + source] : 0;
        }
    }
    seq(2, 0x03);
    seq(4, 0x02);
    gc(4, 0);
    gc(5, 0x10);
    gc(6, 0x0E);
}

bool
VgaMode::available(Id id) {
    const Info &m = info(id);
    if (m.layout == Layout::Linear) {
        return Svga::canSet(m.width, m.height, m.bytes);
    }
    if (m.layout == Layout::FramebufferText) {
        return Svga::supportsFramebufferText() && Svga::canSet(frameWidth(id), frameHeight(id), m.bytes);
    }
    return true;
}

void
VgaMode::program(Id id) {
    if (info(id).layout == Layout::FramebufferText) {
        Svga::enter(frameWidth(id), frameHeight(id), info(id).bytes);
        return;
    }
    if (info(id).layout == Layout::Linear) {
        const Info &m = info(id);
        Svga::enter(m.width, m.height, m.bytes);
        return;
    }
    Svga::leave();
    const ModeRegisters &r = REGISTERS[info(id).layout == Layout::Text ? 0 : (unsigned)id];
    seq(0, 0x01);
    Ports::outb(MISC, r.misc);
    for (uint8_t i = 1; i < 5; i++) {
        seq(i, r.seq[i]);
    }
    seq(0, 0x03);
    crtc(0x11, 0x00);
    for (uint8_t i = 0; i < 25; i++) {
        crtc(i, i == 0x11 ? (uint8_t)(r.crtc[i] & 0x7F) : r.crtc[i]);
    }
    for (uint8_t i = 0; i < 9; i++) {
        gc(i, r.gc[i]);
    }
    Ports::inb(STATUS);
    for (uint8_t i = 0; i < 21; i++) { Ports::outb(ATTR, i); Ports::outb(ATTR, r.ac[i]); }
    if (info(id).layout == Layout::Text) {
        unsigned glyphRows = 16;
        if (id == Id::Text80x40) {
            crtc(0x09, 0x09);
            glyphRows = 10;
        } else if (id == Id::Text80x50 || id == Id::Text80x43 || id == Id::Text80x60 ||
                   id == Id::Text90x43 || id == Id::Text90x50 || id == Id::Text90x60) {
            crtc(0x09, 0x07);
            glyphRows = 8;
        } else if (id == Id::Text80x30 || id == Id::Text90x25 || id == Id::Text90x30) {
            crtc(0x09, 0x0F);
        }
        if (id == Id::Text80x43 || id == Id::Text90x43) {
            const ModeRegisters &timing = REGISTERS[(unsigned)Id::G640x350x16];
            for (uint8_t i = 0; i < 25; i++) {
                crtc(i, i == 0x11 ? (uint8_t)(timing.crtc[i] & 0x7F) : timing.crtc[i]);
            }
            crtc(0x17, r.crtc[0x17]);
            crtc(0x09, 0x07);
        } else if (id == Id::Text80x30 || id == Id::Text80x60 || id == Id::Text90x30 || id == Id::Text90x60) {
            const ModeRegisters &timing = REGISTERS[(unsigned)Id::G640x480x2];
            for (uint8_t i = 0; i < 25; i++) {
                crtc(i, i == 0x11 ? (uint8_t)(timing.crtc[i] & 0x7F) : timing.crtc[i]);
            }
            crtc(0x17, r.crtc[0x17]);
            crtc(0x09, (id == Id::Text80x60 || id == Id::Text90x60) ? 0x07 : 0x0F);
        }
        if (id == Id::Text90x25 || id == Id::Text90x30 || id == Id::Text90x43 || id == Id::Text90x50 ||
            id == Id::Text90x60) {
            seq(1, 0x01);
            crtc(0x01, 0x59);
            crtc(0x13, 0x2D);
        }
        loadPalette16();
        restoreFont(glyphRows);
    } else if (info(id).colors != 256) {
        loadPalette16();
    }
}

void
VgaMode::display() {
    Ports::inb(STATUS);
    Ports::outb(ATTR, 0x20);
}

void
VgaMode::blit(Id id, const uint8_t *px) {
    const Info &m = info(id);
    volatile uint8_t *vram = (volatile uint8_t *)VRAM;
    unsigned w = m.width;
    unsigned h = m.height;
    switch (m.layout) {
    case Layout::Linear:
        Svga::blit(px, w, h, m.bytes);
        break;
    case Layout::Chain4:
        seq(2, 0x0F);
        for (unsigned i = 0; i < w * h; i++) {
            vram[i] = px[i];
        }
        break;
    case Layout::Unchained: {
        unsigned rowBytes = w / 4;
        for (unsigned p = 0; p < 4; p++) {
            seq(2, (uint8_t)(1u << p));
            for (unsigned y = 0; y < h; y++) {
                for (unsigned xb = 0; xb < rowBytes; xb++) {
                    vram[y * rowBytes + xb] = px[y * w + xb * 4 + p];
                }
            }
        }
        break;
    }
    case Layout::Planar: {
        gc(1, 0); gc(3, 0); gc(5, 0); gc(8, 0xFF);
        unsigned rowBytes = w / 8;
        for (unsigned p = 0; p < m.planes; p++) {
            seq(2, (uint8_t)(1u << p));
            for (unsigned y = 0; y < h; y++) {
                for (unsigned xb = 0; xb < rowBytes; xb++) {
                    const uint8_t *src = px + y * w + xb * 8;
                    unsigned v = 0;
                    for (unsigned b = 0; b < 8; b++) {
                        v |= (unsigned)((src[b] >> p) & 1) << (7 - b);
                    }
                    vram[y * rowBytes + xb] = (uint8_t)v;
                }
            }
        }
        break;
    }
    default: break;
    }
}

void
VgaMode::plot(Id id, int x, int y, uint32_t color) {
    const Info &m = info(id);
    volatile uint8_t *vram = (volatile uint8_t *)VRAM;
    switch (m.layout) {
    case Layout::Linear:
    case Layout::FramebufferText:
        Svga::plot(x, y, m.bytes, color);
        break;
    case Layout::Chain4:
        vram[y * m.width + x] = (uint8_t)color;
        break;
    case Layout::Unchained:
        seq(2, (uint8_t)(1u << (x & 3)));
        vram[y * (m.width / 4) + (x >> 2)] = (uint8_t)color;
        break;
    case Layout::Planar: {
        unsigned offset = (unsigned)y * (m.width / 8) + (unsigned)(x >> 3);
        seq(2, 0x0F); gc(5, 0); gc(3, 0);
        gc(0, (uint8_t)color); gc(1, 0x0F);
        gc(8, (uint8_t)(0x80 >> (x & 7)));
        (void)vram[offset];
        vram[offset] = 0;
        gc(1, 0); gc(8, 0xFF);
        break;
    }
    default: break;
    }
}
