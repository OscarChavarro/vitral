#include <dos.h>

#include "boot/Protected286Loader.h"
#include "boot/Protected286PhysicalRange.h"
extern unsigned _psp;
extern unsigned _version;

static void loaderTrace(unsigned row, const char far* text)
{
    volatile unsigned short far* video = (volatile unsigned short far*)MK_FP(0xB800, row * 160U);
    unsigned column = 0;
    while (*text && column < 80) video[column++] = 0x0F00U | (unsigned char)*text++;
    while (column < 80) video[column++] = 0x0720U;
}
/* Print through DOS while still in real mode so the message survives exit. */
static void loaderNotice(const char far* message)
{
    asm push ds;
    _DX = FP_OFF(message);
    _AX = FP_SEG(message);
    asm mov ds, ax;
    _AX = 0x0900;
    asm int 21h;
    asm pop ds;
}
static void loaderRangeTrace(Protected286Dword base, Protected286Dword bytes)
{
    char text[80];
    const char* prefix = "286 loader: RAM base=0x";
    unsigned n = 0, i;
    while (*prefix) text[n++] = *prefix++;
    for (i = 0; i < 8; ++i) text[n++] = "0123456789ABCDEF"[(unsigned)(base >> ((7 - i) * 4)) & 15];
    prefix = " bytes=0x";
    while (*prefix) text[n++] = *prefix++;
    for (i = 0; i < 8; ++i) text[n++] = "0123456789ABCDEF"[(unsigned)(bytes >> ((7 - i) * 4)) & 15];
    text[n] = 0;
    loaderTrace(9, text);
}

Protected286Dword protected286ImageBase = 0;
Protected286Dword protected286ImageBytes = 0;
Protected286Dword protected286CodeBase = 0;
Protected286Dword protected286DataBase = 0;
Protected286Dword protected286StackBase = 0;
Protected286Word protected286GatewaySegment = 0;
Protected286Dword protected286PoolStart = 0;
Protected286Dword protected286PoolLimit = 0;
Protected286Dword protected286InitOffset = 0;
Protected286Word protected286InitBytes = 0;
Protected286Dword protected286ExitOffset = 0;
Protected286Word protected286ExitBytes = 0;
char protected286CommandLine[128] = {0};
char protected286CurrentDirectory[68] = {0};
Protected286Word protected286DescriptorCount = 0;
Protected286Dword protected286DescriptorBases[PROTECTED_286_LOADER_MAX_DESCRIPTORS];
Protected286Byte protected286DescriptorAccess[PROTECTED_286_LOADER_MAX_DESCRIPTORS];

static Protected286Word codeSegments[248];
static unsigned codeSegmentCount = 0;

/* Real-mode loader only. Avoid pulling DOS's CL.LIB into the protected
 * executable merely to read its MZ header. All calls finish before LMSW. */
static int loaderOpen(const char far* path)
{
    unsigned result;
    asm push ds;
    _DX = FP_OFF(path);
    _AX = FP_SEG(path);
    asm mov ds, ax;
    _AX = 0x3D00;
    asm int 21h;
    asm jnc loaderOpenOk;
    _AX = 0xFFFF;
loaderOpenOk:
    asm pop ds;
    result = _AX;
    return (int)result;
}

static int loaderRead(int file, void far* buffer, unsigned bytes)
{
    asm push ds;
    _BX = file;
    _CX = bytes;
    _DX = FP_OFF(buffer);
    _AX = FP_SEG(buffer);
    asm mov ds, ax;
    _AX = 0x3F00;
    asm int 21h;
    asm jnc loaderReadOk;
    _AX = 0xFFFF;
loaderReadOk:
    asm pop ds;
    return (int)_AX;
}

static void loaderClose(int file)
{
    _BX = file;
    _AX = 0x3E00;
    asm int 21h;
}

static int loaderSeek(int file, Protected286Dword position)
{
    _BX = file;
    _CX = (unsigned)(position >> 16);
    _DX = (unsigned)position;
    _AX = 0x4200;
    asm int 21h;
    asm jnc loaderSeekOk;
    return 0;
loaderSeekOk:
    return 1;
}

