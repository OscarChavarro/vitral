#include "programs/sceneEditor/SceneEditorProgram.h"
#include "kernel/ArgList.h"

int
sceneEditorMain(int argc, char *argv[]);

int
SceneEditorProgram::main(const ArgList &args) {
    char *argv[ArgList::MAX_ARGS + 2];
    int argc = 0;
    argv[argc++] = const_cast<char *>("sceneEditor");
    for (unsigned i = 0; i < args.count(); i++) {
        argv[argc++] = const_cast<char *>(args.at(i));
    }
    argv[argc] = nullptr;
    return sceneEditorMain(argc, argv);
}
