#!/bin/bash

SRC=$1
DEST=$2
ENV=$3

VERSION='0.0.1'
ARTIFACT_NAME="payment-service-$VERSION.jar"

echo $ARTIFACT_NAME

if [ ! -d "$DEST" ]; then
    mkdir -p $DEST/config
else
    echo "$DEST already exist ..."
    rm -rf $DEST && mkdir -p $DEST/config
fi

cp -r $SRC/deployment/* $DEST/
cp $SRC/deployment/config/application-$ENV.yaml $DEST/config/application.yaml
cp $SRC/target/$ARTIFACT_NAME $DEST/bin