static Protected286Dword conventionalPhysical(const void far* pointer)
{
    return ((Protected286Dword)FP_SEG(pointer) << 4) + FP_OFF(pointer);
}

/* AT BIOS 15h/87h copies physical memory before the permanent PE switch.
 * This is not XMS. The BIOS owns the temporary transition and return path. */
static int physicalCopy(Protected286Dword source, Protected286Dword destination,
                         Protected286Word bytes)
{
    unsigned char table[48];
    unsigned i;
    if (!bytes || (bytes & 1) || source > 0x01000000UL - bytes ||
        destination > 0x01000000UL - bytes) return 0;
    for (i = 0; i < sizeof(table); ++i) table[i] = 0;
    table[16] = table[17] = table[24] = table[25] = 0xFF;
    table[21] = table[29] = 0x93;
    for (i = 0; i < 3; ++i) {
        table[18 + i] = (unsigned char)(source >> (i * 8));
        table[26 + i] = (unsigned char)(destination >> (i * 8));
    }
    asm push es;
    asm push si;
    asm push di;
    asm push ds;
    _ES = FP_SEG(table);
    _SI = FP_OFF(table);
    _CX = bytes / 2;
    _AX = 0x8700;
    asm int 15h;
    asm pop ds;
    asm pop di;
    asm pop si;
    asm pop es;
    asm jc copyFailed;
    return (_AX & 0xFF00) == 0;
copyFailed:
    return 0;
}

static int protected286ImageRead(Protected286Dword offset, void far* destination,
                                 Protected286Word bytes)
{
    if (offset > protected286ImageBytes || bytes > protected286ImageBytes - offset) return 0;
    return physicalCopy(protected286ImageBase + offset, conventionalPhysical(destination), bytes);
}
static int protected286ImageWrite(Protected286Dword offset, const void far* source,
                                  Protected286Word bytes)
{
    if (offset > protected286ImageBytes || bytes > protected286ImageBytes - offset) return 0;
    return physicalCopy(conventionalPhysical(source), protected286ImageBase + offset, bytes);
}

#ifdef TURBO_C_286_EXTERNAL_PAYLOAD
static Protected286Dword payloadHeaderBytes, payloadFileBytes, payloadDataOffset;
static Protected286Word payloadStackSegment;
static unsigned char payloadTransfer[16384];
static const char far* protected286ExecutablePath(Protected286Word psp);

static int payloadHeader(Protected286Word psp)
{
    unsigned char h[28];
    int file = loaderOpen(protected286ExecutablePath(psp));
    if (file < 0) return 0;
    int good = loaderRead(file, h, sizeof(h)) == sizeof(h);
    loaderClose(file);
    if (!good || h[0] != 'M' || h[1] != 'Z') return 0;
    Protected286Word pages = h[4] | ((Protected286Word)h[5] << 8);
    Protected286Word last = h[2] | ((Protected286Word)h[3] << 8);
    if (!pages || last > 511) return 0;
    Protected286Dword length = (Protected286Dword)pages * 512UL;
    if (last) length -= 512U - last;
    payloadHeaderBytes = (Protected286Dword)(h[8] | ((Protected286Word)h[9] << 8)) * 16UL;
    if (length <= payloadHeaderBytes) return 0;
    payloadFileBytes = length - payloadHeaderBytes;
    Protected286Word extra = h[10] | ((Protected286Word)h[11] << 8);
    protected286ImageBytes = ((payloadFileBytes + 15UL) & ~15UL) + (Protected286Dword)extra * 16UL;
    payloadStackSegment = h[14] | ((Protected286Word)h[15] << 8);
    // The shared first object supplies CS=0 and a separate 32 KiB stack.
    if (h[22] || h[23] || h[16] || h[17] != 0x80) return 0;
    return protected286ImageBytes >= (Protected286Dword)payloadStackSegment * 16UL + 32768UL;
}

