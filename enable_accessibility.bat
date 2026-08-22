@echo off
set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
set SERVICE=com.example.autoclicker/com.example.autoclicker.AutoClickerService

:: 获取当前已启用的服务列表
for /f "delims=" %%a in ('%ADB% shell settings get secure enabled_accessibility_services') do set CURRENT=%%a

:: 检查是否已包含
echo %CURRENT% | findstr /C:"%SERVICE%" >nul
if %errorlevel%==0 (
    echo 无障碍服务已处于开启状态
) else (
    :: 拼接并写入
    %ADB% shell "settings put secure enabled_accessibility_services '%SERVICE%:%CURRENT%'"
    %ADB% shell settings put secure accessibility_enabled 1
    echo 无障碍服务已开启
)
