//
// Created by ielson on 03/07/20.
//
#include <android/bitmap.h>
#include <stdlib.h>

#define toInt(pValue) (0xff & (int32_t) pValue)
#define max(pValue1, pValue2) (pValue1 < pValue2) ? pValue2 : pValue1
#define clamp(pValue, pLowest, pHighest) ((pValue < 0) ? pLowest : (pValue > pHighest) ? pHighest : pValue)
#define color(pColorR, pColorG, pColorB) \
    (0xFF000000 | ((pColorB << 6) & 0x00FF0000) \
                | ((pColorG >> 2) & 0x0000FF00) \
                | ((pColorR >> 10)& 0x000000FF)
void JNICALL decode(JNIEnv * pEnv, jclass pClass, jobject pTarget, jbyteArray pSource, jint pFilter) {
AndroidBitmapInfo lBitmapInfo;
if (AndroidBitmap_getInfo(pEnv, pTarget, &lBitmapInfo) < 0) {
return;
}
if (lBitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
return;
}
uint32_t* lBitmapContent;
if (AndroidBitmap_lockPixels(pEnv, pTarget,
(void**)&lBitmapContent) < 0) {
return;
}
