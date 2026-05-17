#ifndef RAYTRACING_OFFLINE_COMMANDOPTIONSPROCESSOR_H
#define RAYTRACING_OFFLINE_COMMANDOPTIONSPROCESSOR_H

#include <string>
#include <vector>

class CommandOptionsProcessor {
private:
    static const char* DEFAULT_SCENE_FILE;
    static const char* DEFAULT_OUTPUT_FILE_NAME;

    std::string sceneFile;
    std::string outputFile;
    bool save;
    bool parallel;
    bool showHelp;

public:
    CommandOptionsProcessor();

    static CommandOptionsProcessor process(const std::vector<std::string>& args);
    static void printUsage();

    const std::string& getSceneFile() const;
    const std::string& getOutputFile() const;
    bool shouldSave() const;
    bool shouldUseParallelExecutor() const;
    bool shouldShowHelp() const;
};

#endif
