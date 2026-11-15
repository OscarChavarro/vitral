#ifndef __MESH_COMMAND_LINE_OPTIONS__
#define __MESH_COMMAND_LINE_OPTIONS__

#include "vsdk/toolkit/common/VSDK.h"

class MeshModel;

class CommandLineOptions {
private:
    MeshModel* model;

public:
    explicit CommandLineOptions(MeshModel* model);
    void processArguments(int argc, char** argv);
};

#endif
