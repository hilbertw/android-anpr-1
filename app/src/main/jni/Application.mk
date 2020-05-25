# Build both ARMv5TE and ARMv7-A machine code.
APP_PLATFORM := android-9
APP_ABI := arm64-v8a armeabi-v7a
APP_STL := gnustl_static
#APP_STL := stlport_static
APP_MODULES := cxcore cv com_graphics_NativeGraphics cpufeatures
STLPORT_FORCE_REBUILD := true