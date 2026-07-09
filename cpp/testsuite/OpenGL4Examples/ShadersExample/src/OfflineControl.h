#ifndef __OFFLINE_CONTROL__
#define __OFFLINE_CONTROL__

class CommandLineOptions;

class OfflineControl {
public:
    static int run(const CommandLineOptions& options);
};

#endif
