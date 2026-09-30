#include "java/lang/String.h"
#include "model/SolidTextureModel.h"
#include "options/CommandLineOptions.h"

CommandLineOptions::CommandLineOptions(SolidTextureModel* model)
    : model(model)
{
}

void CommandLineOptions::processArguments(int argc, char** argv)
{
    if ( model == 0 || argv == 0 ) return;
    for ( int i = 1; i < argc; i++ ) {
        java::String arg(argv[i]);
        if ( arg == "-tangibleServer" && i + 1 < argc ) {
            model->setTangibleServiceUrl(java::String(argv[++i]));
        }
    }
}
