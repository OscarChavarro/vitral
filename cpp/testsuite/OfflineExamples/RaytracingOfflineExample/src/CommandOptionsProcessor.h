#include "java/lang/String.h"
#ifndef RAYTRACING_OFFLINE_COMMANDOPTIONSPROCESSOR_H
#define RAYTRACING_OFFLINE_COMMANDOPTIONSPROCESSOR_H

#include <string>
#include <vector>

class CommandOptionsProcessor {
private:
    static const char* DEFAULT_SCENE_FILE;
    static const char* DEFAULT_OUTPUT_FILE_NAME;

    java::String sceneFile;
    java::String outputFile;
    bool save;
    bool parallel;
    bool showHelp;

public:
    CommandOptionsProcessor();

    static CommandOptionsProcessor process(const std::vector<java::String>& args);
    static void printUsage();

    const java::String& getSceneFile() const;
    const java::String& getOutputFile() const;
    bool shouldSave() const;
    bool shouldUseParallelExecutor() const;
    bool shouldShowHelp() const;
};

#endif
