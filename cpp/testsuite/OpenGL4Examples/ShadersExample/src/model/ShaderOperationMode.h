#ifndef __SHADER_OPERATION_MODE__
#define __SHADER_OPERATION_MODE__

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
