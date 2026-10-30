#include "kernel/Program.h"
#include "programs/date/DateCommand.h"
#include "programs/uptime/UptimeCommand.h"
#include "programs/top/Top.h"
#include "programs/shell/Shell.h"
#include "programs/vga/VgaProgram.h"
#include "programs/vitralRasterizer/VitralRasterizerProgram.h"
#include "programs/vitralRaytracer/VitralRaytracerProgram.h"
#include "programs/ifconfig/IfconfigCommand.h"
#include "programs/httpd/Httpd.h"
#include "programs/init/Init.h"
#include "programs/dmesg/DmesgCommand.h"

static const Program programsMainFunctionsIndex[] __attribute__((section(".programs"), used)) = {
    {"date", &DateCommand::main, false},
    {"uptime", &UptimeCommand::main, false},
    {"top", &Top::main, false},
    {"shell", &Shell::main},
    {"vga", &VgaProgram::main},
    {"vitral_rasterizer", &VitralRasterizerProgram::main},
    {"vitral_raytracer", &VitralRaytracerProgram::main},
    {"ifconfig", &IfconfigCommand::main, false},
    {"httpd", &Httpd::main, false},
    {"init", &Init::main, false},
    {"dmesg", &DmesgCommand::main, false}
};
