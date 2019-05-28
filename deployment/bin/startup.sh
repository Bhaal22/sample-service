#!/bin/bash

java -jar -Dserver.port=80 -Dspring.profiles.active=SIT-BE /usr/share/yb-payment-service/bin/payment-service-0.0.1-SNAPSHOT.jar --spring.config.location=../config/ &
