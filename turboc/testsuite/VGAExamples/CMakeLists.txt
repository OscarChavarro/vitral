option(VITRAL_BUILD_VGA_EXAMPLES "Build the native Linux svgalib examples" ON)

if(NOT VITRAL_BUILD_VGA_EXAMPLES)
    return()
endif()

if(NOT UNIX OR APPLE)
    message(STATUS "VGAExamples disabled: svgalib examples require Linux")
    return()
endif()

set(VITRAL_VGA_PORTABLE_SOURCE_DIR "" CACHE PATH
    "Path to the src directory containing lib/dithering, lib/mouseCursor and lib/vgaModes")

if(NOT EXISTS "${VITRAL_VGA_PORTABLE_SOURCE_DIR}/lib/dithering/Dithering.cpp"
   OR NOT EXISTS "${VITRAL_VGA_PORTABLE_SOURCE_DIR}/lib/mouseCursor/MouseCursor.cpp"
   OR NOT EXISTS "${VITRAL_VGA_PORTABLE_SOURCE_DIR}/lib/vgaModes/VgaModes.cpp")
    message(STATUS "VGAExamples disabled: set VITRAL_VGA_PORTABLE_SOURCE_DIR to the portable source tree")
    return()
endif()

find_path(SVGALIB_INCLUDE_DIR NAMES vga.h)
find_path(SVGALIB_MOUSE_INCLUDE_DIR NAMES vgamouse.h)
find_library(SVGALIB_LIBRARY NAMES vga)
find_path(READLINE_INCLUDE_DIR NAMES readline/readline.h)
find_library(READLINE_LIBRARY NAMES readline)

if(NOT SVGALIB_INCLUDE_DIR OR NOT SVGALIB_MOUSE_INCLUDE_DIR OR NOT SVGALIB_LIBRARY
   OR NOT READLINE_INCLUDE_DIR OR NOT READLINE_LIBRARY)
    message(STATUS "VGAExamples disabled: install the svgalib and readline development packages")
    return()
endif()

set(VITRAL_VGA_PALETTE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../etc/palettes" CACHE PATH
    "Directory containing the GPL palette files used by vitral_vga")
set(VITRAL_RAYTRACER_DEFAULT_SCENE "${CMAKE_CURRENT_LIST_DIR}/../../../etc/geometry/mitscenes/balls.ray" CACHE FILEPATH
    "Scene displayed by vitral_raytracer when no scene is supplied")

set(VITRAL_VGA_GENERATED_INCLUDE_DIR "${CMAKE_CURRENT_BINARY_DIR}/include")
file(MAKE_DIRECTORY "${VITRAL_VGA_GENERATED_INCLUDE_DIR}/programs")
foreach(VITRAL_VGA_PROGRAM vga vitralRasterizer vitralRaytracer)
    set(VITRAL_VGA_PROGRAM_LINK "${VITRAL_VGA_GENERATED_INCLUDE_DIR}/programs/${VITRAL_VGA_PROGRAM}")
    if(NOT EXISTS "${VITRAL_VGA_PROGRAM_LINK}")
        file(CREATE_LINK "${CMAKE_CURRENT_LIST_DIR}/${VITRAL_VGA_PROGRAM}" "${VITRAL_VGA_PROGRAM_LINK}" SYMBOLIC)
    endif()
endforeach()

add_library(vitral_vga_support STATIC
    "${VITRAL_VGA_PORTABLE_SOURCE_DIR}/lib/dithering/Dithering.cpp"
    "${VITRAL_VGA_PORTABLE_SOURCE_DIR}/lib/mouseCursor/MouseCursor.cpp"
    "${VITRAL_VGA_PORTABLE_SOURCE_DIR}/lib/vgaModes/VgaModes.cpp"
)
target_include_directories(vitral_vga_support PUBLIC
    "${VITRAL_VGA_PORTABLE_SOURCE_DIR}"
    "${VITRAL_VGA_GENERATED_INCLUDE_DIR}"
    "${SVGALIB_INCLUDE_DIR}"
    "${SVGALIB_MOUSE_INCLUDE_DIR}"
    "${READLINE_INCLUDE_DIR}"
)
target_link_libraries(vitral_vga_support PUBLIC "${SVGALIB_LIBRARY}" "${READLINE_LIBRARY}")
target_compile_features(vitral_vga_support PUBLIC cxx_std_11)

add_executable(vitral_vga
    vga/Hud.cpp
    vga/HudPatch.cpp
    vga/Menu.cpp
    vga/Mouse.cpp
    vga/PaletteSet.cpp
    vga/Picture.cpp
    vga/Vga.cpp
    vga/VgaKeyboard.cpp
)
target_compile_definitions(vitral_vga PRIVATE "VGA_PALETTE_DIR=\"${VITRAL_VGA_PALETTE_DIR}\"")
target_link_libraries(vitral_vga PRIVATE vitral_base vitral_vga_support)

add_executable(vitral_rasterizer
    vitralRasterizer/RasterizerDisplay.cpp
    vitralRasterizer/RasterizerExamples.cpp
    vitralRasterizer/RasterizerHud.cpp
    vitralRasterizer/RasterizerKeyboard.cpp
    vitralRasterizer/RasterizerPalette.cpp
    vitralRasterizer/View.cpp
    vitralRasterizer/VitralRasterizer.cpp
)
target_link_libraries(vitral_rasterizer PRIVATE vitral_base vitral_vga_support)

add_executable(vitral_raytracer
    vitralRaytracer/Frame.cpp
    vitralRaytracer/RaytracerDisplay.cpp
    vitralRaytracer/RaytracerHud.cpp
    vitralRaytracer/RaytracerKeyboard.cpp
    vitralRaytracer/RaytracerPalette.cpp
    vitralRaytracer/RaytracerRenderer.cpp
    vitralRaytracer/SceneList.cpp
    vitralRaytracer/ScreenProgress.cpp
    vitralRaytracer/VitralRaytracer.cpp
)
target_compile_definitions(vitral_raytracer PRIVATE
    "VITRAL_RAYTRACER_DEFAULT_SCENE=\"${VITRAL_RAYTRACER_DEFAULT_SCENE}\"")
target_link_libraries(vitral_raytracer PRIVATE vitral_base vitral_vga_support)