static int copyPayload(Protected286Word psp)
{
    Protected286Dword offset;
    unsigned i;
    for (i = 0; i < sizeof(payloadTransfer); ++i) payloadTransfer[i] = 0;
    // Clear the uninitialized data/stack as well as the on-disk image.
    for (offset = 0; offset < protected286ImageBytes;) {
        unsigned bytes = protected286ImageBytes - offset > sizeof(payloadTransfer) ?
            sizeof(payloadTransfer) : (unsigned)(protected286ImageBytes - offset);
        if (!protected286ImageWrite(offset, payloadTransfer, bytes)) return 0;
        offset += bytes;
    }
    int file = loaderOpen(protected286ExecutablePath(psp));
    if (file < 0) return 0;
    if (!loaderSeek(file, payloadHeaderBytes)) { loaderClose(file); return 0; }
    for (offset = 0; offset < payloadFileBytes;) {
        unsigned bytes = payloadFileBytes - offset > sizeof(payloadTransfer) ?
            sizeof(payloadTransfer) : (unsigned)(payloadFileBytes - offset);
        if (loaderRead(file, payloadTransfer, bytes) != bytes) { loaderClose(file); return 0; }
        if (bytes & 1) payloadTransfer[bytes] = 0;
        if (!protected286ImageWrite(offset, payloadTransfer, (unsigned)((bytes + 1U) & ~1U))) {
            loaderClose(file); return 0;
        }
        offset += bytes;
    }
    loaderClose(file);
    return 1;
}
#endif

static int copyImage(Protected286Word programSegment)
{
    Protected286Dword offset = 0;
    Protected286Dword source = (Protected286Dword)programSegment << 4;
    while (offset < protected286ImageBytes) {
        Protected286Word bytes = (Protected286Word)
            ((protected286ImageBytes - offset > 32768UL) ? 32768UL : protected286ImageBytes - offset);
        if (!physicalCopy(source + offset, protected286ImageBase + offset, bytes)) return 0;
        offset += bytes;
    }
    return 1;
}

