#!/bin/bash

echo "====== 华为推送快速诊断 ======"
DEVICE="66B0220328000554"

echo "1. 清除应用数据..."
adb -s $DEVICE shell pm clear com.ephnic.withyou

echo "2. 清除 logcat 缓存..."
adb -s $DEVICE logcat -c

echo "3. 启动日志监听（后台）..."
adb -s $DEVICE logcat | grep -E "(HuaweiPush|Token|初始化|HMS|WxtpushClient)" > /tmp/huawei_push_log.txt &
LOGCAT_PID=$!

echo "4. 重新运行应用..."
cd /Users/sheltonwan/Desktop/app/package/wxtpush/client_sdk/example
flutter run -d $DEVICE &
FLUTTER_PID=$!

echo "5. 等待 15 秒让应用启动..."
sleep 15

echo "6. 停止日志监听..."
kill $LOGCAT_PID 2>/dev/null

echo ""
echo "====== 诊断结果 ======"
echo ""
cat /tmp/huawei_push_log.txt

echo ""
echo "====== 关键信息检查 ======"
if grep -q "HMS Core可用" /tmp/huawei_push_log.txt; then
    echo "✅ HMS Core 可用"
else
    echo "❌ HMS Core 不可用或未初始化"
fi

if grep -q "Token获取成功" /tmp/huawei_push_log.txt; then
    echo "✅ Token 获取成功"
    grep "Token获取成功" /tmp/huawei_push_log.txt
else
    echo "❌ Token 获取失败"
    grep -E "(失败|error|Error|异常)" /tmp/huawei_push_log.txt || echo "未找到错误信息"
fi

echo ""
echo "完整日志已保存到: /tmp/huawei_push_log.txt"
echo "Flutter 进程 PID: $FLUTTER_PID"
echo ""
echo "按 Ctrl+C 结束 Flutter 应用"
wait $FLUTTER_PID
