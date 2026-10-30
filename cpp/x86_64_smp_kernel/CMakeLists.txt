cmake_minimum_required(VERSION 3.20)
project(vgaboot LANGUAGES C CXX ASM)

# The same kernel, libraries and programs are built for either CPU mode. What differs lives in src/main/os/kernel/<arch>/,
# src/main/os/syscalls/<arch>/ and the boot code, and is chosen here: the rest of the sources do not depend on BM_ARCH.
set(BM_ARCH "x86_64" CACHE STRING "Kernel architecture: x86_64 or i386")
set_property(CACHE BM_ARCH PROPERTY STRINGS x86_64 i386)
if(BM_ARCH STREQUAL "x86_64")
  # Long mode kernel, started by stage 2 after building the PAE tables (or by a Multiboot2 loader).
  set(KERNEL_BITS 64)
  set(ARCH_FLAGS --target=x86_64-none-elf -mno-red-zone)
  set(ARCH_LIB_FLAGS -msse2)
  set(ARCH_LINK_FLAGS --target=x86_64-none-elf -Wl,-m,elf_x86_64)
  set(LINKER_SCRIPT_KERNEL ${CMAKE_SOURCE_DIR}/src/main/os/linker64.ld)
elseif(BM_ARCH STREQUAL "i386")
  # 32-bit protected mode kernel without paging, started by stage 2. The oldest CPU is a Pentium: the kernel uses
  # CPUID, RDTSC and CMPXCHG8B (64-bit atomics), and floating point is x87 (no SSE).
  set(KERNEL_BITS 32)
  set(ARCH_FLAGS --target=i386-none-elf -march=pentium)
  set(ARCH_LIB_FLAGS -mfpmath=387)
  set(ARCH_LINK_FLAGS --target=i386-none-elf -Wl,-m,elf_i386)
  set(LINKER_SCRIPT_KERNEL ${CMAKE_SOURCE_DIR}/src/main/os/boot/i386/linker32.ld)
else()
  message(FATAL_ERROR "BM_ARCH must be x86_64 or i386")
endif()
set(KERNEL_NAME kernel${KERNEL_BITS})

# Freestanding code, no libc nor libstdc++. The CPU mode flags (-m16/-m32/-m64) are per target or
# per source file: real mode is 16-bit, protected mode 32-bit, long mode 64-bit.
set(COMMON_FLAGS -ffreestanding -fno-pic -fno-stack-protector -fno-builtin -mno-sse -mno-mmx
                 -Os -Wall -Wextra)
add_compile_options(${COMMON_FLAGS})
add_compile_options($<$<COMPILE_LANGUAGE:CXX>:-fno-exceptions>
                    $<$<COMPILE_LANGUAGE:CXX>:-fno-rtti>
                    $<$<COMPILE_LANGUAGE:CXX>:-fno-threadsafe-statics>
                    $<$<COMPILE_LANGUAGE:CXX>:-nostdinc++>
                    $<$<COMPILE_LANGUAGE:CXX>:-std=c++17>)
include_directories(src/main src/main/os)

# Test only: force the real mode CPU check to report an 8086 or a 80286 (QEMU cannot emulate them).
set(BM_SIMULATE_CPU "" CACHE STRING "Simulate an old CPU in entry16.S: 8086 or 286")
if(BM_SIMULATE_CPU STREQUAL "8086")
  add_compile_definitions(SIMULATE_CPU=1)
elseif(BM_SIMULATE_CPU STREQUAL "286")
  add_compile_definitions(SIMULATE_CPU=2)
elseif(NOT BM_SIMULATE_CPU STREQUAL "")
  message(FATAL_ERROR "BM_SIMULATE_CPU must be empty, 8086 or 286")
endif()

# svgalib modes this kernel adds after svgalib's last one (2 colour SVGA modes and G1920x1080x16, numbers 147-152).
# Global, because the kernel and every program that includes <vga.h> must agree. The Linux build leaves them out.
option(SVGALIB_KERNEL_MODES "Number the kernel's own svgalib modes after __GLASTMODE" ON)
if(SVGALIB_KERNEL_MODES)
  add_compile_definitions(SVGALIB_KERNEL_MODES)
