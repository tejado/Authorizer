LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := PasswdSafe
LOCAL_SRC_FILES := PasswdSafe.cpp sha256.cpp Util.cpp

include $(BUILD_SHARED_LIBRARY)
