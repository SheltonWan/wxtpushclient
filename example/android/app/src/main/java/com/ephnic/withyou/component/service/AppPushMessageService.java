package com.ephnic.withyou.component.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

/**
 * 占位的推送消息 Service，用于满足三方 SDK 在清单中的组件要求，避免 ClassNotFound 崩溃。
 * 如需接收 OPPO/Heytap 透传或通知点击回调，请按官方文档实现具体逻辑。
 */
public class AppPushMessageService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // 非绑定型服务
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 仅作为占位，避免崩溃。后续根据需要处理收到的消息。
        return START_NOT_STICKY;
    }
}
