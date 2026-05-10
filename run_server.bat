@echo off
chcp 65001 >nul
echo Compiling...
javac -encoding UTF-8 NordTheme.java LoginPanel.java LobbyPanel.java RoomPanel.java GameServer.java
if %ERRORLEVEL% EQU 0 (
    echo Starting Server...
    java -Dfile.encoding=UTF-8 GameServer
) else (
    echo Compilation failed!
    pause
)
