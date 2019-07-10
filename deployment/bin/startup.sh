#!/bin/bash

SCRIPT_PATH="`dirname \"$0\"`"

java -jar -Dserver.port=80 $SCRIPT_PATH/payment-service-0.0.1.jar \
    --spring.config.location="file:/usr/share/yb-payment-service/config/,classpath:/" &
