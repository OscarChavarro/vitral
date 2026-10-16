#include "GlfwSystem.h"
#include <algorithm>
#include <cstdio>
#include <vector>
#include <GLFW/glfw3.h>
#ifdef VITRAL_GLFW_WITH_FREETYPE
#include <ft2build.h>
#include FT_FREETYPE_H
#endif
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#ifdef __APPLE__
#define GLFW_INCLUDE_GLCOREARB
#endif
KeyEvent GlfwSystem::glfw2vsdkKeyEvent(int glfwKey, int glfwMods) {
    KeyEvent event;

    if (glfwMods & GLFW_MOD_SHIFT) {
        event.modifierMask |= KeyEvent::MASK_SHIFT;
    }
    if (glfwMods & GLFW_MOD_CONTROL) {
        event.modifierMask |= KeyEvent::MASK_CTRL;
    }
    if (glfwMods & GLFW_MOD_ALT) {
        event.modifierMask |= KeyEvent::MASK_ALT;
    }

    switch (glfwKey) {
        case GLFW_KEY_ESCAPE:
            event.keycode = KeyEvent::KEY_ESC;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_BACKSPACE:
            event.keycode = KeyEvent::KEY_BACKSPACE;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_TAB:
            event.keycode = KeyEvent::KEY_TAB;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_ENTER:
            event.keycode = KeyEvent::KEY_ENTER;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_SPACE:
            event.keycode = KeyEvent::KEY_SPACE;
            event.unicodeId = ' ';
            break;

        case GLFW_KEY_A:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_A : KeyEvent::KEY_a;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'A' : 'a';
            break;
        case GLFW_KEY_B:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_B : KeyEvent::KEY_b;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'B' : 'b';
            break;
        case GLFW_KEY_C:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_C : KeyEvent::KEY_c;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'C' : 'c';
            break;
        case GLFW_KEY_D:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_D : KeyEvent::KEY_d;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'D' : 'd';
            break;
        case GLFW_KEY_E:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_E : KeyEvent::KEY_e;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'E' : 'e';
            break;
        case GLFW_KEY_F:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_F : KeyEvent::KEY_f;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'F' : 'f';
            break;
        case GLFW_KEY_G:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_G : KeyEvent::KEY_g;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'G' : 'g';
            break;
        case GLFW_KEY_H:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_H : KeyEvent::KEY_h;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'H' : 'h';
            break;
        case GLFW_KEY_I:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_I : KeyEvent::KEY_i;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'I' : 'i';
            break;
        case GLFW_KEY_J:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_J : KeyEvent::KEY_j;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'J' : 'j';
            break;
        case GLFW_KEY_K:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_K : KeyEvent::KEY_k;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'K' : 'k';
            break;
        case GLFW_KEY_L:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_L : KeyEvent::KEY_l;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'L' : 'l';
            break;
        case GLFW_KEY_M:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_M : KeyEvent::KEY_m;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'M' : 'm';
            break;
        case GLFW_KEY_N:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_N : KeyEvent::KEY_n;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'N' : 'n';
            break;
        case GLFW_KEY_O:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_O : KeyEvent::KEY_o;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'O' : 'o';
            break;
        case GLFW_KEY_P:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_P : KeyEvent::KEY_p;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'P' : 'p';
            break;
        case GLFW_KEY_Q:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_Q : KeyEvent::KEY_q;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'Q' : 'q';
            break;
        case GLFW_KEY_R:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_R : KeyEvent::KEY_r;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'R' : 'r';
            break;
        case GLFW_KEY_S:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_S : KeyEvent::KEY_s;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'S' : 's';
            break;
        case GLFW_KEY_T:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_T : KeyEvent::KEY_t;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'T' : 't';
            break;
        case GLFW_KEY_U:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_U : KeyEvent::KEY_u;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'U' : 'u';
            break;
        case GLFW_KEY_V:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_V : KeyEvent::KEY_v;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'V' : 'v';
            break;
        case GLFW_KEY_W:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_W : KeyEvent::KEY_w;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'W' : 'w';
            break;
        case GLFW_KEY_X:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_X : KeyEvent::KEY_x;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'X' : 'x';
            break;
        case GLFW_KEY_Y:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_Y : KeyEvent::KEY_y;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'Y' : 'y';
            break;
        case GLFW_KEY_Z:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_Z : KeyEvent::KEY_z;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'Z' : 'z';
            break;

        case GLFW_KEY_0:
            event.keycode = KeyEvent::KEY_0;
            event.unicodeId = '0';
            break;
        case GLFW_KEY_1:
            event.keycode = KeyEvent::KEY_1;
            event.unicodeId = '1';
            break;
        case GLFW_KEY_2:
            event.keycode = KeyEvent::KEY_2;
            event.unicodeId = '2';
            break;
        case GLFW_KEY_3:
            event.keycode = KeyEvent::KEY_3;
            event.unicodeId = '3';
            break;
        case GLFW_KEY_4:
            event.keycode = KeyEvent::KEY_4;
            event.unicodeId = '4';
            break;
        case GLFW_KEY_5:
            event.keycode = KeyEvent::KEY_5;
            event.unicodeId = '5';
            break;
        case GLFW_KEY_6:
            event.keycode = KeyEvent::KEY_6;
            event.unicodeId = '6';
            break;
        case GLFW_KEY_7:
            event.keycode = KeyEvent::KEY_7;
            event.unicodeId = '7';
            break;
        case GLFW_KEY_8:
            event.keycode = KeyEvent::KEY_8;
            event.unicodeId = '8';
            break;
        case GLFW_KEY_9:
            event.keycode = KeyEvent::KEY_9;
            event.unicodeId = '9';
            break;
        case GLFW_KEY_PERIOD:
            event.keycode = KeyEvent::KEY_PERIOD;
            event.unicodeId = '.';
            break;

        case GLFW_KEY_F1:
            event.keycode = KeyEvent::KEY_F1;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F2:
            event.keycode = KeyEvent::KEY_F2;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F3:
            event.keycode = KeyEvent::KEY_F3;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F4:
            event.keycode = KeyEvent::KEY_F4;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F5:
            event.keycode = KeyEvent::KEY_F5;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F6:
            event.keycode = KeyEvent::KEY_F6;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F7:
            event.keycode = KeyEvent::KEY_F7;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F8:
            event.keycode = KeyEvent::KEY_F8;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F9:
            event.keycode = KeyEvent::KEY_F9;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F10:
            event.keycode = KeyEvent::KEY_F10;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F11:
            event.keycode = KeyEvent::KEY_F11;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F12:
            event.keycode = KeyEvent::KEY_F12;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;

        case GLFW_KEY_UP:
            event.keycode = KeyEvent::KEY_UP;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_DOWN:
            event.keycode = KeyEvent::KEY_DOWN;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_LEFT:
            event.keycode = KeyEvent::KEY_LEFT;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_RIGHT:
            event.keycode = KeyEvent::KEY_RIGHT;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;

        case GLFW_KEY_HOME:
            event.keycode = KeyEvent::KEY_HOME;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_END:
            event.keycode = KeyEvent::KEY_END;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_PAGE_UP:
            event.keycode = KeyEvent::KEY_PAGEUP;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_PAGE_DOWN:
            event.keycode = KeyEvent::KEY_PAGEDOWN;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_INSERT:
            event.keycode = KeyEvent::KEY_INSERT;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_DELETE:
            event.keycode = KeyEvent::KEY_DELETE;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;

        default:
            event.keycode = KeyEvent::KEY_NONE;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
    }

    return event;
}

