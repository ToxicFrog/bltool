set STEAM_NAME=steam username
set BL_NAME=backloggery username
set BL_PASS=backloggery password
set EDITOR=notepad

java -jar bltool.jar --steam-name "%STEAM_NAME%" --bl-name "%BL_NAME%" --bl-pass "%BL_PASS%" --filter --from steam --to text --output games.txt
%EDITOR% games.txt
java -jar bltool.jar --bl-name "%BL_NAME%" --bl-pass "%BL_PASS%" --from text --to backloggery --input games.txt
