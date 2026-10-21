#ifndef __COMMAND_OPTIONS_PROCESSOR__
#define __COMMAND_OPTIONS_PROCESSOR__

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
    bool save;
    bool parallel;
    bool showHelp;
    int xResolution;
    int yResolution;

    static bool parseResolution(const String& value, int* x, int* y);

public:
    CommandOptionsProcessor();

    static CommandOptionsProcessor process(const ArrayList<String>& args);
    static void printUsage();

    const String& getSceneFile() const;
    const String& getOutputFile() const;
    bool shouldSave() const;
    bool shouldUseParallelExecutor() const;
    bool shouldShowHelp() const;
    bool shouldUseSceneResolution() const;
    int getXResolution() const;
    int getYResolution() const;
};

#endif
#include "java/lang/String.h"