static const char far* protected286ExecutablePath(Protected286Word psp)
{
#ifdef TURBO_C_286_EXTERNAL_PAYLOAD
    static char path[13];
    unsigned i = 0;
    const char* command = protected286CommandLine;
    (void)psp;
    while (*command == ' ' || *command == '\t') ++command;
    while (*command && *command != ' ' && *command != '\t') {
        char c = *command++;
        if (i == 8 || !((c >= 'A' && c <= 'Z') ||
            (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') ||
            c == '_' || c == '-')) return 0;
        path[i++] = c;
    }
    if (!i) return 0;
    path[i++] = '.'; path[i++] = 'B'; path[i++] = 'I'; path[i++] = 'N';
    path[i] = 0;
    return path;
#else
    Protected286Word environment = *(Protected286Word far*)MK_FP(psp, 0x2C);
    const char far* p;
    if (environment == 0) return 0;
    p = (const char far*)MK_FP(environment, 0);
    while (*p != 0 || *(p + 1) != 0) p++;
    return p + 4;
#endif
}

/* MZ describes segment fixups but carries no code/data attributes. TLINK's
 * detailed MAP supplies those attributes for large-model C++ modules. */
static int loaderHex(char c)
{
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    return -1;
}

static int mapToken(const char* line, unsigned position, const char* token)
{
    if (position && line[position - 1] != ' ') return 0;
    while (*token) if (line[position++] != *token++) return 0;
    return line[position] == 0 || line[position] == ' ' || line[position] == '\r';
}
static Protected286Dword mapHex(const char* line, unsigned* position)
{
    Protected286Dword value = 0;
    while (line[*position] == ' ') ++*position;
    while (loaderHex(line[*position]) >= 0)
        value = value * 16UL + loaderHex(line[(*position)++]);
    if (line[*position] == 'H') ++*position;
    return value;
}

#ifdef TURBO_C_286_EXTERNAL_PAYLOAD
struct PayloadSymbol {
    const char* name;
    void* value;
    unsigned bytes;
    Protected286Dword offset;
};
static PayloadSymbol payloadSymbols[] = {
    {"_protected286ImageBase", &protected286ImageBase, 4, 0},
    {"_protected286ImageBytes", &protected286ImageBytes, 4, 0},
    {"_protected286PoolStart", &protected286PoolStart, 4, 0},
    {"_protected286PoolLimit", &protected286PoolLimit, 4, 0},
    {"_protected286InitOffset", &protected286InitOffset, 4, 0},
    {"_protected286InitBytes", &protected286InitBytes, 2, 0},
    {"_protected286ExitOffset", &protected286ExitOffset, 4, 0},
    {"_protected286ExitBytes", &protected286ExitBytes, 2, 0},
    {"_protected286CommandLine", protected286CommandLine, 128, 0},
    {"_protected286CurrentDirectory", protected286CurrentDirectory, 68, 0},
    {"__psp", &_psp, 2, 0},
    {"__version", &_version, 2, 0}
};
#define PAYLOAD_SYMBOL_COUNT (sizeof(payloadSymbols) / sizeof(payloadSymbols[0]))
static void payloadMapLine(const char* line)
{
    unsigned p = 0;
    Protected286Dword first = mapHex(line, &p);
    if (line[p] == ':') {
        ++p;
        Protected286Dword offset = first * 16UL + mapHex(line, &p);
        while (line[p] == ' ') ++p;
        // TLINK /yx annotates unreferenced public symbols before their name.
        if (mapToken(line, p, "idle")) {
            p += 4;
            while (line[p] == ' ') ++p;
        }
        for (unsigned i = 0; i < PAYLOAD_SYMBOL_COUNT; ++i)
            if (mapToken(line, p, payloadSymbols[i].name)) payloadSymbols[i].offset = offset;
    }
    else {
        mapHex(line, &p); mapHex(line, &p);
        while (line[p] == ' ') ++p;
        // MAP public offsets use a paragraph-based DGROUP segment. _DATA
        // itself can start a few bytes into that paragraph after DOSSEG.
        if (mapToken(line, p, "_DATA")) payloadDataOffset = first & ~15UL;
    }
}
static int publishPayloadState()
{
    for (unsigned i = 0; i < PAYLOAD_SYMBOL_COUNT; ++i) {
        if (payloadSymbols[i].offset == 0xFFFFFFFFUL ||
            !protected286ImageWrite(payloadSymbols[i].offset,
                payloadSymbols[i].value, payloadSymbols[i].bytes)) return 0;
    }
    return 1;
}
#endif

static int loaderCodeMap(Protected286Word psp)
{
    char path[128];
    char line[160];
    const char far* source = protected286ExecutablePath(psp);
    unsigned n = 0, length = 0, i;
    int file;
    char ch;
    codeSegmentCount = 0;
#ifdef TURBO_C_286_EXTERNAL_PAYLOAD
    payloadDataOffset = 0xFFFFFFFFUL;
    for (unsigned symbol = 0; symbol < PAYLOAD_SYMBOL_COUNT; ++symbol)
        payloadSymbols[symbol].offset = 0xFFFFFFFFUL;
#endif
    if (!source) return 0;
    while (*source && n < sizeof(path) - 1) path[n++] = *source++;
    if (*source || n < 4 || path[n - 4] != '.') return 0;
    path[n - 3] = 'M'; path[n - 2] = 'A'; path[n - 1] = 'P'; path[n] = 0;
    file = loaderOpen(path);
    if (file < 0) return 0;
    while (loaderRead(file, &ch, 1) == 1) {
        if (ch != '\n') {
            if (length < sizeof(line) - 1) line[length++] = ch;
            continue;
        }
        line[length] = 0;
#ifdef TURBO_C_286_EXTERNAL_PAYLOAD
        payloadMapLine(line);
#endif
        for (i = 0; i + 4 <= length; ++i) {
            int kind = mapToken(line, i, "CODE") ? 1 :
                mapToken(line, i, "INITDATA") ? 2 : mapToken(line, i, "EXITDATA") ? 3 : 0;
            if (kind) {
                unsigned j = 0;
                Protected286Dword address = 0;
                while (line[j] == ' ') ++j;
                while (loaderHex(line[j]) >= 0)
                    address = address * 16UL + loaderHex(line[j++]);
                if (line[j] == ':') {
                    Protected286Dword offset = 0;
                    ++j;
                    while (loaderHex(line[j]) >= 0)
                        offset = offset * 16UL + loaderHex(line[j++]);
                    address = address * 16UL + offset;
                }
                if (kind != 1) {
                    if (line[j] == 'H') ++j;
                    mapHex(line, &j); /* inclusive stop */
                    Protected286Dword bytes = mapHex(line, &j);
                    if (bytes > 65535UL || bytes % 6UL) { loaderClose(file); return 0; }
                    if (kind == 2) { protected286InitOffset = address; protected286InitBytes = (Protected286Word)bytes; }
                    else { protected286ExitOffset = address; protected286ExitBytes = (Protected286Word)bytes; }
                    break;
                }
                if (address > 0xFFFF0UL || codeSegmentCount == 248) {
                    loaderClose(file); return 0;
                }
                codeSegments[codeSegmentCount++] = (Protected286Word)(address >> 4);
                break;
            }
        }
        length = 0;
    }
    loaderClose(file);
#ifdef TURBO_C_286_EXTERNAL_PAYLOAD
    if (payloadDataOffset == 0xFFFFFFFFUL) return 0;
#endif
    return codeSegmentCount != 0;
}

static Protected286Word protected286SelectorFor(Protected286Dword relative,
                                                Protected286Byte access)
{
    /* Keep the original segment base: MZ relocations contain segment words,
     * including immediate operands, not necessarily adjacent far pointers. */
    Protected286Dword block = protected286ImageBase + relative;
    Protected286Word i;
    for (i = 0; i < protected286DescriptorCount; i++) {
        if (protected286DescriptorBases[i] == block &&
            protected286DescriptorAccess[i] == access) {
            return (Protected286Word)(0x28 + i * 8);
        }
    }
    if (protected286DescriptorCount >= PROTECTED_286_LOADER_MAX_DESCRIPTORS) return 0;
    protected286DescriptorBases[protected286DescriptorCount] = block;
    protected286DescriptorAccess[protected286DescriptorCount] = access;
    i = protected286DescriptorCount++;
    return (Protected286Word)(0x28 + i * 8);
}

/* The MZ relocation entries are the authoritative inventory of words that
 * DOS changed from image-relative segments to real-mode segments. */
static int protected286PatchRelocations(Protected286Word psp,
                                        Protected286Word programSegment)
{
    const char far* path = protected286ExecutablePath(psp);
    unsigned char header[28];
    unsigned char relocation[4];
    unsigned char segmentWord[2];
    int file;
    Protected286Word count;
    Protected286Word table;
    Protected286Word i;

    if (path == 0) return 0;
    file = loaderOpen(path);
    if (file < 0 || loaderRead(file, header, sizeof(header)) != sizeof(header)) {
        if (file >= 0) loaderClose(file);
        return 0;
    }
    if (header[0] != 'M' || header[1] != 'Z') {
        loaderClose(file);
        return 0;
    }
    count = (Protected286Word)(header[6] | ((Protected286Word)header[7] << 8));
    table = (Protected286Word)(header[24] | ((Protected286Word)header[25] << 8));
    if (!loaderSeek(file, table)) {
        loaderClose(file);
        return 0;
    }
    for (i = 0; i < count; i++) {
        Protected286Word offset;
        Protected286Word sourceSegment;
        Protected286Word targetSegment;
        Protected286Word selector;
        Protected286Byte access;
        unsigned codeIndex;
        Protected286Dword target;
        if (loaderRead(file, relocation, 4) != 4) { loaderClose(file); return 0; }
        offset = (Protected286Word)(relocation[0] | ((Protected286Word)relocation[1] << 8));
        sourceSegment = (Protected286Word)(relocation[2] | ((Protected286Word)relocation[3] << 8));
        target = ((Protected286Dword)sourceSegment << 4) + offset;
        if (target + 2UL > protected286ImageBytes || !protected286ImageRead(target, segmentWord, 2)) {
            loaderClose(file); return 0;
        }
        targetSegment = (Protected286Word)(segmentWord[0] | ((Protected286Word)segmentWord[1] << 8));
        if (targetSegment < programSegment) { loaderClose(file); return 0; }
        target = (Protected286Dword)(targetSegment - programSegment) << 4;
        if (target >= protected286ImageBytes) { loaderClose(file); return 0; }
        /* A target in the active C code segment is executable.  Everything
         * else is a writable data selector: this covers far image buffers and
         * static objects, while vtables/function pointers still point at CS. */
        access = 0x92;
        for (codeIndex = 0; codeIndex < codeSegmentCount; ++codeIndex) {
            if (targetSegment - programSegment == codeSegments[codeIndex]) {
                access = 0x9A;
                break;
            }
        }
        selector = protected286SelectorFor(target, access);
        if (selector == 0) { loaderClose(file); return 0; }
        segmentWord[0] = (Protected286Byte)selector;
        segmentWord[1] = (Protected286Byte)(selector >> 8);
        /* Only rewrite the segment word named by the MZ relocation. */
        target = ((Protected286Dword)sourceSegment << 4) + offset;
        if (!protected286ImageWrite(target, segmentWord, 2)) { loaderClose(file); return 0; }
    }
    loaderClose(file);
    return 1;
}

extern "C" void
protected286DiscardImage()
{
    protected286ImageBase = protected286ImageBytes = 0;
    protected286PoolStart = protected286PoolLimit = 0;
    protected286DescriptorCount = 0;
}

/* Bare 286 takeover: BIOS reports contiguous RAM; the process owns it until
 * reset. Refuse a memory manager rather than overwrite its allocations. */
extern "C" int
protected286PrepareImage()
{
    Protected286Word psp, endSegment, programSegment, memoryKB;
    Protected286Dword limit;
    unsigned machineStatus, commandBytes, commandIndex;
    protected286DiscardImage();
    loaderTrace(2, "286 loader: [02] Checking exclusive bare-machine memory ownership.");
    /* DPMI installation check is a read-only real-mode multiplex call. */
    asm push es;
    asm push di;
    asm push si;
    _AX = 0x1687;
    asm int 2Fh;
    machineStatus = _AX;
    asm pop si;
    asm pop di;
    asm pop es;
    if (machineStatus == 0) {
        loaderNotice("\r\n286 loader: DPMI host detected. Restart DOS without the DPMI host.\r\n$");
        return 0;
    }
    asm db 0Fh, 01h, 0E0h; /* SMSW AX, available on an 80286. */
    machineStatus = _AX;
    if (machineStatus & 1) {
        loaderNotice("\r\n286 loader: Protected or virtual-8086 mode detected.\r\n"
                     "286 loader: Restart DOS without EMM386, QEMM or the active manager.\r\n$");
        return 0;
    }
    /* XMS identifies HIMEM and compatible managers without allocating memory. */
    _AX = 0x4300;
    asm int 2Fh;
    if ((_AX & 255) == 0x80) {
        loaderNotice("\r\n286 loader: HIMEM.SYS or another XMS manager detected.\r\n"
                     "286 loader: Restart DOS without HIMEM.SYS or the active XMS manager.\r\n$");
        return 0;
    }
    _AX = 0x8800;
    asm int 15h;
    asm jc noExtendedMemory;
    memoryKB = _AX;
    if (memoryKB <= 64) goto noExtendedMemory;
    loaderTrace(3, "286 loader: [03] BIOS RAM detected; reading PSP and segment map.");
    _AX = 0x6200;
    asm int 21h;
    psp = _BX;
    _psp = psp;
    commandBytes = *(unsigned char far*)MK_FP(psp, 0x80);
    if (commandBytes > 126) return 0;
    for (commandIndex = 0; commandIndex < commandBytes; ++commandIndex)
        protected286CommandLine[commandIndex] = *(char far*)MK_FP(psp, 0x81 + commandIndex);
    protected286CommandLine[commandBytes] = 0;
    /* Capture the caller's current drive and directory while DOS is available.
     * The protected ATA backend currently mounts the C: volume only. */
    _AX = 0x1900;
    asm int 21h;
    if ((_AX & 255) != 2) {
        loaderTrace(9, "286 loader: ERROR: Current drive must be C:.");
        return 0;
    }
    protected286CurrentDirectory[0] = 'C';
    protected286CurrentDirectory[1] = ':';
    protected286CurrentDirectory[2] = '\\';
    asm push ds;
    _SI = FP_OFF(protected286CurrentDirectory + 3);
    _AX = FP_SEG(protected286CurrentDirectory + 3);
    asm mov ds, ax;
    _DX = 0;
    _AX = 0x4700;
    asm int 21h;
    asm jnc cwdReady;
    asm pop ds;
    loaderTrace(9, "286 loader: ERROR: Cannot read current directory.");
    return 0;
cwdReady:
    asm pop ds;
    _AX = 0x3000;
    asm int 21h;
    _version = _AX;
    endSegment = *(Protected286Word far*)MK_FP(psp, 2);
    programSegment = (Protected286Word)(psp + 0x10);
    if (endSegment <= programSegment || !protected286ExecutablePath(psp)) {
        loaderTrace(9, "286 loader: ERROR: Expected program basename (1-8 characters).");
        return 0;
    }
    if (!loaderCodeMap(psp)) {
        loaderTrace(9, "286 loader: ERROR: Missing or invalid payload MAP.");
        return 0;
    }
#ifdef TURBO_C_286_EXTERNAL_PAYLOAD
    if (!payloadHeader(psp)) {
        loaderTrace(9, "286 loader: ERROR: Invalid/missing payload or stack ABI.");
        return 0;
    }
#else
    protected286ImageBytes = (Protected286Dword)(endSegment - programSegment) << 4;
#endif
    if (!protected286PlanPhysicalRange(memoryKB, protected286ImageBytes,
            &protected286ImageBase, &protected286PoolStart, &limit)) return 0;
    protected286PoolLimit = limit;
    loaderRangeTrace(protected286ImageBase, limit - protected286ImageBase);
#ifdef TURBO_C_286_EXTERNAL_PAYLOAD
    protected286CodeBase = protected286ImageBase;
    protected286DataBase = protected286ImageBase + payloadDataOffset;
    protected286StackBase = protected286ImageBase + (Protected286Dword)payloadStackSegment * 16UL;
#else
    protected286CodeBase = protected286ImageBase + ((Protected286Dword)(protected286GatewaySegment - programSegment) << 4);
    protected286DataBase = protected286ImageBase + ((Protected286Dword)(_DS - programSegment) << 4);
    protected286StackBase = protected286ImageBase + ((Protected286Dword)(_SS - programSegment) << 4);
#endif
    loaderTrace(4, "286 loader: [04] Physical range bounded by detected RAM and 16 MiB.");
    loaderTrace(5, "286 loader: [05] Copying image with AT BIOS physical block transfer.");
#ifdef TURBO_C_286_EXTERNAL_PAYLOAD
    if (!copyPayload(psp)) {
#else
    if (!copyImage(programSegment)) {
#endif
        loaderTrace(9, "286 loader: ERROR: BIOS physical image copy failed.");
        return 0;
    }
    loaderTrace(7, "286 loader: [07] Patching MZ relocations (no XMS calls).");
#ifdef TURBO_C_286_EXTERNAL_PAYLOAD
    if (!protected286PatchRelocations(psp, 0) || !publishPayloadState()) return 0;
#else
    if (!protected286PatchRelocations(psp, programSegment)) return 0;
#endif
    loaderTrace(8, "286 loader: [08] Physical image ready; returning to ASM gateway.");
    return 1;
noExtendedMemory:
    loaderTrace(9, "286 loader: ERROR: BIOS reported no usable extended memory.");
    return 0;
}
