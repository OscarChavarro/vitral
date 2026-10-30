#include "programs/top/Top.h"
#include "drivers/keyboard/Keyboard.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "drivers/video/vga/VgaMode.h"
#include "kernel/Clock.h"
#include "kernel/Idle.h"
#include "kernel/Process.h"
#include "kernel/KernelLog.h"
#include "kernel/Smp.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"
#include "fs/OpenFlag.h"
#include "fs/BlockIoctl.h"
#include "fs/DirRecord.h"
#include "kernel/ArgList.h"
#include "kernel/ProgramRegistry.h"

void
Top::refresh() {
    if (mode == Mode::Filesystems) {
        listedAt = Syscalls::uptimeMs();
        uptimeSeconds = Syscalls::uptime();
        refreshFilesystems();
        return;
    }
    uint64_t elapsed = Clock::ticks() - lastTick;
    lastTick = Clock::ticks();

    count = Syscalls::listProcesses(list, MAX_ROWS);
    listedAt = Syscalls::uptimeMs();
    uptimeSeconds = Syscalls::uptime();
    for (unsigned i = 0; i < count; i++) {
        percentage[i] = 0;
        for (unsigned j = 0; j < previousCount && elapsed; j++) {
            if (previousPid[j] == list[i].pid) {
                percentage[i] = (unsigned)((list[i].ticks - previousTicks[j]) * 100 / elapsed);
            }
        }
    }
    for (unsigned i = 0; i < count; i++) {
        previousPid[i] = list[i].pid;
        previousTicks[i] = list[i].ticks;
    }
    previousCount = count;
}

void
Top::refreshFilesystems() {
    mountCount = Syscalls::mountList(mounts, MAX_MOUNTS);
    deviceCount = 0;
    int64_t dir = Syscalls::open("/dev", OpenFlag::RDONLY | OpenFlag::DIRECTORY);
    if (dir >= 0) {
        alignas(8) char buffer[512];
        int64_t used;
        while ((used = Syscalls::getdents((int)dir, buffer, sizeof(buffer))) > 0) {
            for (int64_t at = 0; at < used;) {
                const DirRecord *record = (const DirRecord *)(buffer + at);
                at += record->length;
                if (record->type != DirRecord::TYPE_BLOCK || deviceCount >= MAX_DEVICES) {
                    continue;
                }
                DeviceRow &d = devices[deviceCount];
                String::copy(d.name, record->name, sizeof(d.name));
                d.bytes = 0;
                d.sectorSize = 0;
                d.mountedOn[0] = 0;
                char path[32];
                Format::print(path, sizeof(path), "/dev/%s", d.name);
                int64_t fd = Syscalls::open(path, OpenFlag::RDONLY);
                if (fd >= 0) {
                    uint64_t bytes = 0;
                    int32_t sector = 0;
                    if (Syscalls::ioctl((int)fd, BlockIoctl::GET_SIZE_BYTES, &bytes) == 0) {
                        d.bytes = bytes;
                    }
                    if (Syscalls::ioctl((int)fd, BlockIoctl::GET_SECTOR_SIZE, &sector) == 0) {
                        d.sectorSize = (uint32_t)sector;
                    }
                    Syscalls::close((int)fd);
                }
                for (unsigned m = 0; m < mountCount; m++) {
                    if (String::equals(mounts[m].source, path)) {
                        String::copy(d.mountedOn, mounts[m].target, sizeof(d.mountedOn));
                    }
                }
                deviceCount++;
            }
        }
        Syscalls::close((int)dir);
    }
    if (selectedDevice >= deviceCount) {
        selectedDevice = deviceCount ? deviceCount - 1 : 0;
    }
}

void
Top::draw() {
    if (mode == Mode::Filesystems) {
        drawFilesystems();
    } else {
        drawProcesses();
    }
}

void
Top::drawFooter(const char *keys) {
    char line[VirtualConsole::MAX_COLS + 1];
    int rows = VirtualConsole::rows(console);
    VirtualConsole::fillRow(console, rows - 1, ' ', HEADER);
    Format::print(line, sizeof(line), " F1: processes  F2: filesystems  r: video mode  %s  Alt-F1..F12: consoles%s",
                  keys,
                  systemTop ? "" : "  ESC: exit");
    VirtualConsole::writeAt(console, 0, rows - 1, line, HEADER);
}

