@echo off
set PACKAGE_DIR="%CD%"

REM clean if requested
if "%1" == "clean" (
      call clean-build.bat
      if exist CMakeCache.txt del CMakeCache.txt
)

REM check for required programs on PATH
for %%F in (ant cmake) do (
      where /q %%F
      if ERRORLEVEL 1 (
            echo '%%F' not found on PATH; exiting
            exit /b 1
      )
)

setlocal

REM Remove system Rtools from PATH
call set PATH=%PATH:C:\Rtools\bin=%
call set PATH=%PATH:C:\Rtools\gcc-4.6.3\bin=%

REM Remove Git from PATH (otherwise cmake complains about 'sh.exe')
call set PATH=%PATH:C:\Program Files (x86)\Git\bin=%
call set PATH=%PATH:C:\Program Files\Git\bin=%

REM Establish build dir
set BUILD_DIR=build
if "%CMAKE_BUILD_TYPE%" == "" set CMAKE_BUILD_TYPE=RelWithDebInfo
if "%CMAKE_BUILD_TYPE%" == "Debug" set BUILD_DIR=build-debug

REM perform 64-bit build
cd "%PACKAGE_DIR%"
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
cd "%BUILD_DIR%"
if exist "%BUILD_DIR%/_CPack_Packages" rmdir /s /q "%BUILD_DIR%\_CPack_Packages"

REM Configure and build the project. (Note that Windows / MSVC builds require
REM that we specify the build type both at configure time and at build time)
cmake -G"Visual Studio 15 2017 Win64" ^
      -T host=x64 ^
      -DRSTUDIO_TARGET=Desktop ^
      -DCMAKE_BUILD_TYPE=%CMAKE_BUILD_TYPE% ^
      -DRSTUDIO_PACKAGE_BUILD=1 ^
      ..\..\.. || goto :error
cmake --build . --config %CMAKE_BUILD_TYPE% || goto :error
cd ..

REM perform 32-bit build and install it into the 64-bit tree
call make-install-win32.bat "%PACKAGE_DIR%\%BUILD_DIR%\src\cpp\session" %1 || goto :error

REM create packages
set VSINSTALLDIR="C:\Program Files (x86)\Microsoft Visual Studio\2017\BuildTools"
set VCINSTALLDIR="C:\Program Files (x86)\Microsoft Visual Studio\2017\BuildTools\VC"
cd "%BUILD_DIR%"
if not "%1" == "quick" cpack -C "%CMAKE_BUILD_TYPE%" -G NSIS
if "%CMAKE_BUILD_TYPE%" == "RelWithDebInfo" cpack -C "%CMAKE_BUILD_TYPE%" -G ZIP
cd ..

REM reset modified environment variables (PATH)
endlocal
goto :EOF

:error
echo Failed to build RStudio! Error: %ERRORLEVEL%
exit /b %ERRORLEVEL%

