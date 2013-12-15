#!/bin/bash

VERSION="$1"
DIR="bltool-$VERSION"

lein uberjar
cd target
mkdir "$DIR"
cp $(ls bltool-*-standalone.jar | tail -n1) "$DIR/bltool.jar"
cp ../steam2backloggery.bat "$DIR"
zip -r "bltool-$VERSION.zip" "$DIR"
rm -rf "$DIR"
