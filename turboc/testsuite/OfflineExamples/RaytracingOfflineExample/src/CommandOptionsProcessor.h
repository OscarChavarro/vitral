#ifndef __COMMAND_OPTIONS_PROCESSOR__
#define __COMMAND_OPTIONS_PROCESSOR__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"

class CommandOptionsProcessor {
private:
    static const char* DEFAULT_SCENE_FILE;
    static const char* DEFAULT_OUTPUT_FILE_NAME;
    static const int DEFAULT_X_RESOLUTION;
    static const int DEFAULT_Y_RESOLUTION;

    String sceneFile;
    String outputFile;
    BOOLEAN save;
    BOOLEAN parallel;
    BOOLEAN showHelp;
    int xResolution;
    int yResolution;

    static BOOLEAN parseResolution(const String& value, int* x, int* y);

public:
    CommandOptionsProcessor();

    static CommandOptionsProcessor process(const ArrayList<String>& args);
    static void printUsage();

    const String& getSceneFile() const;
    const String& getOutputFile() const;
    BOOLEAN shouldSave() const;
    BOOLEAN shouldUseParallelExecutor() const;
    BOOLEAN shouldShowHelp() const;
    BOOLEAN shouldUseSceneResolution() const;
    int getXResolution() const;
    int getYResolution() const;
};

#endif
#include "java/lang/String.h"
