#include "PolygonClippingHudRenderer.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
#include <GL/glew.h>
#include <cctype>
#include <cstring>

static const int HUD_HEIGHT = 122;
static const int CHAR_W = 12;
static const int PIXEL_SCALE = 2;

PolygonClippingHudRenderer::PolygonClippingHudRenderer() : hudImage(0), hudWidth(0), hudHeight(0) {}
PolygonClippingHudRenderer::~PolygonClippingHudRenderer() { dispose(); }

void PolygonClippingHudRenderer::dispose()
{
    if (hudImage) {
        OpenGL4ImageRenderer::unload(hudImage);
        delete hudImage;
        hudImage = 0;
    }
    hudWidth = hudHeight = 0;
}

void PolygonClippingHudRenderer::ensureHudBuffers(int width, int height)
{
    if (width < 1) width = 1;
    if (height < 1) height = 1;
    if (hudImage && hudWidth == width && hudHeight == height) return;
    dispose();
    hudImage = new RGBImageUncompressed();
    hudImage->init(width, height);
    hudWidth = width;
    hudHeight = height;
}

void PolygonClippingHudRenderer::putPixelSafe(int x, int y, unsigned char r, unsigned char g, unsigned char b)
{
    if (!hudImage) return;
    if (x < 0 || y < 0 || x >= hudWidth || y >= hudHeight) return;
    hudImage->putPixel(x, y, (char)r, (char)g, (char)b);
}

void PolygonClippingHudRenderer::clearBlack()
{
    for (int y = 0; y < hudHeight; y++)
        for (int x = 0; x < hudWidth; x++) hudImage->putPixel(x, y, 0, 0, 0);
}

void PolygonClippingHudRenderer::drawChar5x7(int x, int y, char c)
{
    static const unsigned char fallback[7] = {0x1E,0x11,0x04,0x04,0x04,0x00,0x04};
    unsigned char g[7] = {0,0,0,0,0,0,0};
    char u = (char)std::toupper((unsigned char)c);

    switch (u) {
      case 'A': { unsigned char t[7]={0x0E,0x11,0x11,0x1F,0x11,0x11,0x11}; memcpy(g, t, 7); } break;
      case 'B': { unsigned char t[7]={0x1E,0x11,0x11,0x1E,0x11,0x11,0x1E}; memcpy(g, t, 7); } break;
      case 'C': { unsigned char t[7]={0x0E,0x11,0x10,0x10,0x10,0x11,0x0E}; memcpy(g, t, 7); } break;
      case 'D': { unsigned char t[7]={0x1E,0x11,0x11,0x11,0x11,0x11,0x1E}; memcpy(g, t, 7); } break;
      case 'E': { unsigned char t[7]={0x1F,0x10,0x10,0x1E,0x10,0x10,0x1F}; memcpy(g, t, 7); } break;
      case 'F': { unsigned char t[7]={0x1F,0x10,0x10,0x1E,0x10,0x10,0x10}; memcpy(g, t, 7); } break;
      case 'G': { unsigned char t[7]={0x0E,0x11,0x10,0x10,0x13,0x11,0x0E}; memcpy(g, t, 7); } break;
      case 'H': { unsigned char t[7]={0x11,0x11,0x11,0x1F,0x11,0x11,0x11}; memcpy(g, t, 7); } break;
      case 'I': { unsigned char t[7]={0x1F,0x04,0x04,0x04,0x04,0x04,0x1F}; memcpy(g, t, 7); } break;
      case 'J': { unsigned char t[7]={0x1F,0x02,0x02,0x02,0x02,0x12,0x0C}; memcpy(g, t, 7); } break;
      case 'K': { unsigned char t[7]={0x11,0x12,0x14,0x18,0x14,0x12,0x11}; memcpy(g, t, 7); } break;
      case 'L': { unsigned char t[7]={0x10,0x10,0x10,0x10,0x10,0x10,0x1F}; memcpy(g, t, 7); } break;
      case 'M': { unsigned char t[7]={0x11,0x1B,0x15,0x15,0x11,0x11,0x11}; memcpy(g, t, 7); } break;
      case 'N': { unsigned char t[7]={0x11,0x19,0x15,0x13,0x11,0x11,0x11}; memcpy(g, t, 7); } break;
      case 'O': { unsigned char t[7]={0x0E,0x11,0x11,0x11,0x11,0x11,0x0E}; memcpy(g, t, 7); } break;
      case 'P': { unsigned char t[7]={0x1E,0x11,0x11,0x1E,0x10,0x10,0x10}; memcpy(g, t, 7); } break;
      case 'Q': { unsigned char t[7]={0x0E,0x11,0x11,0x11,0x15,0x12,0x0D}; memcpy(g, t, 7); } break;
      case 'R': { unsigned char t[7]={0x1E,0x11,0x11,0x1E,0x14,0x12,0x11}; memcpy(g, t, 7); } break;
      case 'S': { unsigned char t[7]={0x0F,0x10,0x10,0x0E,0x01,0x01,0x1E}; memcpy(g, t, 7); } break;
      case 'T': { unsigned char t[7]={0x1F,0x04,0x04,0x04,0x04,0x04,0x04}; memcpy(g, t, 7); } break;
      case 'U': { unsigned char t[7]={0x11,0x11,0x11,0x11,0x11,0x11,0x0E}; memcpy(g, t, 7); } break;
      case 'V': { unsigned char t[7]={0x11,0x11,0x11,0x11,0x11,0x0A,0x04}; memcpy(g, t, 7); } break;
      case 'W': { unsigned char t[7]={0x11,0x11,0x11,0x15,0x15,0x15,0x0A}; memcpy(g, t, 7); } break;
      case 'X': { unsigned char t[7]={0x11,0x11,0x0A,0x04,0x0A,0x11,0x11}; memcpy(g, t, 7); } break;
      case 'Y': { unsigned char t[7]={0x11,0x11,0x0A,0x04,0x04,0x04,0x04}; memcpy(g, t, 7); } break;
      case 'Z': { unsigned char t[7]={0x1F,0x01,0x02,0x04,0x08,0x10,0x1F}; memcpy(g, t, 7); } break;
      case '0': { unsigned char t[7]={0x0E,0x11,0x13,0x15,0x19,0x11,0x0E}; memcpy(g, t, 7); } break;
      case '1': { unsigned char t[7]={0x04,0x0C,0x04,0x04,0x04,0x04,0x0E}; memcpy(g, t, 7); } break;
      case '2': { unsigned char t[7]={0x0E,0x11,0x01,0x02,0x04,0x08,0x1F}; memcpy(g, t, 7); } break;
      case '3': { unsigned char t[7]={0x1E,0x01,0x01,0x0E,0x01,0x01,0x1E}; memcpy(g, t, 7); } break;
      case '4': { unsigned char t[7]={0x02,0x06,0x0A,0x12,0x1F,0x02,0x02}; memcpy(g, t, 7); } break;
      case '5': { unsigned char t[7]={0x1F,0x10,0x10,0x1E,0x01,0x01,0x1E}; memcpy(g, t, 7); } break;
      case '6': { unsigned char t[7]={0x0E,0x10,0x10,0x1E,0x11,0x11,0x0E}; memcpy(g, t, 7); } break;
      case '7': { unsigned char t[7]={0x1F,0x01,0x02,0x04,0x08,0x08,0x08}; memcpy(g, t, 7); } break;
      case '8': { unsigned char t[7]={0x0E,0x11,0x11,0x0E,0x11,0x11,0x0E}; memcpy(g, t, 7); } break;
      case '9': { unsigned char t[7]={0x0E,0x11,0x11,0x0F,0x01,0x01,0x0E}; memcpy(g, t, 7); } break;
      case '[': { unsigned char t[7]={0x0E,0x08,0x08,0x08,0x08,0x08,0x0E}; memcpy(g, t, 7); } break;
      case ']': { unsigned char t[7]={0x0E,0x02,0x02,0x02,0x02,0x02,0x0E}; memcpy(g, t, 7); } break;
      case ':': { unsigned char t[7]={0x00,0x04,0x04,0x00,0x04,0x04,0x00}; memcpy(g, t, 7); } break;
      case '.': { unsigned char t[7]={0x00,0x00,0x00,0x00,0x00,0x0C,0x0C}; memcpy(g, t, 7); } break;
      case '-': { unsigned char t[7]={0x00,0x00,0x00,0x1F,0x00,0x00,0x00}; memcpy(g, t, 7); } break;
      case '/': { unsigned char t[7]={0x01,0x02,0x02,0x04,0x08,0x08,0x10}; memcpy(g, t, 7); } break;
      case '(': { unsigned char t[7]={0x02,0x04,0x08,0x08,0x08,0x04,0x02}; memcpy(g, t, 7); } break;
      case ')': { unsigned char t[7]={0x08,0x04,0x02,0x02,0x02,0x04,0x08}; memcpy(g, t, 7); } break;
      case ' ': { unsigned char t[7]={0,0,0,0,0,0,0}; memcpy(g, t, 7); } break;
      default: memcpy(g, fallback, 7); break;
    }
    for (int row = 0; row < 7; row++) {
        for (int col = 0; col < 5; col++) {
            if (((g[row] >> (4-col)) & 1) == 0) continue;
            int px = x + col * PIXEL_SCALE;
            int py = y + row * PIXEL_SCALE;
            for (int dy = 0; dy < PIXEL_SCALE; dy++) {
                for (int dx = 0; dx < PIXEL_SCALE; dx++) {
                    putPixelSafe(px + dx, py + dy, 255, 242, 51);
                }
            }
        }
    }
}

