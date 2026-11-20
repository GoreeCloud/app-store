cmake_minimum_required(VERSION 3.21)
project(GoreeCloudAppStore VERSION 0.1.0 LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
find_package(Qt6 6.12.0 EXACT REQUIRED COMPONENTS Quick QuickControls2)
qt_standard_project_setup(REQUIRES 6.12)
qt_add_executable(goreecloud-app-store main.cpp)
qt_add_qml_module(goreecloud-app-store
    URI GoreeCloud.Store
    VERSION 1.0
    QML_FILES Main.qml
    RESOURCES catalog.json
)
target_link_libraries(goreecloud-app-store PRIVATE Qt6::Quick Qt6::QuickControls2)
install(TARGETS goreecloud-app-store RUNTIME DESTINATION bin)
