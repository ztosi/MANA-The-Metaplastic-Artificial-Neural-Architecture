#!/bin/bash
# Compiles and runs a program to convert the TP file output by OSLOM to a .mat file
# Must have java
# Put "tp in the same directory as this script"

javac -cp ".:./jamtio.jar" TPtoCellArray.java

java -cp ".:./jamtio.jar" TPtoCellArray ./tp
