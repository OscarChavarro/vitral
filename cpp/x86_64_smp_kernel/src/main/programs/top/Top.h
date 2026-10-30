#ifndef __TOP__
#define __TOP__
#include "syscalls/Syscalls.h"
#include "fs/MountInfo.h"
#include "kernel/ArgList.h"

class Top {
public:
    static int main(const ArgList &args);

private:
    static constexpr unsigned MAX_ROWS = 256;
    static constexpr uint64_t REFRESH_MS = 500;
    static constexpr unsigned MAX_CHOICES = 9;
    static constexpr uint8_t HEADER = 0x70;
    static constexpr uint8_t COLUMNS = 0x30;
    static constexpr uint8_t NORMAL = 0x07;
    static constexpr uint8_t ACTIVE = 0x0F;
    static constexpr uint8_t DIM = 0x08;
    static constexpr uint8_t WINDOW = 0x1F;
    static constexpr uint8_t CHOSEN = 0x70;
    static constexpr uint8_t WARNING = 0x4F;
    static constexpr uint8_t GOOD = 0x2F;
    static constexpr char TL = (char)0xDA;
    static constexpr char TR = (char)0xBF;
    static constexpr char BL = (char)0xC0;
    static constexpr char BR = (char)0xD9;
    static constexpr char HORIZONTAL = (char)0xC4;
    static constexpr char VERTICAL = (char)0xB3;

    Top();
    ~Top();
    Top(const Top &) = delete;
    Top &operator=(const Top &) = delete;

    int run();
    void refresh();
    void refreshFilesystems();
    void draw();
    void drawProcesses();
    void drawFilesystems();
    void drawFooter(const char *keys);
    void drawWindow();
    void startSelected();
    void cycleTextMode();
    bool handleKey(int key);

    unsigned console;
    uint32_t pid;
    bool systemTop;

    Syscalls::ProcessInfo *list;
    unsigned count;
    unsigned percentage[MAX_ROWS];
    uint32_t previousPid[MAX_ROWS];
    uint64_t previousTicks[MAX_ROWS];
    unsigned previousCount;
    uint64_t lastTick;
    uint64_t listedAt;
    uint64_t uptimeSeconds;

    enum class Mode { Processes, Filesystems };
    static constexpr unsigned MAX_DEVICES = 8;
    static constexpr unsigned MAX_MOUNTS = 8;
    class DeviceRow {
    public:
        char name[16];
        uint64_t bytes;
        uint32_t sectorSize;
        char mountedOn[64];
    };

    Mode mode;
    DeviceRow devices[MAX_DEVICES];
    unsigned deviceCount;
    unsigned selectedDevice;
    MountInfo mounts[MAX_MOUNTS];
    unsigned mountCount;

    bool windowOpen;
    unsigned selected;
    char status[64];
    uint64_t statusUntil;
};

#endif
