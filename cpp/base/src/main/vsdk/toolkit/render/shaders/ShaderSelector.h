#ifndef __SHADERSELECTOR__
#define __SHADERSELECTOR__

class RendererConfiguration;
class Shader;

class ShaderSelector {
public:
    static Shader* select(const RendererConfiguration* qualitySelection);
};

#endif
