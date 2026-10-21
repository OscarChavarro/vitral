#ifndef __VGA_CONSTANT__
#define __VGA_CONSTANT__

enum VgaConstant {
    PALETTES = 6,
    MAX_FILES = 64,
    FILE_NAME = 48,
    SWATCH = 10,
    MENU_TOP = 4,
    MENU_ROWS = 16,
    MENU_WIDTH = 79,
    KEY_ESC = 27,
    KEY_LEFT = 256,
    KEY_RIGHT,
    KEY_UP,
    KEY_DOWN,
    KEY_PAGE_UP,
    KEY_PAGE_DOWN,
    KEY_OTHER,
    KEY_MOUSE,
    HUD_MARGIN = 5,
    HUD_LINE_GAP = 2,
    HUD_INFO_LINES = 3,
    HUD_MOUSE_LINES = 2,
    HUD_LINES = HUD_INFO_LINES + HUD_MOUSE_LINES,
    HUD_TEXT = 40,
    HUD_COLUMNS = HUD_TEXT,
    FONT_GLYPHS = 256,
    FONT_SLOT = 32,
    GLYPH_WIDTH = 8,
    HUD_EMPTY = 0,
    HUD_SHADOW,
    HUD_INK
};

#endif
