# Freestanding toolchain for x86 (cross-compiling from macOS/Linux with LLVM).
set(CMAKE_SYSTEM_NAME Generic)
set(CMAKE_SYSTEM_PROCESSOR i386)

# Debian/Ubuntu install LLVM in versioned directories (/usr/lib/llvm-14/bin): newest version first.
file(GLOB LLVM_VERSIONED_BINS /usr/lib/llvm-*/bin)
list(SORT LLVM_VERSIONED_BINS COMPARE NATURAL ORDER DESCENDING)
set(LLVM_HINTS /opt/homebrew/opt/llvm/bin /usr/local/opt/llvm/bin /usr/lib/llvm/bin ${LLVM_VERSIONED_BINS})
find_program(BM_CLANG   clang   HINTS ${LLVM_HINTS} NO_DEFAULT_PATH)
find_program(BM_CLANGXX clang++ HINTS ${LLVM_HINTS} NO_DEFAULT_PATH)
find_program(BM_OBJCOPY llvm-objcopy HINTS ${LLVM_HINTS})
find_program(BM_LLD     ld.lld  HINTS /opt/homebrew/opt/lld/bin /usr/local/opt/lld/bin ${LLVM_HINTS})
if(NOT BM_CLANG OR NOT BM_LLD OR NOT BM_OBJCOPY)
  message(FATAL_ERROR "LLVM clang, ld.lld and llvm-objcopy are required (brew install llvm lld / apt install clang lld llvm)")
endif()

set(CMAKE_C_COMPILER   ${BM_CLANG})
set(CMAKE_CXX_COMPILER ${BM_CLANGXX})
set(CMAKE_ASM_COMPILER ${BM_CLANG})
set(CMAKE_C_COMPILER_TARGET   i386-none-elf)
set(CMAKE_CXX_COMPILER_TARGET i386-none-elf)
set(CMAKE_ASM_COMPILER_TARGET i386-none-elf)
set(CMAKE_TRY_COMPILE_TARGET_TYPE STATIC_LIBRARY)
set(CMAKE_OBJCOPY ${BM_OBJCOPY})
set(CMAKE_LINKER  ${BM_LLD})

# Value for -fuse-ld=. On a Linux host clang has no bare-metal driver for x86 *-none-elf and links through the host
# g++, which only accepts a linker name (it then runs ld.lld from the PATH), not a path.
if(CMAKE_HOST_SYSTEM_NAME STREQUAL "Linux")
  set(BM_FUSE_LD lld)
else()
  set(BM_FUSE_LD ${BM_LLD})
endif()
