# Two shared libraries from sources vendored at pinned commits (see THIRD_PARTY);
# nothing is fetched at build time, keeping the F-Droid build reproducible.
cmake_minimum_required(VERSION 3.22.1)
project(notestakingts C CXX)

# No build-id note: it hashes the pre-strip output (build paths included), so it
# varies across build hosts and breaks F-Droid's reproducible-build verification.
# max-page-size=16384 keeps the libs loadable on 16 KB page-size devices.
set(NOTESTAKING_LINK_OPTIONS -Wl,--gc-sections -Wl,--build-id=none -Wl,-z,max-page-size=16384)

set(GRAMMAR_SOURCES
    grammars/asm/parser.c
    grammars/bash/parser.c
    grammars/bash/scanner.c
    grammars/c/parser.c
    grammars/cpp/parser.c
    grammars/cpp/scanner.c
    grammars/css/parser.c
    grammars/css/scanner.c
    grammars/diff/parser.c
    grammars/dockerfile/parser.c
    grammars/dockerfile/scanner.c
    grammars/go/parser.c
    grammars/html/parser.c
    grammars/html/scanner.c
    grammars/java/parser.c
    grammars/javascript/parser.c
    grammars/javascript/scanner.c
    grammars/json/parser.c
    grammars/kotlin/parser.c
    grammars/kotlin/scanner.c
    grammars/lua/parser.c
    grammars/lua/scanner.c
    grammars/php/parser.c
    grammars/php/scanner.c
    grammars/python/parser.c
    grammars/python/scanner.c
    grammars/rust/parser.c
    grammars/rust/scanner.c
    grammars/toml/parser.c
    grammars/toml/scanner.c
    grammars/typescript/parser.c
    grammars/typescript/scanner.c
    grammars/xml/parser.c
    grammars/xml/scanner.c
    grammars/yaml/parser.c
    grammars/yaml/scanner.c
)

# libnotestakingts: the tree-sitter runtime, the bundled grammars and a thin JNI bridge.
add_library(notestakingts SHARED
    tree-sitter/lib/src/lib.c
    ${GRAMMAR_SOURCES}
    ts_jni.c
)
set_target_properties(notestakingts PROPERTIES C_STANDARD 11)
# Applied to every build type (AGP release uses RelWithDebInfo, not Release).
target_compile_options(notestakingts PRIVATE
    -Os -fvisibility=hidden -ffunction-sections -fdata-sections)

target_include_directories(notestakingts PRIVATE
    tree-sitter/lib/include
    tree-sitter/lib/src
)

# Each grammar carries its own tree_sitter/parser.h; scope it to that grammar's
# sources so the copies never collide.
foreach(src ${GRAMMAR_SOURCES})
    get_filename_component(dir ${src} DIRECTORY)
    set_source_files_properties(${src} PROPERTIES
        INCLUDE_DIRECTORIES "${CMAKE_CURRENT_SOURCE_DIR}/${dir}")
endforeach()

target_link_options(notestakingts PRIVATE ${NOTESTAKING_LINK_OPTIONS})

# libnotestakingpdf: PDFium and its JNI bridge. pdfium.cmake (tools/pdfium-sync.sh)
# gives each PDFium library upstream's own flags, so none set here leak in.
set(PDFIUM_DIR ${CMAKE_CURRENT_SOURCE_DIR}/pdfium)
include(pdfium.cmake)

add_library(notestakingpdf SHARED pdf_jni.cpp)
target_include_directories(notestakingpdf PRIVATE ${PDFIUM_DIR})
target_compile_definitions(notestakingpdf PRIVATE NOTESTAKING_PDFIUM_REVISION="${PDFIUM_REVISION}")
target_compile_options(notestakingpdf PRIVATE -std=c++20 -fno-exceptions -fno-rtti -Os
    -fvisibility=hidden -ffunction-sections -fdata-sections
    "-ffile-prefix-map=${CMAKE_CURRENT_SOURCE_DIR}/=")
target_link_libraries(notestakingpdf PRIVATE ${PDFIUM_LIBRARIES} jnigraphics log)
# Only the JNI entry points are exported; FreeType would export its whole API.
set(NOTESTAKINGPDF_EXPORTS ${CMAKE_CURRENT_SOURCE_DIR}/pdf_jni.map)
target_link_options(notestakingpdf PRIVATE ${NOTESTAKING_LINK_OPTIONS}
    "-Wl,--version-script=${NOTESTAKINGPDF_EXPORTS}")
set_target_properties(notestakingpdf PROPERTIES LINK_DEPENDS ${NOTESTAKINGPDF_EXPORTS})
