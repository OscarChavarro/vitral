#include "fs/DevFs.h"
#include "drivers/block/BlockDevice.h"
#include "drivers/char/CharDevice.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"
#include "drivers/block/BlockDevices.h"
#include "drivers/char/CharDevices.h"
#include "fs/FileType.h"
#include "fs/PollEvent.h"
#include "fs/Inode.h"
#include "fs/DirEntry.h"

void
DevFs::describe(BlockDevice *device, unsigned index, Inode &out) {
    out = Inode();
    out.fs = this;
    out.mode = FileType::BLOCK_DEVICE;
    out.size = device->sizeBytes();
    out.ino = ROOT_INO + 1 + index;
    out.device = device;
}

void
DevFs::describe(CharDevice *device, unsigned index, Inode &out) {
    out = Inode();
    out.fs = this;
    out.mode = FileType::CHAR_DEVICE;
    out.ino = CHAR_INO + index;
    out.charDevice = device;
}

void
DevFs::describeDirectory(unsigned index, Inode &out) {
    out = Inode();
    out.fs = this;
    out.mode = FileType::DIRECTORY;
    out.ino = DIR_INO + index;
    out.aux0 = index;
}

bool
DevFs::sameDirectory(const char *a, const char *b) {
    if (!a || !b) {
        return a == b;
    }
    return String::equals(a, b);
}

int
DevFs::firstIn(const char *directory) {
    for (unsigned i = 0; i < CharDevices::count(); i++) {
        if (sameDirectory(CharDevices::at(i)->directory(), directory)) {
            return (int)i;
        }
    }
    return -1;
}

bool
DevFs::root(Inode &out) {
    out = Inode();
    out.fs = this;
    out.mode = FileType::DIRECTORY;
    out.ino = ROOT_INO;
    return true;
}

int64_t
DevFs::lookup(const Inode &dir, const char *name, Inode &out) {
    if (dir.mode != FileType::DIRECTORY) {
        return -Err::NOTDIR;
    }
    const char *inside = dir.ino == ROOT_INO ? nullptr : CharDevices::at((unsigned)dir.aux0)->directory();
    if (!inside) {
        for (unsigned i = 0; i < BlockDevices::count(); i++) {
            BlockDevice *device = BlockDevices::at(i);
            if (String::equals(device->name(), name)) {
                describe(device, i, out);
                return 0;
            }
        }
    }
    for (unsigned i = 0; i < CharDevices::count(); i++) {
        CharDevice *device = CharDevices::at(i);
        if (sameDirectory(device->directory(), inside) && String::equals(device->name(), name)) {
            describe(device, i, out);
            return 0;
        }
        if (!inside && device->directory() && String::equals(device->directory(), name) &&
            firstIn(device->directory()) == (int)i) {
            describeDirectory(i, out);
            return 0;
        }
    }
    return -Err::NOENT;
}

int64_t
DevFs::readDir(const Inode &dir, uint64_t &cursor, DirEntry &out) {
    if (dir.mode != FileType::DIRECTORY) {
        return -Err::NOTDIR;
    }
    const char *inside = dir.ino == ROOT_INO ? nullptr : CharDevices::at((unsigned)dir.aux0)->directory();
    unsigned blocks = inside ? 0 : BlockDevices::count();
    for (;;) {
        if (cursor < blocks) {
            BlockDevice *device = BlockDevices::at((unsigned)cursor);
            out.ino = ROOT_INO + 1 + cursor;
            out.mode = FileType::BLOCK_DEVICE;
            String::copy(out.name, device->name(), sizeof(out.name));
            cursor++;
            return 1;
        }
        unsigned index = (unsigned)(cursor - blocks);
        CharDevice *device = CharDevices::at(index);
        if (!device) {
            return 0;
        }
        cursor++;
        if (sameDirectory(device->directory(), inside)) {
            out.ino = CHAR_INO + index;
            out.mode = FileType::CHAR_DEVICE;
            String::copy(out.name, device->name(), sizeof(out.name));
            return 1;
        }
        if (!inside && firstIn(device->directory()) == (int)index) {
            out.ino = DIR_INO + index;
            out.mode = FileType::DIRECTORY;
            String::copy(out.name, device->directory(), sizeof(out.name));
            return 1;
        }
    }
}

int64_t
DevFs::read(Inode &file, uint64_t offset, void *buffer, size_t count) {
    if (file.charDevice) {
        return file.charDevice->read(buffer, count);
    }
    if (!file.device) {
        return -Err::INVAL;
    }
    return file.device->read(offset, buffer, count);
}

int64_t
DevFs::write(Inode &file, uint64_t offset, const void *buffer, size_t count) {
    if (file.charDevice) {
        return file.charDevice->write(buffer, count);
    }
    if (!file.device) {
        return -Err::INVAL;
    }
    return file.device->write(offset, buffer, count);
}

int64_t
DevFs::refresh(Inode &node) {
    if (node.device) {
        node.size = node.device->sizeBytes();
    }
    return 0;
}

int
DevFs::poll(Inode &file, int events) {
    if (!file.charDevice) {
        return Filesystem::poll(file, events);
    }
    int ready = events & PollEvent::OUT;
    if ((events & PollEvent::IN) && file.charDevice->readable()) {
        ready |= PollEvent::IN;
    }
    return ready;
}
