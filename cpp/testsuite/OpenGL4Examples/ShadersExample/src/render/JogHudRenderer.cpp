#include "JogHudRenderer.h"

#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"

#include <GL/glew.h>
#include <algorithm>
#include <cctype>

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
        case 'A': { unsigned char t[7]={0x0E,0x11,0x11,0x1F,0x11,0x11,0x11}; std::copy(t,t+7,g); } break;
        case 'B': { unsigned char t[7]={0x1E,0x11,0x11,0x1E,0x11,0x11,0x1E}; std::copy(t,t+7,g); } break;
        case 'C': { unsigned char t[7]={0x0E,0x11,0x10,0x10,0x10,0x11,0x0E}; std::copy(t,t+7,g); } break;
        case 'D': { unsigned char t[7]={0x1E,0x11,0x11,0x11,0x11,0x11,0x1E}; std::copy(t,t+7,g); } break;
        case 'E': { unsigned char t[7]={0x1F,0x10,0x10,0x1E,0x10,0x10,0x1F}; std::copy(t,t+7,g); } break;
        case 'F': { unsigned char t[7]={0x1F,0x10,0x10,0x1E,0x10,0x10,0x10}; std::copy(t,t+7,g); } break;
        case 'G': { unsigned char t[7]={0x0E,0x11,0x10,0x10,0x13,0x11,0x0E}; std::copy(t,t+7,g); } break;
        case 'H': { unsigned char t[7]={0x11,0x11,0x11,0x1F,0x11,0x11,0x11}; std::copy(t,t+7,g); } break;
        case 'I': { unsigned char t[7]={0x1F,0x04,0x04,0x04,0x04,0x04,0x1F}; std::copy(t,t+7,g); } break;
        case 'J': { unsigned char t[7]={0x1F,0x02,0x02,0x02,0x02,0x12,0x0C}; std::copy(t,t+7,g); } break;
        case 'K': { unsigned char t[7]={0x11,0x12,0x14,0x18,0x14,0x12,0x11}; std::copy(t,t+7,g); } break;
        case 'L': { unsigned char t[7]={0x10,0x10,0x10,0x10,0x10,0x10,0x1F}; std::copy(t,t+7,g); } break;
        case 'M': { unsigned char t[7]={0x11,0x1B,0x15,0x15,0x11,0x11,0x11}; std::copy(t,t+7,g); } break;
        case 'N': { unsigned char t[7]={0x11,0x19,0x15,0x13,0x11,0x11,0x11}; std::copy(t,t+7,g); } break;
        case 'O': { unsigned char t[7]={0x0E,0x11,0x11,0x11,0x11,0x11,0x0E}; std::copy(t,t+7,g); } break;
        case 'P': { unsigned char t[7]={0x1E,0x11,0x11,0x1E,0x10,0x10,0x10}; std::copy(t,t+7,g); } break;
        case 'Q': { unsigned char t[7]={0x0E,0x11,0x11,0x11,0x15,0x12,0x0D}; std::copy(t,t+7,g); } break;
        case 'R': { unsigned char t[7]={0x1E,0x11,0x11,0x1E,0x14,0x12,0x11}; std::copy(t,t+7,g); } break;
        case 'S': { unsigned char t[7]={0x0F,0x10,0x10,0x0E,0x01,0x01,0x1E}; std::copy(t,t+7,g); } break;
        case 'T': { unsigned char t[7]={0x1F,0x04,0x04,0x04,0x04,0x04,0x04}; std::copy(t,t+7,g); } break;
        case 'U': { unsigned char t[7]={0x11,0x11,0x11,0x11,0x11,0x11,0x0E}; std::copy(t,t+7,g); } break;
        case 'V': { unsigned char t[7]={0x11,0x11,0x11,0x11,0x11,0x0A,0x04}; std::copy(t,t+7,g); } break;
        case 'W': { unsigned char t[7]={0x11,0x11,0x11,0x15,0x15,0x15,0x0A}; std::copy(t,t+7,g); } break;
        case 'X': { unsigned char t[7]={0x11,0x11,0x0A,0x04,0x0A,0x11,0x11}; std::copy(t,t+7,g); } break;
        case 'Y': { unsigned char t[7]={0x11,0x11,0x0A,0x04,0x04,0x04,0x04}; std::copy(t,t+7,g); } break;
        case 'Z': { unsigned char t[7]={0x1F,0x01,0x02,0x04,0x08,0x10,0x1F}; std::copy(t,t+7,g); } break;
        case '0': { unsigned char t[7]={0x0E,0x11,0x13,0x15,0x19,0x11,0x0E}; std::copy(t,t+7,g); } break;
        case '1': { unsigned char t[7]={0x04,0x0C,0x04,0x04,0x04,0x04,0x0E}; std::copy(t,t+7,g); } break;
        case '2': { unsigned char t[7]={0x0E,0x11,0x01,0x02,0x04,0x08,0x1F}; std::copy(t,t+7,g); } break;
        case '3': { unsigned char t[7]={0x1E,0x01,0x01,0x0E,0x01,0x01,0x1E}; std::copy(t,t+7,g); } break;
        case '4': { unsigned char t[7]={0x02,0x06,0x0A,0x12,0x1F,0x02,0x02}; std::copy(t,t+7,g); } break;
        case '5': { unsigned char t[7]={0x1F,0x10,0x10,0x1E,0x01,0x01,0x1E}; std::copy(t,t+7,g); } break;
        case '6': { unsigned char t[7]={0x0E,0x10,0x10,0x1E,0x11,0x11,0x0E}; std::copy(t,t+7,g); } break;
        case '7': { unsigned char t[7]={0x1F,0x01,0x02,0x04,0x08,0x08,0x08}; std::copy(t,t+7,g); } break;
        case '8': { unsigned char t[7]={0x0E,0x11,0x11,0x0E,0x11,0x11,0x0E}; std::copy(t,t+7,g); } break;
        case '9': { unsigned char t[7]={0x0E,0x11,0x11,0x0F,0x01,0x01,0x0E}; std::copy(t,t+7,g); } break;
        case '[': { unsigned char t[7]={0x0E,0x08,0x08,0x08,0x08,0x08,0x0E}; std::copy(t,t+7,g); } break;
        case ']': { unsigned char t[7]={0x0E,0x02,0x02,0x02,0x02,0x02,0x0E}; std::copy(t,t+7,g); } break;
        case ':': { unsigned char t[7]={0x00,0x04,0x04,0x00,0x04,0x04,0x00}; std::copy(t,t+7,g); } break;
        case '.': { unsigned char t[7]={0x00,0x00,0x00,0x00,0x00,0x0C,0x0C}; std::copy(t,t+7,g); } break;
        case '-': { unsigned char t[7]={0x00,0x00,0x00,0x1F,0x00,0x00,0x00}; std::copy(t,t+7,g); } break;
        case ' ': { unsigned char t[7]={0,0,0,0,0,0,0}; std::copy(t,t+7,g); } break;
        default: std::copy(fallback,fallback+7,g); break;
    }

    for (int row = 0; row < 7; row++) {
        for (int col = 0; col < 5; col++) {
            if ((g[row] >> (4 - col)) & 1) {
                putPixelSafe(x + col, y + row, 255, 255, 255);
            }
        }
    }
}

