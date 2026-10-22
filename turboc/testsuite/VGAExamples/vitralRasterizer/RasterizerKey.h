#ifndef __RASTERIZER_KEY__
#define __RASTERIZER_KEY__

/**
Keys of the rasterizer example, named as `RasterizerKey::Escape`... (C++98
replacement enumeration).
*/
struct RasterizerKey {
    enum Value {
        Escape = 27,
        Left = 256,
        Right,
        Up,
        Down,
        Other
    };
};

#endif
