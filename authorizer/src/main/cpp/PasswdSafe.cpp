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

#include <vector>
#include <string>

JNIEXPORT jbyteArray JNICALL Java_org_pwsafe_lib_crypto_SHA256Pws_digestNNative
(
    JNIEnv* env,
    jclass,
    jbyteArray p,
    jint iter
)
{
    jsize plen = env->GetArrayLength(p);
    jbyte* pdata = env->GetByteArrayElements(p, nullptr);
    unsigned char output[SHA256::HASHLEN];

    SHA256 H0;
    H0.Update(reinterpret_cast<unsigned char*>(pdata), plen);
    H0.Final(output);

    for (jint i = 0; i < iter; ++i)
    {
        SHA256 H;
        H.Update(output, SHA256::HASHLEN);
        H.Final(output);
    }

    burnStack(sizeof(unsigned long) * 74);

    jbyteArray outputArray = env->NewByteArray(SHA256::HASHLEN);
    env->SetByteArrayRegion(outputArray, 0, SHA256::HASHLEN,
                            reinterpret_cast<jbyte*>(output));

    env->ReleaseByteArrayElements(p, pdata, 0);
    return outputArray;
}

