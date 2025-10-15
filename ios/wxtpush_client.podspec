Pod::Spec.new do |s|
  s.name             = 'wxtpush_client'
  s.version          = '1.0.0'
  s.summary          = '多厂商原生推送客户端SDK'
  s.description      = <<-DESC
支持华为HMS、小米、OPPO、VIVO、荣耀、苹果APNs的多厂商原生推送客户端SDK
                       DESC
  s.homepage         = 'https://github.com/wxtpush/wxtpush_client'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'WxtPush' => 'support@wxtpush.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '9.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'
end
