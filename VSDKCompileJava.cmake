# --------------------------------------------------------------------------
# TODO : document
# --------------------------------------------------------------------------

MACRO(VSDK_COMPILE_JAVA_APPLICATION APPLICATION CLASSPATH SOURCES)

  # Gather basic compilation values
  SET(src_path "${CMAKE_CURRENT_SOURCE_DIR}")
  SET(bin_path "${CMAKE_CURRENT_BINARY_DIR}")
  SET(app_name "${CMAKE_CURRENT_BINARY_DIR}/${APPLICATION}.class")
  SET(java_srcs "")
  SET(java_bins "")
  SET(comp_args "")
  FOREACH(src ${SOURCES})
    SET(full_path "${src_path}/${src}")
    IF(IS_DIRECTORY ${full_path})
      FILE(GLOB path_sources "${full_path}/*.java")
      FOREACH(file ${path_sources})
        SET(java_srcs "${java_srcs};${file}")
        GET_FILENAME_COMPONENT(fname ${file} NAME_WE)
        SET(java_bins "${java_bins};${bin_path}/${src}/${fname}.class")
      ENDFOREACH(file ${path_sources})
      SET(comp_args "${comp_args};${full_path}/*.java")
    ELSE(IS_DIRECTORY ${full_path})
      GET_FILENAME_COMPONENT(ext ${full_path} EXT)
      IF(${ext} MATCHES ".java")
        SET(java_srcs "${java_srcs};${full_path}")
        SET(comp_args "${comp_args};${full_path}")
      ELSE(${ext} MATCHES ".java")
        MESSAGE(FATAL_ERROR "${src} is not a java source file.")
      ENDIF(${ext} MATCHES ".java")
    ENDIF(IS_DIRECTORY ${full_path})
  ENDFOREACH(src)

  # Create compilation command
  IF(BUILD_JAVA_DEBUG)
    SET(java_debug_flags "-g")
  ELSE(BUILD_JAVA_DEBUG)
    SET(java_debug_flags "-g:none")
  ENDIF(BUILD_JAVA_DEBUG)

  ADD_CUSTOM_TARGET(${APPLICATION} ALL
    DEPENDS ${CLASSPATH} ${app_name})
  ADD_CUSTOM_COMMAND(
    OUTPUT          ${app_name}
    DEPENDS         ${java_srcs}
    COMMAND         ${CMAKE_Java_COMPILER}
    ARGS            -d ${CMAKE_CURRENT_BINARY_DIR}
                    -cp ${CLASSPATH}
                    ${java_debug_flags}
                    ${comp_args})

ENDMACRO(VSDK_COMPILE_JAVA_APPLICATION APPLICATION CLASSPATH SOURCES)

# --------------------------------------------------------------------------
# TODO : document
# --------------------------------------------------------------------------

MACRO(VSDK_COMPILE_JAVA_PACKAGE PACKAGE PATH)

  # Gather basic compilation values
  SET(src_path "${PATH}/${PACKAGE}")
  SET(bin_path "${CMAKE_CURRENT_BINARY_DIR}/${PACKAGE}")
  SET(java_srcs "")
  SET(java_bins "")
  SET(comp_args "")
  FOREACH(src ${ARGN})
    SET(full_path "${src_path}/${src}")
    IF(IS_DIRECTORY ${full_path})
      FILE(GLOB path_sources "${full_path}/*.java")
      FOREACH(file ${path_sources})
        SET(java_srcs "${java_srcs};${file}")
        GET_FILENAME_COMPONENT(fname ${file} NAME_WE)
        SET(java_bins "${java_bins};${bin_path}/${src}/${fname}.class")
      ENDFOREACH(file ${path_sources})
      SET(comp_args "${comp_args};${full_path}/*.java")
    ELSE(IS_DIRECTORY ${full_path})
      MESSAGE(FATAL_ERROR "TODO : adding file")
    ENDIF(IS_DIRECTORY ${full_path})
  ENDFOREACH(src)

  # Create compilation command
  IF(BUILD_JAVA_DEBUG)
    SET(java_debug_flags "-g")
  ELSE(BUILD_JAVA_DEBUG)
    SET(java_debug_flags "-g:none")
  ENDIF(BUILD_JAVA_DEBUG)
  SET(pck_jar "${bin_path}.jar")
  ADD_CUSTOM_TARGET(${PACKAGE} ALL
    DEPENDS ${pck_jar})
  ADD_CUSTOM_COMMAND(
    OUTPUT  ${pck_jar}
    DEPENDS ${java_srcs}
    COMMAND ${CMAKE_Java_COMPILER}
    ARGS -d ${CMAKE_CURRENT_BINARY_DIR} ${java_debug_flags} ${comp_args}
    COMMAND ${CMAKE_Java_ARCHIVE}
    ARGS cf ${pck_jar} ${PACKAGE})

  # Additional make rules
  SET_DIRECTORY_PROPERTIES(PROPERTIES
    ADDITIONAL_MAKE_CLEAN_FILES "${java_bins}")

ENDMACRO(VSDK_COMPILE_JAVA_PACKAGE PACKAGE PATH)

# eof - VSDKCompileJavaPackage.cmake
