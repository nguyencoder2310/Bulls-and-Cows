@echo off
chcp 65001 >nul
echo Compiling...
javac -encoding UTF-8 NordTheme.java LoginPanel.java LobbyPanel.java RoomPanel.java GameClient.java
if %ERRORLEVEL% EQU 0 (
    echo Starting Client...
    java -Dfile.encoding=UTF-8 GameClient
) else (
    echo Compilation failed!
    pause
)
