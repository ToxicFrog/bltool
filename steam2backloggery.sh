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

function bltool() {
  # Unlike windows we can usually assume that 'java' is in $PATH already and does the right thing
  java -jar bltool.jar --steam-name "$STEAM_NAME" --bl-name "$BL_NAME" --bl-pass "$BL_PASS" "$@"
}

# Initialize the filter file, if needed
[[ -f filter.txt ]] || {
  bltool --from backloggery --to text --output filter.txt
}

# Get initial games list from Steam.
bltool --from steam --to text --output games.txt --filter-from text --filter-input filter.txt
# Assume everything we've gotten will either be added or should be ignored forever.
cat games.txt >> filter.txt
$EDITOR games.txt
bltool --from text --to backloggery --input games.txt
