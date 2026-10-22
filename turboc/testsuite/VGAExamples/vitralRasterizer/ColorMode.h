#ifndef __COLOR_MODE__
#define __COLOR_MODE__

/**
Color modes of the rasterizer example, named as `ColorMode::NEAREST`...
(portable enumeration wrapper).
*/
struct ColorMode {
    enum Value {
        NEAREST,
        ORDERED_DITHERED_STANDARD_PALETTE,
        ORDERED_DITHERED_OPTIMIZED_PALETTE,
        FLOYD_STEINBERG_STANDARD_PALETTE,
        FLOYD_STEINBERG_OPTIMIZED_PALETTE,
        COLOR_MODES
    };
};

#endif
