#!/bin/bash

#######################
# USER SETTINGS GO HERE
#######################

STEAM_NAME="steamcommunity ID"
BL_NAME="backloggery username"
BL_PASS="backloggery password"
EDITOR="nano -w"

###########################################
# NO USER SERVICEABLE PARTS BELOW THIS LINE
###########################################

# Unlike windows we can usually assume that 'java' is in $PATH already and does the right thing

java -jar bltool.jar --steam-name "$STEAM_NAME" --bl-name "$BL_NAME" --bl-pass "$BL_PASS" --filter-from backloggery --from steam --to text --output games.txt
$EDITOR games.txt
java -jar bltool.jar --bl-name "$BL_NAME" --bl-pass "$BL_PASS" --from text --to backloggery --input games.txt