KeyEvent GlfwSystem::glfw2vsdkSpecialKeyEvent(int glfwKey, int glfwMods) {
    return glfw2vsdkKeyEvent(glfwKey, glfwMods);
}

MouseEvent GlfwSystem::glfw2vsdkMouseEvent(int glfwButton, int glfwAction, double x, double y) {
    MouseEvent event;
    event.setX((int)x);
    event.setY((int)y);

    switch (glfwButton) {
        case GLFW_MOUSE_BUTTON_LEFT:
            event.setButton(MouseEvent::BUTTON1);
            break;
        case GLFW_MOUSE_BUTTON_MIDDLE:
            event.setButton(MouseEvent::BUTTON2);
            break;
        case GLFW_MOUSE_BUTTON_RIGHT:
            event.setButton(MouseEvent::BUTTON3);
            break;
        default:
            event.setButton(0);
            break;
    }

    int modifiers = 0;
    if (glfwAction == GLFW_PRESS) {
        switch (glfwButton) {
            case GLFW_MOUSE_BUTTON_LEFT:
                modifiers |= MouseEvent::BUTTON1_DOWN_MASK;
                break;
            case GLFW_MOUSE_BUTTON_MIDDLE:
                modifiers |= MouseEvent::BUTTON2_DOWN_MASK;
                break;
            case GLFW_MOUSE_BUTTON_RIGHT:
                modifiers |= MouseEvent::BUTTON3_DOWN_MASK;
                break;
        }
    }
    event.setModifiers(modifiers);

    return event;
}

