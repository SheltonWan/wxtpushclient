#!/bin/bash

# 华为推送快速诊断脚本

DEVICE_ID="66B0220328000554"

echo "====== 华为推送诊断工具 ======"
echo ""

echo "1. 清除日志缓存..."
adb -s $DEVICE_ID logcat -c

echo "2. 等待应用启动 (5秒)..."
sleep 5

echo "3. 捕获推送相关日志..."
echo ""
echo "====== 开始监听日志 ======"
adb -s $DEVICE_ID logcat | grep -E "(HuaweiPushService|WxtpushClient|HMS|Token|初始化|获取)" --line-buffered

