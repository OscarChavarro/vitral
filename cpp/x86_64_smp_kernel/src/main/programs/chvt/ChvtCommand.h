#ifndef __CHVT_COMMAND__
#define __CHVT_COMMAND__
#include "kernel/ArgList.h"

// chvt [N]: shows the virtual console N (0 to 11, as Alt-F1 to Alt-F12) on the screen; without N, tells which one
// the screen shows.
class ChvtCommand {
public:
    static int main(const ArgList &args);

private:
    static constexpr int STDOUT = 1;

    static void print(const char *text);
};

#endif
