#include <stdio.h>
#include <stdlib.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "CommandOptionsProcessor.h"
const char* CommandOptionsProcessor::DEFAULT_SCENE_FILE = "../../../../etc/geometry/mitscenes/balls.ray";
const char* CommandOptionsProcessor::DEFAULT_OUTPUT_FILE_NAME = "./output.ppm";

// turboc runs on less powerful machines, usually with a single thread, so
// the scene viewport is replaced by a small resolution unless requested
const int CommandOptionsProcessor::DEFAULT_X_RESOLUTION = 320;
const int CommandOptionsProcessor::DEFAULT_Y_RESOLUTION = 240;

CommandOptionsProcessor::CommandOptionsProcessor()
    : sceneFile(DEFAULT_SCENE_FILE), outputFile(DEFAULT_OUTPUT_FILE_NAME),
      save(TRUE), parallel(FALSE), showHelp(FALSE),
      xResolution(DEFAULT_X_RESOLUTION), yResolution(DEFAULT_Y_RESOLUTION)
{
}

// Parses "<width>x<height>" (e.g. 640x480), or "scene" for the viewport
// defined on the scene file (reported as 0x0)
BOOLEAN CommandOptionsProcessor::parseResolution(const String& value, int* x, int* y)
{
    if ( value == "scene" ) {
        *x = 0;
        *y = 0;
        return TRUE;
    }

    const char* text = value.c_str();
    char* end = 0;
    long w = strtol(text, &end, 10);
    if ( end == text || (*end != 'x' && *end != 'X') ) return FALSE;
    const char* heightText = end + 1;
    long h = strtol(heightText, &end, 10);
    if ( end == heightText || *end != '\0' || w <= 0 || h <= 0 ) return FALSE;
    *x = (int)w;
    *y = (int)h;
    return TRUE;
}

CommandOptionsProcessor CommandOptionsProcessor::process(const ArrayList<String>& args)
{
    CommandOptionsProcessor options;
    int positionalCount = 0;

    for (long int i = 0; i < args.size(); i++) {
        String arg = args.get(i);
        if ( arg == "nosave" || arg == "--nosave" || arg == "-n" ) {
            options.save = FALSE;
            continue;
        }
        if ( arg == "-parallel" || arg == "--parallel" ) {
            options.parallel = TRUE;
            continue;
        }
        if ( arg == "--help" || arg == "-h" ) {
            options.showHelp = TRUE;
            continue;
        }
        if ( arg == "--scene" || arg == "-s" ) {
            if ( i + 1 >= args.size() ) {
                fprintf(stderr, "Missing value for %s\n", arg.c_str());
                options.showHelp = TRUE;
                return options;
            }
            options.sceneFile = args.get(++i);
            continue;
        }
        if ( arg == "--output" || arg == "-o" ) {
            if ( i + 1 >= args.size() ) {
                fprintf(stderr, "Missing value for %s\n", arg.c_str());
                options.showHelp = TRUE;
                return options;
            }
            options.outputFile = args.get(++i);
            continue;
        }
        if ( arg == "--resolution" || arg == "-r" ) {
            if ( i + 1 >= args.size() ) {
                fprintf(stderr, "Missing value for %s\n", arg.c_str());
                options.showHelp = TRUE;
                return options;
            }
            String value = args.get(++i);
            if ( !parseResolution(value, &options.xResolution, &options.yResolution) ) {
                fprintf(stderr, "Invalid resolution: %s\n", value.c_str());
                options.showHelp = TRUE;
                return options;
            }
            continue;
        }
        if ( !arg.empty() && arg[0] == '-' ) {
            fprintf(stderr, "Unknown option: %s\n", arg.c_str());
            options.showHelp = TRUE;
            return options;
        }

        if ( positionalCount == 0 ) options.sceneFile = arg;
        else if ( positionalCount == 1 ) options.outputFile = arg;
        else {
            fprintf(stderr, "Unexpected argument: %s\n", arg.c_str());
            options.showHelp = TRUE;
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
    printf("  --resolution, -r <WxH> Image size (default %dx%d), or `scene`\n",
           DEFAULT_X_RESOLUTION, DEFAULT_Y_RESOLUTION);
    printf("                         to use the viewport of the scene file\n");
    printf("  --nosave, -n           Render only, no image file\n");
    printf("  -parallel, --parallel  Render tiles in parallel\n");
    printf("  --help, -h             Show this help\n\n");
    printf("Legacy compatibility:\n");
    printf("  - `nosave` (without dashes) is still accepted.\n");
}

const String& CommandOptionsProcessor::getSceneFile() const { return sceneFile; }
const String& CommandOptionsProcessor::getOutputFile() const { return outputFile; }
BOOLEAN CommandOptionsProcessor::shouldSave() const { return save; }
BOOLEAN CommandOptionsProcessor::shouldUseParallelExecutor() const { return parallel; }
BOOLEAN CommandOptionsProcessor::shouldShowHelp() const { return showHelp; }
BOOLEAN CommandOptionsProcessor::shouldUseSceneResolution() const { return xResolution <= 0 || yResolution <= 0; }
int CommandOptionsProcessor::getXResolution() const { return xResolution; }
int CommandOptionsProcessor::getYResolution() const { return yResolution; }