endif()

# Stage 2 of the x86_64 boot path checks the CPU and enters long mode from 32-bit protected mode: those sources are
# compiled for 32 bits (objs32) and linked into boot.elf. The i386 boot path jumps straight into the kernel.
set(SOURCES_32BIT src/main/os/kernel/Main32.cpp src/main/os/drivers/video/vga/VgaText.cpp src/main/lib/vitralKernel/Printf32.cpp
                  src/main/lib/vitralKernel/Cpu.cpp src/main/os/paging/Pae.cpp)
# The kernel and its built-in programs, for the CPU mode of BM_ARCH.
set(SOURCES_KERNEL src/main/os/kernel/KernelMain.cpp src/main/os/kernel/LocalApic.cpp src/main/os/kernel/Idle.cpp src/main/os/kernel/Smp.cpp
                  src/main/os/acpi/Acpi.cpp src/main/os/kernel/Interrupts.cpp src/main/os/kernel/Pic.cpp src/main/os/kernel/Clock.cpp
                  src/main/os/kernel/Panic.cpp src/main/os/kernel/Consoles.cpp src/main/os/drivers/video/virtualConsole/VirtualConsole.cpp
                  src/main/os/drivers/keyboard/Keyboard.cpp
                  src/main/os/drivers/char/CharDevice.cpp src/main/os/drivers/char/CharDevices.cpp src/main/os/drivers/hid/MouseDevice.cpp src/main/os/drivers/hid/psmouse/PsMouse.cpp
                  src/main/os/drivers/char/uart/Uart.cpp
                  src/main/os/kernel/ProgramRegistry.cpp src/main/os/kernel/ArgList.cpp src/main/os/kernel/Process.cpp
                  src/main/os/kernel/Scheduler.cpp src/main/os/kernel/Kernel.cpp src/main/os/kernel/InitScripts.cpp src/main/os/kernel/KernelLog.cpp
                  src/main/os/syscalls/Syscalls.cpp src/main/os/drivers/rtc/Rtc.cpp src/main/os/kernel/WallClock.cpp src/main/lib/vitralKernel/Calendar.cpp
                  src/main/programs/date/DateCommand.cpp src/main/programs/uptime/UptimeCommand.cpp
                  src/main/os/drivers/block/BlockDevice.cpp src/main/os/drivers/block/BlockDevices.cpp src/main/os/drivers/block/LoopDevice.cpp
                  src/main/os/drivers/block/loop_image.S src/main/os/fs/RootFs.cpp
                  src/main/os/fs/DevFs.cpp src/main/os/fs/Fat.cpp src/main/os/fs/Vfs.cpp
                  src/main/programs/top/Top.cpp src/main/programs/shell/Shell.cpp src/main/programs/vga/VgaProgram.cpp
                  src/main/programs/vitralRasterizer/VitralRasterizerProgram.cpp src/main/programs/vitralRaytracer/VitralRaytracerProgram.cpp
                  src/main/os/drivers/video/svgalib/Svgalib.cpp src/main/os/drivers/tty/Tty.cpp src/main/lib/readline/Readline.cpp
                  src/main/os/drivers/video/vga/VgaMode.cpp
                  src/main/os/net/Net.cpp src/main/os/net/NetDevice.cpp
                  src/main/os/net/NetDevices.cpp src/main/os/net/Ethernet.cpp
                  src/main/os/net/LoopbackDevice.cpp
                  src/main/os/net/PacketBuffer.cpp src/main/os/net/Checksum.cpp src/main/os/net/Ipv4Address.cpp
                  src/main/os/net/Arp.cpp src/main/os/net/Ipv4.cpp
                  src/main/os/net/Icmp.cpp src/main/os/net/Tcp.cpp
                  src/main/os/drivers/net/e1000/E1000.cpp src/main/os/drivers/net/rtl8169/Rtl8169.cpp src/main/os/drivers/net/tg3/Tg3.cpp src/main/programs/ifconfig/IfconfigCommand.cpp src/main/programs/httpd/Httpd.cpp
                  src/main/programs/httpd/WebSocket.cpp src/main/programs/httpd/Feeds.cpp
                  src/main/programs/init/Init.cpp src/main/programs/dmesg/DmesgCommand.cpp src/main/programs/ProgramsMainFunctionsIndex.cpp
                  src/main/os/drivers/pci/Pci.cpp src/main/os/drivers/video/svga/Svga.cpp src/main/os/drivers/video/bochsVBE/BochsVbe.cpp
                  src/main/os/drivers/video/bootFramebuffer/BootFramebuffer.cpp
                  src/main/os/kernel/PhysicalMemory.cpp src/main/os/kernel/KernelHeap.cpp src/main/lib/vitralKernel/Memory.cpp
                  src/main/lib/vitralKernel/Format.cpp
                  src/main/os/drivers/video/vga/VgaText.cpp src/main/lib/vitralKernel/Printf64.cpp src/main/lib/vitralKernel/Cpu.cpp)
