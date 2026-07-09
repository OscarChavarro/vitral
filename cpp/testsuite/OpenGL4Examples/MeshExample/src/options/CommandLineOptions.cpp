#include "java/lang/String.h"
#include "model/MeshModel.h"
#include "options/CommandLineOptions.h"

CommandLineOptions::CommandLineOptions(MeshModel* model)
    : model(model)
{
}

void CommandLineOptions::processArguments(int argc, char** argv)
{
    if ( argv == 0 ) {
        return;
    }

    for ( int i = 0; i < argc; i++ ) {
        java::String arg(argv[i]);
        if ( arg == "-tangibleServer" && i + 1 < argc ) {
            model->setTangibleServiceUrl(java::String(argv[++i]));
        }
    }
}