void
Top::drawProcesses() {
    char line[VirtualConsole::MAX_COLS + 1];
    unsigned running = 0;
    unsigned sleeping = 0;
    unsigned zombies = 0;
    for (unsigned i = 0; i < count; i++) {
        switch ((Process::State)list[i].state) {
        case Process::State::Running: case Process::State::Ready: running++; break;
        case Process::State::Sleeping: case Process::State::Blocked: sleeping++; break;
        default: zombies++; break;
        }
    }

    uint64_t s = uptimeSeconds;
    VirtualConsole::fillRow(console, 0, ' ', HEADER);
    Format::print(line, sizeof(line), " top - up %02u:%02u:%02u   CPUs: %u/%u online (%u cores x %u threads)   idle: %s",
                  (uint32_t)(s / 3600), (uint32_t)(s / 60 % 60), (uint32_t)(s % 60), Smp::onlineCount(),
                  Smp::logicalCount(), Smp::coreCount(), Smp::threadsPerCore(), Idle::name());
    VirtualConsole::writeAt(console, 0, 0, line, HEADER);

    VirtualConsole::fillRow(console, 1, ' ', NORMAL);
    Format::print(line, sizeof(line), " Tasks: %u total, %u running, %u sleeping, %u zombie   (this is console %u)",
                  count, running, sleeping, zombies, console);
    VirtualConsole::writeAt(console, 0, 1, line, NORMAL);
    VirtualConsole::fillRow(console, 2, ' ', NORMAL);

    VirtualConsole::fillRow(console, 3, ' ', COLUMNS);
    VirtualConsole::writeAt(console, 0, 3, "   PID  PPID NAME             STATE   CPU  CON  AFFINITY     TICKS  %CPU", COLUMNS);

    int rows = VirtualConsole::rows(console);
    int processRows = rows - 6;
    for (int r = 0; r < processRows; r++) {
        VirtualConsole::fillRow(console, 4 + r, ' ', NORMAL);
        if ((unsigned)r >= count) {
            continue;
        }
        const Syscalls::ProcessInfo &p = list[r];
        Process::State st = (Process::State)p.state;
        const char *state = st == Process::State::Running ? "RUN" : st == Process::State::Ready ? "READY"
                          : st == Process::State::Sleeping ? "SLEEP" : st == Process::State::Blocked ? "WAIT" : "ZOMBIE";
        char cpu[8];
        if (p.cpu < 0) {
            Format::print(cpu, sizeof(cpu), "-");
        } else {
            Format::print(cpu, sizeof(cpu), "%d", p.cpu);
        }
        Format::print(line, sizeof(line), "%6u %5u %-16s %-6s %3s  %3u  %08x %9u  %3u%%", p.pid, p.ppid, p.name,
                      state, cpu, (unsigned)p.console, (uint32_t)p.affinity, (uint32_t)p.ticks, percentage[r]);
        bool active = st == Process::State::Running || st == Process::State::Ready;
        VirtualConsole::writeAt(console, 0, 4 + r, line, active ? ACTIVE : (st == Process::State::Sleeping || st == Process::State::Blocked ? NORMAL : DIM));
    }

    VirtualConsole::fillRow(console, rows - 2, ' ', NORMAL);
    if (listedAt < statusUntil) {
        VirtualConsole::writeAt(console, 1, rows - 2, status, status[0] == 'C' ? WARNING : GOOD);
    }

    drawFooter("s: start");
    if (windowOpen) {
        drawWindow();
    }
}

