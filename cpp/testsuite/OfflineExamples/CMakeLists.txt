# IEEE 754 floating point everywhere: no fast-math (no reassociation, no
# finite-math or reciprocal shortcuts) and no fused multiply-add contraction,
# so results do not depend on how the compiler rewrites an expression
if(CMAKE_C_COMPILER_ID MATCHES "Clang|GNU" OR CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
    add_compile_options(-fno-fast-math -ffp-contract=off)
endif()

add_subdirectory(RaytracingOfflineExample)
add_subdirectory(Rasterizer2DExample)
add_subdirectory(ImageOfflineExample)
add_subdirectory(AlgebraicExpressionExample)
add_subdirectory(WireframeOfflineExample)
add_subdirectory(PolygonTriangulation)
