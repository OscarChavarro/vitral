#ifndef SHADERSEXAMPLE_MODEL_SHADEROPERATIONMODE_H
#define SHADERSEXAMPLE_MODEL_SHADEROPERATIONMODE_H

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
