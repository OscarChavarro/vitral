#ifndef __SOLID_TEXTURE_COMMAND_LINE_OPTIONS__
#define __SOLID_TEXTURE_COMMAND_LINE_OPTIONS__

class SolidTextureModel;

class CommandLineOptions {
private:
    SolidTextureModel* model;

public:
    explicit CommandLineOptions(SolidTextureModel* model);
    void processArguments(int argc, char** argv);
};

#endif
