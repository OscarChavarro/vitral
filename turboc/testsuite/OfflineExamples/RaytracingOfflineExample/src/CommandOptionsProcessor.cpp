#include <cstdio>

#include "java/util/ArrayList.txx"
#include "CommandOptionsProcessor.h"
const char* CommandOptionsProcessor::DEFAULT_SCENE_FILE = "../../../../etc/geometry/mitscenes/balls.ray";
const char* CommandOptionsProcessor::DEFAULT_OUTPUT_FILE_NAME = "./output.ppm";

CommandOptionsProcessor::CommandOptionsProcessor()
    : sceneFile(DEFAULT_SCENE_FILE), outputFile(DEFAULT_OUTPUT_FILE_NAME),
      save(true), parallel(false), showHelp(false)
{
}

CommandOptionsProcessor CommandOptionsProcessor::process(const java::ArrayList<java::String>& args)
{
    CommandOptionsProcessor options;
    int positionalCount = 0;

    for (long int i = 0; i < args.size(); i++) {
        java::String arg = args.get(i);
        if ( arg == "nosave" || arg == "--nosave" || arg == "-n" ) {
            options.save = false;
            continue;
        }
        if ( arg == "-parallel" || arg == "--parallel" ) {
            options.parallel = true;
            continue;
        }
        if ( arg == "--help" || arg == "-h" ) {
            options.showHelp = true;
            continue;
        }
        if ( arg == "--scene" || arg == "-s" ) {
            if ( i + 1 >= args.size() ) {
                fprintf(stderr, "Missing value for %s\n", arg.c_str());
                options.showHelp = true;
                return options;
            }
            options.sceneFile = args.get(++i);
            continue;
        }
        if ( arg == "--output" || arg == "-o" ) {
            if ( i + 1 >= args.size() ) {
                fprintf(stderr, "Missing value for %s\n", arg.c_str());
                options.showHelp = true;
                return options;
            }
            options.outputFile = args.get(++i);
            continue;
        }
        if ( !arg.empty() && arg[0] == '-' ) {
            fprintf(stderr, "Unknown option: %s\n", arg.c_str());
            options.showHelp = true;
            return options;
        }

        if ( positionalCount == 0 ) options.sceneFile = arg;
        else if ( positionalCount == 1 ) options.outputFile = arg;
        else {
            fprintf(stderr, "Unexpected argument: %s\n", arg.c_str());
            options.showHelp = true;
            return options;
        }
        positionalCount++;
    }

    return options;
}

void CommandOptionsProcessor::printUsage()
{
    printf("Usage: RaytracerSimple [options] [scene_file]\n");
    printf("Options:\n");
    printf("  --scene, -s <file>     MIT scene file (.ray)\n");
    printf("  --output, -o <file>    Output image file (.ppm/.png/.jpg)\n");
    printf("  --nosave, -n           Render only, no image file\n");
    printf("  -parallel, --parallel  Render tiles in parallel\n");
    printf("  --help, -h             Show this help\n\n");
    printf("Legacy compatibility:\n");
    printf("  - `nosave` (without dashes) is still accepted.\n");
}

const java::String& CommandOptionsProcessor::getSceneFile() const { return sceneFile; }
const java::String& CommandOptionsProcessor::getOutputFile() const { return outputFile; }
bool CommandOptionsProcessor::shouldSave() const { return save; }
bool CommandOptionsProcessor::shouldUseParallelExecutor() const { return parallel; }
bool CommandOptionsProcessor::shouldShowHelp() const { return showHelp; }
