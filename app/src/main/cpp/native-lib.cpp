#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_xyan_projects_imu_1tracking_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
