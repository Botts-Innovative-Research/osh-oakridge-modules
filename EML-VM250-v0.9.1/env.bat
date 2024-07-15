@echo off

:: This bat file set up the ERNIE_HOME evironment variable as well
:: update PYTHONPATH and PATH with ERNIE_HOME path

:setupErnieHomePath
    :: ERNIE_HOME points to the parent directory of proj-ernie4
    pushd..
    set ERNIE_HOME=%CD%
    popd

:updatePythonPath
    set PYTHONPATH=%CD%\py;%PYTHONPATH%

:updatePath
    set PATH=%PATH%;%ERNIE_HOME%


