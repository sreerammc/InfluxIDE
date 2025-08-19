#!/bin/bash

echo "========================================"
echo "InfluxDB Simple Table Query"
echo "========================================"
echo

echo "Building the application..."
mvn clean package

if [ $? -ne 0 ]; then
    echo
    echo "Build failed! Please check the error messages above."
    exit 1
fi

echo
echo "Build successful! Running the application..."
echo

java -jar target/influx-simple-1.0.0.jar

echo
echo "Application finished." 