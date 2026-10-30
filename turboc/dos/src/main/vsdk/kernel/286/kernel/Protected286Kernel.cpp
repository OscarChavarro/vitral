#include "drivers/Protected286Keyboard.h"
#include "kernel/Protected286Runtime.h"
#include "svgalib/Protected286VgaText.h"
#include "boot/Protected286Loader.h"
#include "vitralLibC/Protected286Crt.h"
#include "vitralLibC/CRT.H"

extern "C" int raytracerMain(int, char**);
static char applicationName[] = "RAYTRACE.EXE";
static char* arguments[32];
static int prepareArguments()
{
    char* p = protected286CommandLine;
    int count = 1;
    arguments[0] = applicationName;
    while (*p) {
        while (*p == ' ' || *p == '\t') ++p;
        if (!*p) break;
        if (count == 31) return 0;
        char* output = p;
        arguments[count++] = output;
        int quoted = 0;
        while (*p && (quoted || (*p != ' ' && *p != '\t'))) {
            if (*p == '"') { quoted = !quoted; ++p; }
            else *output++ = *p++;
        }
        if (quoted) return 0;
        char* next = *p ? p + 1 : p;
        *output = 0;
        p = next;
    }
    arguments[count] = 0;
    return count;
}

extern "C" void protected286KernelMain()
{
    Protected286VgaText::writeAt(0, 13, "286 [13] C++ kernel entered.\r\n");
    Protected286Runtime::initialize();
    Protected286Crt::writeLine("286 [19] Running C++ static initializers.");
    int valid = crt286RunInitTable(protected286ImageBase + protected286InitOffset,
                                  protected286InitBytes, 0);
    int count = prepareArguments();
    if (valid && count) {
        Protected286Crt::writeLine("286 [20] Calling portable raytracer entry.");
        int status = raytracerMain(count, arguments);
        Protected286Crt::write("286 [21] Raytracer returned: ");
        Protected286Crt::writeUnsigned((unsigned long)status);
        Protected286Crt::writeLine("");
        crt286Finalize();
        crt286RunInitTable(protected286ImageBase + protected286ExitOffset,
                           protected286ExitBytes, 1);
    }
    else Protected286Crt::writeLine("286 ERROR: Invalid init table or command line.");
    Protected286VgaText::write("Press any key to reboot.\r\n");
    Protected286Keyboard::waitCharacter();
    Protected286Runtime::reboot267System();
}