# What depends on the CPU mode: entry code, interrupt stubs, GDT and IDT gates, FPU state, paging, the first frame of a
# process, the system call instruction and the start-up code of the other processors.
set(SOURCES_ARCH_x86_64 src/main/os/boot/entry64.S src/main/os/boot/multiboot2.S src/main/os/kernel/x86_64/Multiboot2.cpp
                        src/main/os/boot/ap_trampoline.S src/main/os/kernel/x86_64/interrupt_stubs.S src/main/os/kernel/x86_64/Gdt.cpp
                        src/main/os/kernel/x86_64/Fpu.cpp src/main/os/kernel/x86_64/Paging.cpp src/main/os/kernel/x86_64/ProcessFrame.cpp
                        src/main/os/kernel/x86_64/PanicRegisters.cpp src/main/os/syscalls/x86_64/SyscallsTrap.cpp)
set(SOURCES_ARCH_i386 src/main/os/boot/i386/entry32kernel.S src/main/os/boot/i386/ap_trampoline.S
                      src/main/os/kernel/i386/interrupt_stubs.S src/main/os/kernel/i386/Gdt.cpp src/main/os/kernel/i386/Fpu.cpp
                      src/main/os/kernel/i386/Paging.cpp src/main/os/kernel/i386/ProcessFrame.cpp
                      src/main/os/kernel/i386/PanicRegisters.cpp src/main/os/kernel/i386/LongDivision.cpp
                      src/main/os/syscalls/i386/SyscallsTrap.cpp)
if(BM_ARCH STREQUAL "x86_64")
  add_library(objs32 OBJECT ${SOURCES_32BIT})
  target_compile_options(objs32 PRIVATE -m32 -march=i386)
endif()
add_library(objsKernel OBJECT ${SOURCES_KERNEL} ${SOURCES_ARCH_${BM_ARCH}})
target_compile_options(objsKernel PRIVATE ${ARCH_FLAGS})

# ---- /dev/loop0: a VFAT image of loopKernelRootFileSystem, embedded in the kernel (loop_image.S) ----
# The same image can be mounted on Linux with `mount -o loop build/loop.img DIR`.
set(LOOP_ROOT_DIR "${CMAKE_SOURCE_DIR}/loopKernelRootFileSystem" CACHE PATH "Contents of the kernel's root file system")
# The front end of httpd (a single page application) is copied to /www in the image: httpd reads it with fopen().
set(HTTPD_FRONTEND_DIR "${CMAKE_SOURCE_DIR}/src/main/programs/httpd/frontend")
set(VITRAL_ETC_DIR "${CMAKE_SOURCE_DIR}/../../etc")
file(GLOB_RECURSE LOOP_ROOT_FILES CONFIGURE_DEPENDS "${LOOP_ROOT_DIR}/*" "${HTTPD_FRONTEND_DIR}/*")
file(GLOB_RECURSE VITRAL_ETC_FILES CONFIGURE_DEPENDS "${VITRAL_ETC_DIR}/*")
add_custom_command(OUTPUT loop.img
  COMMAND ${CMAKE_COMMAND} -E make_directory "${LOOP_ROOT_DIR}/etc"
  COMMAND ${CMAKE_COMMAND} -E copy_directory "${VITRAL_ETC_DIR}" "${LOOP_ROOT_DIR}/etc"
  COMMAND ${CMAKE_SOURCE_DIR}/scripts/makeFatImage.sh ${LOOP_ROOT_DIR} ${CMAKE_BINARY_DIR}/loop.img ${HTTPD_FRONTEND_DIR}:www
  DEPENDS ${CMAKE_SOURCE_DIR}/scripts/makeFatImage.sh ${LOOP_ROOT_FILES} ${VITRAL_ETC_FILES} VERBATIM)