void JogHudRenderer::drawText(int x, int y, const std::string& text)
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
    const std::string& cookMaterialLabel)
{
    if (!showHud || viewportWidth < 1 || viewportHeight < 1) return;
    (void)quality;
    (void)cookMaterialLabel;

    int targetHudWidth = std::max(1, viewportWidth);
    int targetHudHeight = std::min(HUD_HEIGHT, std::max(1, viewportHeight));
    ensureHudBuffers(targetHudWidth, targetHudHeight);
    clearBlack();

    std::string line1;
    if (gpuMode) {
        int triangles = std::max(0, (parallels - 1) * meridians * 2);
        line1 = "MERIDIANS: " + std::to_string(meridians)
            + " PARALLELS: " + std::to_string(parallels)
            + " TRIANGLES: " + std::to_string(triangles);
    }
    else {
        line1 = "RAYTRACING";
    }

    std::string line2 = gpuMode ? "MODE [.]: GPU" : "MODE [.]: CPU";
    std::string line2Right = "SHOW HUD [H]";

    drawText(HUD_LEFT, HUD_BASELINE_1, line1);
    drawText(HUD_LEFT, HUD_BASELINE_2, line2);

    int rightWidth = (int)line2Right.size() * CHAR_W;
    int rightX = std::max(HUD_LEFT, hudWidth - rightWidth - HUD_LEFT);
    drawText(rightX, HUD_BASELINE_1, line2Right);

    OpenGL4ImageRenderer::unload(hudImage);

    glDisable(GL_DEPTH_TEST);
    int hudX = viewportX;
    int hudY = viewportY + viewportHeight - hudHeight;
    glViewport(hudX, hudY, hudWidth, hudHeight);
    OpenGL4ImageRenderer::draw(hudImage);
    glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
    glEnable(GL_DEPTH_TEST);
}