void
Top::drawFilesystems() {
    char line[VirtualConsole::MAX_COLS + 1];
    uint64_t s = uptimeSeconds;
    VirtualConsole::fillRow(console, 0, ' ', HEADER);
    Format::print(line, sizeof(line), " top - up %02u:%02u:%02u   File systems: %u block device(s), %u mounted",
                  (uint32_t)(s / 3600), (uint32_t)(s / 60 % 60), (uint32_t)(s % 60), deviceCount, mountCount);
    VirtualConsole::writeAt(console, 0, 0, line, HEADER);
    int rows = VirtualConsole::rows(console);
    for (int y = 1; y < rows - 1; y++) {
        VirtualConsole::fillRow(console, y, ' ', NORMAL);
    }

    VirtualConsole::fillRow(console, 2, ' ', COLUMNS);
    VirtualConsole::writeAt(console, 0, 2, "  BLOCK DEVICE     SECTOR       SIZE (bytes)   KB  MOUNTED ON", COLUMNS);
    for (unsigned i = 0; i < deviceCount && i < 6; i++) {
        const DeviceRow &d = devices[i];
        Format::print(line, sizeof(line), "  /dev/%-11s %6u %18u %4u  %s", d.name, d.sectorSize, (uint32_t)d.bytes,
                      (uint32_t)(d.bytes / 1024), d.mountedOn[0] ? d.mountedOn : "-");
        VirtualConsole::writeAt(console, 0, 3 + (int)i, line, i == selectedDevice ? CHOSEN : ACTIVE);
    }

    VirtualConsole::fillRow(console, 10, ' ', COLUMNS);
    Format::print(line, sizeof(line), "  %-37s %-17s %s", "MOUNTED FILE SYSTEM", "TARGET", "TYPE");
    VirtualConsole::writeAt(console, 0, 10, line, COLUMNS);
    for (unsigned i = 0; i < mountCount && i < 3; i++) {
        Format::print(line, sizeof(line), "  %-37s %-17s %s", mounts[i].source, mounts[i].target, mounts[i].type);
        VirtualConsole::writeAt(console, 0, 11 + (int)i, line, NORMAL);
    }

    if (listedAt < statusUntil) {
        VirtualConsole::writeAt(console, 1, rows - 2, status, status[0] == 'C' ? WARNING : GOOD);
    }

    drawFooter("Up/Down: device");
}

void
Top::drawWindow() {
    unsigned choices = ProgramRegistry::menuCount();
    if (choices > MAX_CHOICES) {
        choices = MAX_CHOICES;
    }
    const int width = 44;
    const int height = (int)choices + 4;
    const int x0 = (VirtualConsole::columns(console) - width) / 2;
    const int y0 = (VirtualConsole::rows(console) - height) / 2;

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            bool top = y == 0;
            bool bottom = y == height - 1;
            bool left = x == 0;
            bool right = x == width - 1;
            char c = ' ';
            if (top && left) {
                c = TL;
            } else if (top && right) {
                c = TR;
            } else if (bottom && left) {
                c = BL;
            } else if (bottom && right) {
                c = BR;
            } else if (top || bottom) {
                c = HORIZONTAL;
            } else if (left || right) {
                c = VERTICAL;
            }
            VirtualConsole::putAt(console, x0 + x, y0 + y, c, WINDOW);
        }
    }
    VirtualConsole::writeAt(console, x0 + 2, y0, " Start a process ", WINDOW);
    for (unsigned i = 0; i < choices; i++) {
        char line[width];
        Format::print(line, sizeof(line), " %u  %-30s", i + 1, ProgramRegistry::menuAt(i)->name);
        VirtualConsole::writeAt(console, x0 + 2, y0 + 2 + (int)i, line, i == selected ? CHOSEN : WINDOW);
    }
    VirtualConsole::writeAt(console, x0 + 2, y0 + height - 2, "Enter/digit: start   ESC: cancel", WINDOW);
}

void
Top::startSelected() {
    const Program *program = ProgramRegistry::menuAt(selected);
    if (!program) {
        return;
    }
    ArgList args;
    int64_t result = Syscalls::spawn(program->name, args);
    uint64_t now = Syscalls::uptimeMs();
    if (result < 0) {
        Format::print(status, sizeof(status), "Cannot start %s: no free virtual console", program->name);
        statusUntil = now + 4000;
    } else {
        int64_t where = Syscalls::consoleOf((uint32_t)result);
        if (where >= 0) {
            Syscalls::switchConsole((unsigned)where);
        }
    }
    windowOpen = false;
}

