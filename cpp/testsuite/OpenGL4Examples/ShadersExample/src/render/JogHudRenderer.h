#ifndef SHADERSEXAMPLE_JOGHUDRENDERER_H
#define SHADERSEXAMPLE_JOGHUDRENDERER_H

#include <string>

class RGBImageUncompressed;
class RendererConfiguration;

class JogHudRenderer {
public:
    JogHudRenderer();
    ~JogHudRenderer();

    void draw(
        bool showHud,
        int viewportX,
        int viewportY,
        int viewportWidth,
        int viewportHeight,
        bool gpuMode,
        int meridians,
        int parallels,
        const RendererConfiguration* quality,
        const std::string& cookMaterialLabel);

    void dispose();

private:
    RGBImageUncompressed* hudImage;
    int hudWidth;
    int hudHeight;

    void ensureHudBuffers(int width, int height);
    void clearBlack();
    void putPixelSafe(int x, int y, unsigned char r, unsigned char g, unsigned char b);
    void drawChar5x7(int x, int y, char c);
    void drawText(int x, int y, const std::string& text);
};

#endif
