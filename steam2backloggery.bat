REM =====================
REM USER SETTINGS GO HERE
REM =====================

set STEAM_NAME=steamcommunity ID
set BL_NAME=backloggery username
set BL_PASS=backloggery password
set EDITOR=notepad

REM =========================================
REM NO USER SERVICEABLE PARTS BELOW THIS LINE
REM =========================================

REM Java on windows is a complete nightmare, and doesn't always properly get
REM added to %PATH%. So we add a bunch of plausible directories here.

SET PATH=%PATH%;"C:\Program Files\Java\jre7\bin;C:\Program Files\Java\jre6\bin"
SET PATH=%PATH%;"C:\Program Files (x86)\Java\jre7\bin;C:\Program Files (x86)\Java\jre6\bin"

java -jar bltool.jar --steam-name "%STEAM_NAME%" --bl-name "%BL_NAME%" --bl-pass "%BL_PASS%" --filter-from backloggery --from steam --to text --output games.txt
%EDITOR% games.txt
java -jar bltool.jar --bl-name "%BL_NAME%" --bl-pass "%BL_PASS%" --from text --to backloggery --input games.txt
