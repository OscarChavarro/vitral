#include "vsdk/toolkit/gui/framebuffer/Framebuffer.h"
#include "vsdk/toolkit/gui/framebuffer/FramebufferTheme.h"

FramebufferTheme
FramebufferTheme::decwindows(int thickness)
{
    FramebufferTheme theme;
    theme.desktop = 0x4A6078UL;
    theme.face = 0xC9A994UL;
    theme.light = 0xE8D4C6UL;
    theme.shadow = 0x8C6E5EUL;
    theme.outline = 0x5A4438UL;
    theme.activeFace = 0xA4826FUL;
    theme.activeLight = 0xC9A994UL;
    theme.activeShadow = 0x6A5044UL;
    theme.text = 0x000000UL;
    theme.disabledText = theme.shadow;
    theme.armedFace = theme.face;
    theme.armedText = theme.text;
    theme.shaded = TRUE;
    theme.shadowThickness = thickness;
    return theme;
}

FramebufferTheme
FramebufferTheme::monochrome()
{
    FramebufferTheme theme;
    theme.desktop = 0xFFFFFFUL;
    theme.face = 0xFFFFFFUL;
    theme.light = 0xFFFFFFUL;
    theme.shadow = 0x000000UL;
    theme.outline = 0x000000UL;
    theme.activeFace = 0xFFFFFFUL;
    theme.activeLight = 0xFFFFFFUL;
    theme.activeShadow = 0x000000UL;
    theme.text = 0x000000UL;
    theme.disabledText = 0x000000UL;
    theme.armedFace = 0x000000UL;
    theme.armedText = 0xFFFFFFUL;
    theme.shaded = FALSE;
    theme.shadowThickness = 1;
    return theme;
}

FramebufferTheme
FramebufferTheme::forSurface(const Framebuffer &surface)
{
    if ( surface.bitsPerPixel() <= 1 ) {
        return monochrome();
    }
    return decwindows(surface.width() >= 640 ? 2 : 1);
}

int
FramebufferTheme::colors(unsigned long *out, int size) const
{
    unsigned long all[12];
    all[0] = text;
    all[1] = armedText;
    all[2] = desktop;
    all[3] = face;
    all[4] = light;
    all[5] = shadow;
    all[6] = outline;
    all[7] = activeFace;
    all[8] = activeLight;
    all[9] = activeShadow;
    all[10] = disabledText;
    all[11] = armedFace;
    int count = 0;
    int a;
    for ( a = 0; a < 12; a++ ) {
        unsigned long color = all[a];
        BOOLEAN seen = FALSE;
        int i;
        for ( i = 0; i < count && !seen; i++ ) {
            seen = out[i] == color;
        }
        if ( !seen && count < size ) {
            out[count++] = color;
        }
    }
    return count;
}