add_custom_target(loopImage DEPENDS loop.img)
add_dependencies(objsKernel loopImage)
set_source_files_properties(src/main/os/drivers/block/loop_image.S PROPERTIES
  COMPILE_DEFINITIONS "LOOP_IMAGE_PATH=\"${CMAKE_BINARY_DIR}/loop.img\""
  OBJECT_DEPENDS ${CMAKE_BINARY_DIR}/loop.img)

# ---- vitralLibC and vitralStdC++, and the Vitral base library compiled from where it lives ----
set(VITRAL_BASE_DIR "${CMAKE_SOURCE_DIR}/../base" CACHE PATH "Vitral C++ base library")
set(LIB_FLAGS ${ARCH_FLAGS} ${ARCH_LIB_FLAGS} -mno-mmx -std=c++11 -fexceptions -frtti
              -fthreadsafe-statics -nostdinc++ -ffunction-sections -fdata-sections -fno-math-errno)
set(LIB_INCLUDES ${CMAKE_SOURCE_DIR}/src/main/lib/vitralStdC++/include ${CMAKE_SOURCE_DIR}/src/main/lib/vitralLibC/include)
file(GLOB SOURCES_LIBC CONFIGURE_DEPENDS src/main/lib/vitralLibC/src/*.cpp)
file(GLOB SOURCES_STDCXX CONFIGURE_DEPENDS src/main/lib/vitralStdC++/src/*.cpp)
add_library(objsLib OBJECT ${SOURCES_LIBC} ${SOURCES_STDCXX})
target_compile_options(objsLib PRIVATE ${LIB_FLAGS})
target_include_directories(objsLib SYSTEM PRIVATE ${LIB_INCLUDES})

# ---- code written against a standard C API (svgalib, GNU readline, libc): the readline compat library, svgalib's
# mouse (it reads /dev/input/mice through the libc), the mouse pointer library (svgalib + libc only), the video mode
# library (svgalib + readline only) and httpd's file reader. Compiled with the kernel flags, but with the vitralLibC
# headers and the compat headers that stand for <vga.h>, <vgamouse.h> and <readline/*.h>.
add_library(objsPortable OBJECT src/main/lib/readline/GnuReadline.cpp
            src/main/os/drivers/video/svgalib/SvgalibMouse.cpp src/main/lib/mouseCursor/MouseCursor.cpp
            src/main/lib/vgaModes/VgaModes.cpp src/main/lib/dithering/Dithering.cpp
            src/main/programs/httpd/StaticFile.cpp)
target_compile_options(objsPortable PRIVATE ${ARCH_FLAGS} -nostdinc++)
target_include_directories(objsPortable SYSTEM PRIVATE ${LIB_INCLUDES})
target_include_directories(objsPortable PRIVATE src/main/os/drivers/video/svgalib src/main/lib/readline/include)

# Files the portable programs read: the same constants are defined by the build that compiles them on Linux.
set(VGA_PALETTE_DIR "/media/ramdisk/etc/palettes" CACHE STRING "Directory of the vga program's palette files")
set(VITRAL_RAYTRACER_DEFAULT_SCENE "/media/ramdisk/etc/geometry/mitscenes/balls.ray" CACHE STRING
    "Scene vitral_raytracer shows when none is given")

# vitral_rasterizer: a svgalib client that also uses the Vitral base library, so it is compiled like the library
# (exceptions, RTTI, SSE2) and with the base's headers.
add_library(objsRasterizer OBJECT src/main/programs/vitralRasterizer/VitralRasterizer.cpp
            src/main/programs/vitralRasterizer/View.cpp
            src/main/programs/vitralRasterizer/RasterizerKeyboard.cpp
            src/main/programs/vitralRasterizer/RasterizerPalette.cpp
            src/main/programs/vitralRasterizer/RasterizerExamples.cpp
            src/main/programs/vitralRasterizer/RasterizerDisplay.cpp
            src/main/programs/vitralRasterizer/RasterizerHud.cpp
)
target_compile_options(objsRasterizer PRIVATE ${LIB_FLAGS})
target_compile_definitions(objsRasterizer PRIVATE VITRAL_WITH_POSIX_THREADS main=vitralRasterizerMain)
target_include_directories(objsRasterizer SYSTEM PRIVATE ${LIB_INCLUDES})
target_include_directories(objsRasterizer PRIVATE src/main/os/drivers/video/svgalib src/main/lib/readline/include
                           ${CMAKE_SOURCE_DIR}/src/main ${VITRAL_BASE_DIR}/src ${VITRAL_BASE_DIR}/src/main)

# vitral_raytracer: same as the rasterizer (svgalib client + Vitral base library, main renamed).
# Parallel rendering uses the base library's ParallelRaytracer.
add_library(objsRaytracer OBJECT src/main/programs/vitralRaytracer/VitralRaytracer.cpp
            src/main/programs/vitralRaytracer/Frame.cpp
            src/main/programs/vitralRaytracer/RaytracerDisplay.cpp
            src/main/programs/vitralRaytracer/RaytracerHud.cpp
            src/main/programs/vitralRaytracer/RaytracerKeyboard.cpp
            src/main/programs/vitralRaytracer/RaytracerPalette.cpp
            src/main/programs/vitralRaytracer/RaytracerRenderer.cpp
            src/main/programs/vitralRaytracer/SceneList.cpp
)
target_compile_options(objsRaytracer PRIVATE ${LIB_FLAGS})
set_source_files_properties(src/main/programs/vitralRaytracer/VitralRaytracer.cpp PROPERTIES COMPILE_DEFINITIONS main=vitralRaytracerMain)
target_compile_definitions(objsRaytracer PRIVATE VITRAL_WITH_POSIX_THREADS
                           VITRAL_RAYTRACER_KERNEL_LOG
                           "VITRAL_RAYTRACER_DEFAULT_SCENE=\"${VITRAL_RAYTRACER_DEFAULT_SCENE}\"")
target_include_directories(objsRaytracer SYSTEM PRIVATE ${LIB_INCLUDES})
target_include_directories(objsRaytracer PRIVATE src/main/os/drivers/video/svgalib src/main/lib/readline/include
                           ${CMAKE_SOURCE_DIR}/src/main ${VITRAL_BASE_DIR}/src ${VITRAL_BASE_DIR}/src/main)

# pthreadtest (tests/): a portable POSIX threads self-test. It is NOT part of the kernel image (it would show up in top and
# make the binary bigger): configure with -DBUILD_TESTS=ON to build it in.
option(BUILD_TESTS "Include the test programs (pthreadtest) in the kernel image" OFF)
set(TEST_OBJECTS "")
if(BUILD_TESTS)
  add_library(objsPthreadtest OBJECT tests/pthreadtest/pthreadtest.cpp)
  target_compile_options(objsPthreadtest PRIVATE ${LIB_FLAGS})
  target_compile_definitions(objsPthreadtest PRIVATE main=pthreadtestMain)
  target_include_directories(objsPthreadtest SYSTEM PRIVATE ${LIB_INCLUDES})
  target_sources(objsKernel PRIVATE tests/pthreadtest/pthreadtest_program.cpp)
  target_include_directories(objsKernel PRIVATE ${CMAKE_SOURCE_DIR})
  set(TEST_OBJECTS $<TARGET_OBJECTS:objsPthreadtest>)
endif()

# vga: a svgalib client that reads its palette files with the Vitral base library, so it is compiled like the
# library and the rasterizer; only its main is renamed.
add_library(objsVga OBJECT src/main/programs/vga/Vga.cpp
            src/main/programs/vga/Hud.cpp
            src/main/programs/vga/HudPatch.cpp
            src/main/programs/vga/Menu.cpp
            src/main/programs/vga/Mouse.cpp
            src/main/programs/vga/PaletteSet.cpp
            src/main/programs/vga/Picture.cpp
            src/main/programs/vga/VgaKeyboard.cpp
)
target_compile_options(objsVga PRIVATE ${LIB_FLAGS})
target_compile_definitions(objsVga PRIVATE VITRAL_WITH_POSIX_THREADS main=vgaMain
                           "VGA_PALETTE_DIR=\"${VGA_PALETTE_DIR}\"")
target_include_directories(objsVga SYSTEM PRIVATE ${LIB_INCLUDES})
target_include_directories(objsVga PRIVATE src/main/os/drivers/video/svgalib src/main/lib/readline/include
                           ${CMAKE_SOURCE_DIR}/src/main ${VITRAL_BASE_DIR}/src ${VITRAL_BASE_DIR}/src/main)

set(SOURCES_BASE "")
set(BASE_OBJECTS "")
if(EXISTS "${VITRAL_BASE_DIR}/src/main")
  # Every source of the base library, not the list of Vitral's own CMakeLists.txt, which may lag behind.
  file(GLOB_RECURSE SOURCES_BASE CONFIGURE_DEPENDS "${VITRAL_BASE_DIR}/src/main/*.cpp")
  # Left out: what Vitral itself does not build (jackson, the io/wrapper leftovers), what needs a library the kernel
  # does not have (FreeType) and java.util.regex (<regex> is not in vitralStdC++; nothing else in the base uses it).
  list(FILTER SOURCES_BASE EXCLUDE REGEX "/src/main/(jackson/|vsdk/toolkit/io/wrapper/(FileUncompressWrapper|Matrix2x2Printer|PipeInputStream|PipeOutputStream|Vector3DPrinter)\\.cpp|vsdk/toolkit/io/geometry/Freetype2FontReader\\.cpp|java/util/regex/)")
  add_library(objsBase OBJECT ${SOURCES_BASE})
  target_compile_options(objsBase PRIVATE ${LIB_FLAGS} -w)
  target_compile_definitions(objsBase PRIVATE VITRAL_WITH_POSIX_THREADS)
  target_include_directories(objsBase SYSTEM PRIVATE ${LIB_INCLUDES})
  target_include_directories(objsBase PRIVATE ${VITRAL_BASE_DIR}/src ${VITRAL_BASE_DIR}/src/main)
  set(BASE_OBJECTS $<TARGET_OBJECTS:objsBase>)
else()
  message(STATUS "Vitral base library not found in ${VITRAL_BASE_DIR}: not compiled")
endif()

# ---- The kernel (kernel64 or kernel32): flat image linked at 1 MB, appended to the disk after stage 2 ----
add_executable(${KERNEL_NAME}.elf $<TARGET_OBJECTS:objsKernel> $<TARGET_OBJECTS:objsLib> $<TARGET_OBJECTS:objsPortable> $<TARGET_OBJECTS:objsRasterizer> $<TARGET_OBJECTS:objsRaytracer> ${TEST_OBJECTS} $<TARGET_OBJECTS:objsVga> ${BASE_OBJECTS})
set_target_properties(${KERNEL_NAME}.elf PROPERTIES LINKER_LANGUAGE CXX)
target_link_options(${KERNEL_NAME}.elf PRIVATE
  ${ARCH_LINK_FLAGS} -fuse-ld=${BM_FUSE_LD} -nostdlib -static
  -Wl,-T,${LINKER_SCRIPT_KERNEL} -Wl,--gc-sections -Wl,--build-id=none -Wl,-Map,${CMAKE_BINARY_DIR}/${KERNEL_NAME}.map)
set_target_properties(${KERNEL_NAME}.elf PROPERTIES LINK_DEPENDS ${LINKER_SCRIPT_KERNEL})

# Links every object of the base library (no section garbage collection) to prove that nothing is
# unresolved. Not part of the image.
if(SOURCES_BASE)
  add_executable(base_check.elf EXCLUDE_FROM_ALL $<TARGET_OBJECTS:objsKernel> $<TARGET_OBJECTS:objsLib> $<TARGET_OBJECTS:objsPortable> $<TARGET_OBJECTS:objsRasterizer> $<TARGET_OBJECTS:objsRaytracer> ${TEST_OBJECTS} $<TARGET_OBJECTS:objsVga> ${BASE_OBJECTS})
  set_target_properties(base_check.elf PROPERTIES LINKER_LANGUAGE CXX LINK_DEPENDS ${LINKER_SCRIPT_KERNEL})
  target_link_options(base_check.elf PRIVATE
    ${ARCH_LINK_FLAGS} -fuse-ld=${BM_FUSE_LD} -nostdlib -static
    -Wl,-T,${LINKER_SCRIPT_KERNEL} -Wl,--build-id=none -Wl,--no-gc-sections -Wl,-Map,${CMAKE_BINARY_DIR}/base_check.map)
endif()

add_custom_command(OUTPUT ${KERNEL_NAME}.bin
  COMMAND ${CMAKE_OBJCOPY} -O binary $<TARGET_FILE:${KERNEL_NAME}.elf> ${KERNEL_NAME}.bin
  DEPENDS ${KERNEL_NAME}.elf VERBATIM)

# ---- boot.elf: real mode (16-bit) + protected mode (32-bit) ----
# x86_64: pm.S enters protected mode and Main32.cpp (objs32) checks the CPU and enters long mode (lm.S).
# i386: boot/i386/pm32kernel.S enters protected mode and jumps to the kernel.
set(LINKER_SCRIPT ${CMAKE_SOURCE_DIR}/src/main/os/linker.ld)
if(BM_ARCH STREQUAL "x86_64")
  set(SOURCES_BOOT_ARCH src/main/os/boot/pm.S src/main/os/boot/lm.S $<TARGET_OBJECTS:objs32>)
else()
  set(SOURCES_BOOT_ARCH src/main/os/boot/i386/pm32kernel.S)
endif()

add_executable(boot.elf
  src/main/os/boot/boot.S
  src/main/os/boot/entry16.S
  ${SOURCES_BOOT_ARCH}
  src/main/os/boot/unreal.S
  src/main/os/boot/A20.cpp
  src/main/os/boot/E820.cpp
  src/main/os/boot/BiosDisk.cpp
  src/main/os/boot/KernelLoader.cpp
  src/main/os/kernel/Main.cpp
  src/main/os/drivers/video/vga/Vga16.cpp
  src/main/lib/vitralKernel/Printf16.cpp)
target_compile_options(boot.elf PRIVATE -m16 -march=i386)
set_target_properties(boot.elf PROPERTIES LINKER_LANGUAGE CXX)
target_link_options(boot.elf PRIVATE
  -fuse-ld=${BM_FUSE_LD} -nostdlib -static -Wl,-m,elf_i386 -Wl,-T,${LINKER_SCRIPT}
  -Wl,--build-id=none -Wl,-Map,${CMAKE_BINARY_DIR}/boot.map)
set_target_properties(boot.elf PROPERTIES LINK_DEPENDS ${LINKER_SCRIPT})

add_custom_command(OUTPUT boot.bin
  COMMAND ${CMAKE_OBJCOPY} -O binary $<TARGET_FILE:boot.elf> boot.bin
  DEPENDS boot.elf VERBATIM)
add_custom_target(image ALL DEPENDS boot.bin ${KERNEL_NAME}.bin)
