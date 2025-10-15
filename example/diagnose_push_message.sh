#!/bin/bash

# 华为推送消息接收诊断脚本
# 使用方法: ./diagnose_push_message.sh

echo "🔍 华为推送消息接收诊断"
echo "======================================"
echo ""

# 获取设备ID
echo "📱 检测连接的设备..."
DEVICE_ID=$(adb devices | grep -w "device" | awk '{print $1}' | head -1)

if [ -z "$DEVICE_ID" ]; then
    echo "❌ 未检测到连接的设备"
    exit 1
fi

echo "✅ 设备已连接: $DEVICE_ID"
echo ""

# 检查应用是否在运行
echo "🔍 检查应用状态..."
APP_PACKAGE="com.ephnic.withyou"

# 尝试多种方式检测应用进程
APP_PROCESS=$(adb -s $DEVICE_ID shell "ps -A | grep $APP_PACKAGE" 2>/dev/null)
if [ -z "$APP_PROCESS" ]; then
    # 备用方法：使用 pidof
    APP_PID=$(adb -s $DEVICE_ID shell "pidof $APP_PACKAGE" 2>/dev/null)
    if [ -z "$APP_PID" ]; then
        # 最后尝试：检查最近启动的 activity
        RECENT_ACTIVITY=$(adb -s $DEVICE_ID shell "dumpsys activity activities | grep $APP_PACKAGE | head -1" 2>/dev/null)
        if [ -z "$RECENT_ACTIVITY" ]; then
            echo "⚠️  应用可能未运行（如果应用确实在运行，可以忽略此警告）"
        else
            echo "✅ 应用正在运行（通过 activity 检测到）"
        fi
    else
        echo "✅ 应用正在运行（PID: $APP_PID）"
    fi
else
    echo "✅ 应用正在运行"
fi
echo ""

# 检查 HMS Core
echo "🔍 检查 HMS Core..."
HMS_VERSION=$(adb -s $DEVICE_ID shell dumpsys package com.huawei.hwid | grep "versionName" | head -1 | awk -F= '{print $2}')
if [ -z "$HMS_VERSION" ]; then
    echo "❌ HMS Core 未安装或版本信息获取失败"
else
    echo "✅ HMS Core 版本: $HMS_VERSION"
fi
echo ""

# 检查 Service 注册
echo "🔍 检查 HuaweiMessageService 注册..."
SERVICE_CHECK=$(adb -s $DEVICE_ID shell dumpsys package $APP_PACKAGE | grep "HuaweiMessageService" | head -1)
if [ -z "$SERVICE_CHECK" ]; then
    echo "❌ HuaweiMessageService 未找到"
else
    echo "✅ HuaweiMessageService 已注册"
fi
echo ""

# 清除日志
echo "🧹 清除旧日志..."
adb -s $DEVICE_ID logcat -c
echo "✅ 日志已清除"
echo ""

echo "======================================"
echo "📡 开始监听推送消息..."
echo "======================================"
echo ""
echo "⚡ 现在请从后台发送测试推送消息"
echo "💡 提示：建议发送【数据消息】类型以确保应用可以接收"
echo ""
echo "🔍 监听以下关键日志："
echo "  - HuaweiMessageService"
echo "  - messageReceived"
echo "  - PushEventBus"
echo "  - Flutter 事件"
echo ""
echo "按 Ctrl+C 停止监听"
echo "======================================"
echo ""

# 实时监听推送相关日志
adb -s $DEVICE_ID logcat | grep --line-buffered -E "HuaweiMessage|messageReceived|PushEventBus|收到推送|flutter.*收到|flutter.*消息" | while read line; do
    # 高亮显示关键信息
    if echo "$line" | grep -q "收到华为推送消息"; then
        echo "🎯 $line"
    elif echo "$line" | grep -q "已分发到 Flutter"; then
        echo "✅ $line"
    elif echo "$line" | grep -q "收到推送事件"; then
        echo "📥 $line"
    elif echo "$line" | grep -q "收到消息"; then
        echo "📨 $line"
    elif echo "$line" | grep -q -i "error\|失败\|异常"; then
        echo "❌ $line"
    else
        echo "   $line"
    fi
done