MouseEvent GlfwSystem::glfw2vsdkMotionEvent(double x, double y) {
    MouseEvent event;
    event.setX((int)x);
    event.setY((int)y);
    return event;
}

MouseEvent GlfwSystem::glfw2vsdkWheelEvent(double xoffset, double yoffset) {
    MouseEvent event;
    event.setClicks((int)yoffset);
    return event;
}

//===========================================================================
//= Text labels                                                             =
//===========================================================================

#ifdef VITRAL_GLFW_WITH_FREETYPE
namespace {

/**
Sans serif fonts tried in order: Java asks AWT for "Arial", so its metric
compatible substitutes come first, and the font distributed with VitralSDK
(seen from the testsuite and application folders) is the last resort.
*/
const char* const LABEL_FONT_FILES[] = {
    "/System/Library/Fonts/Supplemental/Arial.ttf",
    "/Library/Fonts/Arial.ttf",
    "/usr/share/fonts/truetype/msttcorefonts/Arial.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
    "/usr/share/fonts/liberation-sans/LiberationSans-Regular.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/usr/share/fonts/TTF/DejaVuSans.ttf",
    "C:/Windows/Fonts/arial.ttf",
    "../../../../etc/fonts/notoSansLight.ttf",
    "../../../etc/fonts/notoSansLight.ttf",
    "../../etc/fonts/notoSansLight.ttf",
    "../etc/fonts/notoSansLight.ttf",
    "etc/fonts/notoSansLight.ttf"
};

/**
@return the FreeType face used for every label, loaded on first use, or
null when FreeType could not start or no font file was found
*/
FT_Face labelFace()
{
    static bool initialized = false;
    static FT_Library library = 0;
    static FT_Face face = 0;

    if ( initialized ) {
        return face;
    }
    initialized = true;
    if ( FT_Init_FreeType(&library) != 0 ) {
        library = 0;
        return 0;
    }
    const int n = sizeof(LABEL_FONT_FILES) / sizeof(LABEL_FONT_FILES[0]);
    for ( int i = 0; i < n && face == 0; i++ ) {
        if ( FT_New_Face(library, LABEL_FONT_FILES[i], 0, &face) != 0 ) {
            face = 0;
        }
    }
    if ( face == 0 ) {
        std::fprintf(stderr, "GlfwSystem: no font found for text labels\n");
    }
    return face;
}

/** Decodes an UTF-8 text into its code points (malformed bytes are kept). */
std::vector<unsigned long> decodeUtf8(const char* text)
{
    std::vector<unsigned long> codePoints;
    const unsigned char* p = reinterpret_cast<const unsigned char*>(text);
    while ( p != 0 && *p != 0 ) {
        unsigned long c = *p;
        int extra = 0;
        if ( (c & 0xE0) == 0xC0 ) { c &= 0x1F; extra = 1; }
        else if ( (c & 0xF0) == 0xE0 ) { c &= 0x0F; extra = 2; }
        else if ( (c & 0xF8) == 0xF0 ) { c &= 0x07; extra = 3; }
        p++;
        for ( ; extra > 0 && (*p & 0xC0) == 0x80; extra--, p++ ) {
            c = (c << 6) | (*p & 0x3F);
        }
        codePoints.push_back(c);
    }
    return codePoints;
}

} // namespace
#endif

