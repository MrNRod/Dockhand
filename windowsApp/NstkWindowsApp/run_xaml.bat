@echo off
cd /d Z:\windowsApp\NstkWindowsApp
set XC="C:\WINDOWS\system32\config\systemprofile\.nuget\packages\microsoft.windowsappsdk\1.5.240311000\buildTransitive\..\tools\net6.0\..\net472\XamlCompiler.exe"
set IN=obj\arm64\Debug\net8.0-windows10.0.19041.0\input.json
set OUT=obj\arm64\Debug\net8.0-windows10.0.19041.0\output.json
%XC% %IN% %OUT% 1>Z:\windowsApp\NstkWindowsApp\xaml_stdout.txt 2>Z:\windowsApp\NstkWindowsApp\xaml_stderr.txt
echo EXITCODE=%errorlevel% > Z:\windowsApp\NstkWindowsApp\xaml_exitcode.txt
