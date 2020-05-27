#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint awslogin.podspec' to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'awslogin'
  s.version          = '0.0.1'
  s.summary          = 'An AWS Login Flutter plugin.'
  s.description      = <<-DESC
An AWS Login Flutter plugin.
                       DESC
  s.homepage         = 'http://1stmain.co'
  s.license          = { :file => '../LICENSE' }
  s.author           = { '1stMain' => 'sadak@1stmain.co' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.dependency 'AWSMobileClient', '~> 2.13.0'
  s.platform = :ios, '9.0'

  # Flutter.framework does not contain a i386 slice. Only x86_64 simulators are supported.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'VALID_ARCHS[sdk=iphonesimulator*]' => 'x86_64' }
  s.swift_version = '5.0'
end
