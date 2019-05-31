#!/bin/bash

SRC=$1
DEST=$2
ENV=$3

VERSION='0.0.1'
ARTIFACT_NAME="payment-service-$VERSION-SNAPSHOT.jar"

echo $ARTIFACT_NAME

if [ ! -d "$DEST" ]; then
    mkdir -p $DEST
else
    echo "$DEST already exist ..."
    rm -rf $DEST && mkdir -p $DEST
fi

cp -r $SRC/deployment/* $DEST/
cp $SRC/deployment/config/application-$ENV.yaml $DEST/config/application.yaml
cp $SRC/target/$ARTIFACT_NAME $DEST/bin

