cmake_minimum_required(VERSION 3.6)

project(PasswdSafe)

set(cxxflags "-std=c++14 -Os -fno-rtti -fno-exceptions")
set(cxxflags "${cxxflags} -W -Wall -Werror -Wno-unknown-pragmas")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${cxxflags}")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} ${cxxflags}")
add_library(PasswdSafe SHARED PasswdSafe.cpp sha256.cpp Util.cpp)
