#ifndef __SHADEROPERATIONMODE__
#define __SHADEROPERATIONMODE__

enum class ShaderOperationMode {
    OPENGL_4_1,
    SOFTWARE,
};

inline ShaderOperationMode nextShaderOperationMode(ShaderOperationMode mode)
{
    return mode == ShaderOperationMode::OPENGL_4_1
        ? ShaderOperationMode::SOFTWARE
        : ShaderOperationMode::OPENGL_4_1;
}

#endif
