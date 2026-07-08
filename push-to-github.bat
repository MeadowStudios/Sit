@echo off

echo Checking Git status...
git status

echo.
set /p message=Enter commit message: 

if "%message%"=="" set message=Update plugin

echo.
echo Adding files...
git add .

echo.
echo Committing...
git commit -m "%message%"

echo.
echo Pushing to GitHub...
git push

echo.
echo Done.
pause