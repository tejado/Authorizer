#pragma clang diagnostic push
#pragma ide diagnostic ignored "readability-magic-numbers"
#pragma ide diagnostic ignored "cppcoreguidelines-avoid-magic-numbers"
/*
 * Copyright (©) 2009-2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */

#include <jni.h>

#include "org_pwsafe_lib_crypto_SHA256Pws.h"
#include "sha256.h"
#include "Util.h"

/**
 * Implementation of digestNNative so stack can be cleaned by caller
 * @param env JNI environment
 * @param p Input byte array
 * @param iter Number of iterations
 * @return The digested bytes
 */
[[gnu::noinline]] static jbyteArray digestNNativeImpl(JNIEnv* env,
                                                      jbyteArray p,
                                                      jint iter)
{
    jsize plen = env->GetArrayLength(p);
    jbyte *pdata = env->GetByteArrayElements(p, nullptr);
    std::array<unsigned char, SHA256::HASHLEN> output{};

    SHA256 H0;
    H0.update(reinterpret_cast<unsigned char *>(pdata), (size_t)plen);
    H0.final(output);

    for (jint i = 0; i < iter; ++i) {
        SHA256 H;
        H.update(output.data(), output.size());
        H.final(output);
    }

    jbyteArray outputArray = env->NewByteArray(output.size());
    env->SetByteArrayRegion(outputArray, 0, output.size(),
                            reinterpret_cast<jbyte *>(output.data()));

    env->ReleaseByteArrayElements(p, pdata, 0);
    return outputArray;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_org_pwsafe_lib_crypto_SHA256Pws_digestNNative(JNIEnv* env,
                                                   jclass,
                                                   jbyteArray p,
                                                   jint iter)
{
    jbyteArray outputArray = digestNNativeImpl(env, p, iter);
    burnStack(sizeof(unsigned long) * 74);
    return outputArray;
}


#pragma clang diagnostic pop