RGBAImageUncompressed* GlfwSystem::calculateLabelImage(
    const char* text, const ColorRgb& color, int fontSize)
{
    RGBAImageUncompressed* image = new RGBAImageUncompressed();
    const char r = static_cast<char>(color.r() * 255.0);
    const char g = static_cast<char>(color.g() * 255.0);
    const char b = static_cast<char>(color.b() * 255.0);

#ifdef VITRAL_GLFW_WITH_FREETYPE
    FT_Face face = labelFace();
    if ( face != 0 &&
         FT_Set_Pixel_Sizes(face, 0, std::max(1, fontSize)) == 0 ) {
        const int margin = 2;
        const std::vector<unsigned long> codePoints = decodeUtf8(text);
        const int ascender = static_cast<int>((face->size->metrics.ascender + 63) >> 6);
        const int descender = static_cast<int>(-(face->size->metrics.descender >> 6));

        // First pass measures the text, second one draws its glyphs
        int textWidth = 0;
        for ( size_t i = 0; i < codePoints.size(); i++ ) {
            if ( FT_Load_Char(face, codePoints[i], FT_LOAD_DEFAULT) == 0 ) {
                textWidth += static_cast<int>((face->glyph->advance.x + 63) >> 6);
            }
        }
        const int width = std::max(1, textWidth + 2 * margin);
        const int height = std::max(1, ascender + descender + 2 * margin);
        std::vector<unsigned char> alpha(width * height, 0);

        int penX = margin;
        const int baseline = margin + ascender;
        for ( size_t i = 0; i < codePoints.size(); i++ ) {
            if ( FT_Load_Char(face, codePoints[i], FT_LOAD_RENDER) != 0 ) {
                continue;
            }
            const FT_GlyphSlot glyph = face->glyph;
            const FT_Bitmap& bitmap = glyph->bitmap;
            for ( int y = 0; y < static_cast<int>(bitmap.rows); y++ ) {
                const int ty = baseline - glyph->bitmap_top + y;
                if ( ty < 0 || ty >= height ) continue;
                for ( int x = 0; x < static_cast<int>(bitmap.width); x++ ) {
                    const int tx = penX + glyph->bitmap_left + x;
                    if ( tx < 0 || tx >= width ) continue;
                    unsigned char a = bitmap.buffer[y * bitmap.pitch + x];
                    unsigned char& target = alpha[ty * width + tx];
                    target = std::max(target, a);
                }
            }
            penX += static_cast<int>((glyph->advance.x + 63) >> 6);
        }

        std::vector<char> rgba(width * height * 4);
        for ( int y = 0; y < height; y++ ) {
            for ( int x = 0; x < width; x++ ) {
                const int out = ((height - 1 - y) * width + x) * 4;
                rgba[out + 0] = r;
                rgba[out + 1] = g;
                rgba[out + 2] = b;
                rgba[out + 3] = static_cast<char>(alpha[y * width + x]);
            }
        }
        image->setRawImage(width, height, rgba.data());
        return image;
    }
#else
    (void)text;
    (void)fontSize;
#endif

    char transparent[4] = { r, g, b, 0 };
    image->setRawImage(1, 1, transparent);
    return image;
}
