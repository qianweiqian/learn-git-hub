#include <fcntl.h>
#include <string.h>
#include <stdio.h>
#include <android/log.h>
#include <linux/ioctl.h>
#include <jni.h>
#include <JNIHelp.h>

#define LOG_TAG "boway_mtk_kpd"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define DEVICE_NAME "/sys/boway_tpd/gt9xx_tpd_switch"
int TOUCHSWITCH_ON	 = 1;
int TOUCHSWITCH_OFF  = 0;

static jint enableTouch(JNIEnv *env, jobject obj)
{
	FILE *fp = NULL;

    fp = fopen(DEVICE_NAME,"r+w");
    if(!fp) {
        LOGE("cannot open %s error!!!",DEVICE_NAME);
        return 1;
    }

	fprintf(fp, "%d,",TOUCHSWITCH_ON);

	fclose(fp);
    return 0;
}

static jint disenableTouch(JNIEnv *env, jobject obj)
{

	FILE *fp = NULL;
	int err = 0;
    
    fp = fopen(DEVICE_NAME,"r+w");
    if(!fp) {
        LOGE("cannot open %s error!!!",DEVICE_NAME);
        return 1;
    }

	err = fprintf(fp, "%d,",TOUCHSWITCH_OFF);

	fclose(fp);
    return 0;

}

static jint readCurrentStatus(JNIEnv *env, jobject obj)
{
	int cs = -1;
	int err = 0;
	FILE *fp = NULL;
	
    fp = fopen(DEVICE_NAME,"r+w");
    if(!fp) {
        LOGE("cannot open %s error!!!",DEVICE_NAME);
        return 1;
    }

	fscanf(fp,"%d",&cs);	

	fclose(fp);
    return cs;

}

static JNINativeMethod gNotify[] = {
    { "enableTouch",		"()I", (void*)enableTouch },
    { "disenableTouch",		"()I", (void*)disenableTouch },
	{ "readCurrentStatus",	"()I", (void*)readCurrentStatus },
};

int register_touchswitch(JNIEnv* env) {
    int res = jniRegisterNativeMethods(env, "com/android/incallui/SetTouchSwitch", gNotify, NELEM(gNotify));
    return res;
}

extern "C" jint JNI_OnLoad(JavaVM* vm, void* reserved) {
 
    JNIEnv* env = NULL;
    jint result = -1;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("GetEnv failed!");
        return result;
    }

  //  LOG_ASSERT(env, "Could not retrieve the env!");
    register_touchswitch(env);
    return JNI_VERSION_1_4;
}