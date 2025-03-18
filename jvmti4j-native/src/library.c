//
// Created by ManTou on 2025/3/18.
//

#include "me_mantou_jvmti4j_JVMTIScheduler.h"
#include <jvmti.h>

jvmtiEnv *jvmti = NULL;

void JNICALL classLoadHook(jvmtiEnv *jvmti_env,
                           JNIEnv *jni_env,
                           jclass class_being_redefined,
                           jobject loader,
                           const char *name,
                           jobject protection_domain,
                           jint class_data_len,
                           const unsigned char *class_data,
                           jint *new_class_data_len,
                           unsigned char **new_class_data);

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    if ((*jvm)->GetEnv(jvm, (void **) &jvmti, JVMTI_VERSION_1_0) != JNI_OK) {
        return JNI_ERR;
    }

    jvmtiCapabilities capabilities;
    (*jvmti)->GetPotentialCapabilities(jvmti, &capabilities);
    capabilities.can_retransform_classes = 1;
    capabilities.can_redefine_classes = 1;
    jvmtiError err = (*jvmti)->AddCapabilities(jvmti, &capabilities);
    if (err != JVMTI_ERROR_NONE) {
        return JNI_ERR;
    }

    jvmtiEventCallbacks callbacks;
    callbacks.ClassFileLoadHook = &classLoadHook;
    err = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, sizeof(callbacks));
    if (err != JVMTI_ERROR_NONE) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}

JNIEXPORT jint JNICALL Java_me_mantou_jvmti4j_JVMTIScheduler_retransformClass0
(JNIEnv *env, jclass jclazz, jclass target) {
    jclass classes[] = {target};
    (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
    return (*jvmti)->RetransformClasses(jvmti, 1, classes);
}

JNIEXPORT jobjectArray JNICALL Java_me_mantou_jvmti4j_JVMTIScheduler_getLoadedClasses
(JNIEnv *env, jclass jclazz) {
    jint count;
    jclass *classes;
    (*jvmti)->GetLoadedClasses(jvmti, &count, &classes);

    jobjectArray result = (*env)->NewObjectArray(
        env,
        count,
        (*env)->FindClass(env, "java/lang/Class"),
        NULL
    );

    for (int i = 0; i < count; i++) {
        (*env)->SetObjectArrayElement(env, result, i, classes[i]);
    }

    (*jvmti)->Deallocate(jvmti, (unsigned char *) classes);
    return result;
}

void JNICALL classLoadHook(jvmtiEnv *jvmti_env,
                           JNIEnv *jni_env,
                           jclass class_being_redefined,
                           jobject loader,
                           const char *name,
                           jobject protection_domain,
                           jint class_data_len,
                           const unsigned char *class_data,
                           jint *new_class_data_len,
                           unsigned char **new_class_data) {
    (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_DISABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);

    // 第一次定义class的时候为NULL, name为内部名, 暂时return不考虑使用name
    if (class_being_redefined == NULL) return;

    jclass schedulerClazz = (*jni_env)->FindClass(jni_env, "me/mantou/jvmti4j/JVMTIScheduler");
    jobject loadHookObj = (*jni_env)->CallStaticObjectMethod(
        jni_env,
        schedulerClazz,
        (*jni_env)->GetStaticMethodID(
            jni_env,
            schedulerClazz,
            "getLoadHook",
            "()Lme/mantou/jvmti4j/JVMTIScheduler$LoadHook;"
        )
    );

    if (loadHookObj == NULL) return;

    jbyteArray originalData = (*jni_env)->NewByteArray(jni_env, class_data_len);
    (*jni_env)->SetByteArrayRegion(jni_env, originalData, 0, class_data_len, (const jbyte *) class_data);

    jbyteArray newData = (*jni_env)->CallObjectMethod(
        jni_env,
        loadHookObj,
        (*jni_env)->GetMethodID(
            jni_env,
            (*jni_env)->GetObjectClass(jni_env, loadHookObj),
            "invoke",
            "(Ljava/lang/Class;[B)[B"
        ),
        class_being_redefined,
        originalData
    );

    if (newData == NULL) return;

    *new_class_data_len = (*jni_env)->GetArrayLength(jni_env, newData);
    (*jvmti_env)->Allocate(jvmti_env, *new_class_data_len, new_class_data);
    (*jni_env)->GetByteArrayRegion(jni_env, newData, 0, *new_class_data_len, (jbyte *) *new_class_data);
}
