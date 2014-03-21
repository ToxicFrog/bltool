#!/bin/bash

VERSION="$1"
DIR="bltool-$VERSION"

lein uberjar
cd target
mkdir "$DIR"
cp -v $(ls bltool-*-standalone.jar | tail -n1) "$DIR/bltool.jar"
cp -v ../steam2backloggery.* "$DIR"
cp -v ../README.md "$DIR/README.txt"
zip -r "bltool-$VERSION.zip" "$DIR"
rm -rf "$DIR"
