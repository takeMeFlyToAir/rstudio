@echo off

setlocal

REM Ensure (the default install location for) Git is on the PATH here. Having
REM this directory on the PATH by default can be a pain because Rtools (and its
REM competing tools) can compete with that directory.
set "PATH=C:\Program Files (x86)\Git\bin;%PATH%"

set PATH=%PATH%;%CD%\tools

REM call:install rsconnect master rstudio --no-build-vignettes
REM call:install rmarkdown master rstudio --no-build-vignettes
REM call:install renv master rstudio --no-build-vignettes

GOTO:EOF

:install

set PACKAGE=%1
set PACKAGE_VERSION=%2
set PACKAGE_GITHUB_ROOT=%3
set PACKAGE_BUILD_OPTIONS=%4

REM git clone if necessary
set PACKAGE_DIR="%PACKAGE%"
if not exist "%PACKAGE_DIR%" (
   if "%RSTUDIO_GITHUB_LOGIN%" == "" (
      git clone "https://github.com/%PACKAGE_GITHUB_ROOT%/%PACKAGE%.git"
   ) else (
      git clone "https://%RSTUDIO_GITHUB_LOGIN%@github.com/%PACKAGE_GITHUB_ROOT%/%PACKAGE%.git"
   )
)

REM clean and checkout target branch
pushd "%PACKAGE_DIR%"
git checkout .
git clean -df .
git pull
git checkout "%PACKAGE_VERSION%"


REM append GitHub fields to DESCRIPTION
git rev-parse "%PACKAGE_VERSION%" > PACKAGE_SHA1
set /p PACKAGE_SHA1= < PACKAGE_SHA1
del PACKAGE_SHA1
echo GithubRepo: %PACKAGE% >> DESCRIPTION
echo GithubUsername: rstudio >> DESCRIPTION
echo GithubRef: %PACKAGE_VERSION% >> DESCRIPTION
echo GithubSHA1: %PACKAGE_SHA1% >> DESCRIPTION
echo Origin: RStudioIDE >> DESCRIPTION

REM create source package
popd
set PACKAGE_ARCHIVE_PATTERN="%PACKAGE%*.tar.gz"
del /s /q %PACKAGE_ARCHIVE_PATTERN%
R CMD build "%PACKAGE_BUILD_OPTIONS%" "%PACKAGE%"

REM modify filename to include SHA1
for %%f in (%PACKAGE_ARCHIVE_PATTERN%) do set PACKAGE_ARCHIVE=%%f
set PACKAGE_ARCHIVE_STEM=%PACKAGE_ARCHIVE:~0,-7%
set PACKAGE_ARCHIVE_SHA1=%PACKAGE_ARCHIVE_STEM%_%PACKAGE_SHA1%.tar.gz
move %PACKAGE_ARCHIVE% %PACKAGE_ARCHIVE_SHA1%

GOTO:EOF



