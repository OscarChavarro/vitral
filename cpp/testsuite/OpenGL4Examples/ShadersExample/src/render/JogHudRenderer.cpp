#include <cctype>
#include <cstdio>
#include <cstring>

#include <java/lang/Math.h>
#include "JogHudRenderer.h"
#include <GL/glew.h>
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
static const int HUD_HEIGHT = 64;
static const int HUD_LEFT = 10;
static const int HUD_BASELINE_1 = 12;
static const int HUD_BASELINE_2 = 34;
static const int CHAR_W = 6;

JogHudRenderer::JogHudRenderer() : hudImage(0), hudWidth(0), hudHeight(0) {}

JogHudRenderer::~JogHudRenderer() { dispose(); }

void JogHudRenderer::dispose()
{
    if (hudImage) {
        OpenGL4ImageRenderer::unload(hudImage);
        delete hudImage;
        hudImage = 0;
    }
    hudWidth = 0;
    hudHeight = 0;
}

void JogHudRenderer::ensureHudBuffers(int width, int height)
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

void JogHudRenderer::putPixelSafe(int x, int y, unsigned char r, unsigned char g, unsigned char b)
{
    if (!hudImage) return;
    if (x < 0 || y < 0 || x >= hudWidth || y >= hudHeight) return;
    hudImage->putPixel(x, y, (char)r, (char)g, (char)b);
}

void JogHudRenderer::clearBlack()
{
    if (!hudImage) return;
    for (int y = 0; y < hudHeight; y++) {
        for (int x = 0; x < hudWidth; x++) {
            hudImage->putPixel(x, y, 0, 0, 0);
        }
    }
}

void JogHudRenderer::drawChar5x7(int x, int y, char c)
{
    static const unsigned char fallback[7] = {0x1E,0x11,0x04,0x04,0x04,0x00,0x04}; // '?'
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
        case ' ': { unsigned char t[7]={0,0,0,0,0,0,0}; memcpy(g, t, 7); } break;
        default: memcpy(g, fallback, 7); break;
    }

    for (int row = 0; row < 7; row++) {
        for (int col = 0; col < 5; col++) {
            if ((g[row] >> (4 - col)) & 1) {
                putPixelSafe(x + col, y + row, 255, 255, 255);
            }
        }
    }
}

void JogHudRenderer::drawText(int x, int y, const java::String& text)
{
    int cx = x;
    for (size_t i = 0; i < text.size(); i++) {
        drawChar5x7(cx, y, text[i]);
        cx += CHAR_W;
    }
}

void JogHudRenderer::draw(
    bool showHud,
    int viewportX,
    int viewportY,
    int viewportWidth,
    int viewportHeight,
    bool gpuMode,
    int meridians,
    int parallels,
    const RendererConfiguration* quality,
    const java::String& cookMaterialLabel)
{
    if (!showHud || viewportWidth < 1 || viewportHeight < 1) return;
    (void)quality;
    (void)cookMaterialLabel;

    int targetHudWidth = java::Math::max(1, viewportWidth);
    int targetHudHeight = java::Math::min(HUD_HEIGHT, java::Math::max(1, viewportHeight));
    ensureHudBuffers(targetHudWidth, targetHudHeight);
    clearBlack();

    java::String line1;
    if (gpuMode) {
        int triangles = java::Math::max(0, (parallels - 1) * meridians * 2);
        char buf[256];
        snprintf(buf, sizeof(buf), "MERIDIANS: %d PARALLELS: %d TRIANGLES: %d",
                 meridians, parallels, triangles);
        line1 = java::String(buf);
    }
    else {
        line1 = "RAYTRACING";
    }

    java::String line2 = gpuMode ? "MODE [.]: GPU" : "MODE [.]: CPU";
    java::String line2Right = "SHOW HUD [H]";
    java::String lineCookMaterial;
    if ( quality &&
         quality->getShadingType() == RendererConfiguration::SHADING_TYPE_COOK_TERRANCE ) {
        lineCookMaterial = "SIMPLEMATERIAL [M]: " + cookMaterialLabel;
    }

    drawText(HUD_LEFT, HUD_BASELINE_1, line1);
    drawText(HUD_LEFT, HUD_BASELINE_2, line2);

    int rightWidth = (int)line2Right.size() * CHAR_W;
    int rightX = java::Math::max(HUD_LEFT, hudWidth - rightWidth - HUD_LEFT);
    drawText(rightX, HUD_BASELINE_1, line2Right);
    if (!lineCookMaterial.empty()) {
        int materialWidth = (int)lineCookMaterial.size() * CHAR_W;
        int materialX = java::Math::max(HUD_LEFT, hudWidth - materialWidth - HUD_LEFT);
        drawText(materialX, HUD_BASELINE_2, lineCookMaterial);
    }

    OpenGL4ImageRenderer::unload(hudImage);

    glDisable(GL_DEPTH_TEST);
    int hudX = viewportX;
    int hudY = viewportY + viewportHeight - hudHeight;
    glViewport(hudX, hudY, hudWidth, hudHeight);
    OpenGL4ImageRenderer::draw(hudImage);
    glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
    glEnable(GL_DEPTH_TEST);
}