void PolygonClippingHudRenderer::drawText(int x, int y, const java::String& text)
{
    int cx = x;
    for (size_t i = 0; i < text.size(); i++) {
        drawChar5x7(cx, y, text[i]);
        cx += CHAR_W;
    }
}

void PolygonClippingHudRenderer::draw(
    int viewportX, int viewportY, int viewportWidth, int viewportHeight,
    const java::String& line1, const java::String& line2, const java::String& line3,
    const java::String& line4, const java::String& line5,
    const java::String& right1, const java::String& right2, const java::String& right3)
{
    int targetHeight = HUD_HEIGHT * PIXEL_SCALE;
    int h = (viewportHeight < targetHeight ? viewportHeight : targetHeight);
    ensureHudBuffers(viewportWidth, h);
    clearBlack();
    drawText(16, 18, line1);
    drawText(16, 42, line2);
    drawText(16, 66, line3);
    drawText(16, 90, line4);
    drawText(16, 114, line5);

    int right1x = hudWidth - 16 - (int)right1.size() * CHAR_W;
    int right2x = hudWidth - 16 - (int)right2.size() * CHAR_W;
    int right3x = hudWidth - 16 - (int)right3.size() * CHAR_W;
    if (right1x < 16) right1x = 16;
    if (right2x < 16) right2x = 16;
    if (right3x < 16) right3x = 16;
    drawText(right1x, 18, right1);
    drawText(right2x, 42, right2);
    drawText(right3x, 66, right3);

    OpenGL4ImageRenderer::unload(hudImage);
    glDisable(GL_DEPTH_TEST);
    glViewport(viewportX, viewportY + viewportHeight - h, viewportWidth, h);
    OpenGL4ImageRenderer::draw(hudImage);
    glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
    glEnable(GL_DEPTH_TEST);
}