void
Top::cycleTextMode() {
    static const VgaMode::Id modes[] = {
        VgaMode::Id::Text80x25,
        VgaMode::Id::FramebufferText,
        VgaMode::Id::Text80x40,
        VgaMode::Id::Text80x50,
        VgaMode::Id::Text80x43,
        VgaMode::Id::Text80x30,
        VgaMode::Id::Text80x60,
        VgaMode::Id::Text90x25,
        VgaMode::Id::Text90x30,
        VgaMode::Id::Text90x43,
        VgaMode::Id::Text90x50,
        VgaMode::Id::Text90x60,
    };
    VgaMode::Id current = VirtualConsole::mode(console);
    unsigned next = 0;
    for (unsigned i = 0; i < sizeof(modes) / sizeof(modes[0]); i++) {
        if (modes[i] == current) {
            next = (i + 1) % (sizeof(modes) / sizeof(modes[0]));
            break;
        }
    }
    if (!VirtualConsole::setTextMode(modes[next])) {
        Format::print(status, sizeof(status), "Video mode unavailable");
        kprintf("top: video mode %u unavailable\n", (uint32_t)modes[next]);
    } else {
        const VgaMode::Info &info = VgaMode::info(modes[next]);
        if (info.layout == VgaMode::Layout::FramebufferText) {
            Format::print(status, sizeof(status), "Framebuffer: %ux%u, %u columns x %u rows",
                          VgaMode::frameWidth(modes[next]), VgaMode::frameHeight(modes[next]), info.width, info.height);
            kprintf("top: switched to framebuffer text %ux%u, %u columns x %u rows\n",
                    VgaMode::frameWidth(modes[next]), VgaMode::frameHeight(modes[next]), info.width, info.height);
        } else {
            Format::print(status, sizeof(status), "Text mode: %u columns x %u rows", info.width, info.height);
            kprintf("top: switched to VGA text %u columns x %u rows\n", info.width, info.height);
        }
    }
    statusUntil = Syscalls::uptimeMs() + 4000;
}

bool
Top::handleKey(int key) {
    if (key == Keyboard::KEY_F1 || key == Keyboard::KEY_F1 + 1) {
        mode = key == Keyboard::KEY_F1 ? Mode::Processes : Mode::Filesystems;
        windowOpen = false;
        refresh();
        return true;
    }
    if (key == 'r' || key == 'R') {
        cycleTextMode();
        return true;
    }
    if (mode == Mode::Filesystems) {
        if (key == Keyboard::KEY_UP && selectedDevice > 0) {
            selectedDevice--;
        } else if (key == Keyboard::KEY_DOWN && selectedDevice + 1 < deviceCount) {
            selectedDevice++;
        } else if (key == Keyboard::KEY_ESC && !systemTop) {
            return false;
        }
        refresh();
        return true;
    }
    unsigned choices = ProgramRegistry::menuCount();
    if (choices > MAX_CHOICES) {
        choices = MAX_CHOICES;
    }
    if (windowOpen) {
        if (key == Keyboard::KEY_ESC) {
            windowOpen = false;
        } else if (key == Keyboard::KEY_UP && selected > 0) {
            selected--;
        } else if (key == Keyboard::KEY_DOWN && selected + 1 < choices) {
            selected++;
        } else if (key == Keyboard::KEY_ENTER) {
            startSelected();
        } else if (key >= '1' && key < '1' + (int)choices) {
            selected = (unsigned)(key - '1');
            startSelected();
        }
        return true;
    }
    if (key == 's' || key == 'S') {
        windowOpen = true;
        selected = 0;
    } else if (key == Keyboard::KEY_ESC && !systemTop) {
        return false;
    }
    return true;
}

Top::Top()
    : console(Syscalls::console()), pid(Syscalls::getpid()), systemTop(Syscalls::getppid() == 1), list(new Syscalls::ProcessInfo[MAX_ROWS]),
      count(0), previousCount(0), lastTick(0), listedAt(0), uptimeSeconds(0), mode(Mode::Processes), deviceCount(0),
      selectedDevice(0), mountCount(0), windowOpen(false), selected(0), statusUntil(0) {
    status[0] = 0;
}

Top::~Top() { delete[] list; }

int
Top::main(const ArgList &) {
    Top *top = new Top();
    int code = top->run();
    delete top;
    return code;
}

int
Top::run() {
    VirtualConsole::showCursor(console, false);
    uint64_t nextRefresh = 0;
    refresh();
    draw();
    for (;;) {
        bool redraw = false;
        int key;
        while ((key = Keyboard::read()) >= 0) {
            if (!handleKey(key)) {
                return 0;
            }
            redraw = true;
        }
        uint64_t now = Syscalls::uptimeMs();
        if (now >= nextRefresh) {
            refresh();
            redraw = true;
            nextRefresh = (nextRefresh == 0 || now >= nextRefresh + REFRESH_MS) ? now + REFRESH_MS : nextRefresh + REFRESH_MS;
        }
        if (redraw) {
            draw();
        }
        Syscalls::sleep(50);
    }
}
