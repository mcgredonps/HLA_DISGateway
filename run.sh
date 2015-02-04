#!/bin/bash

export mainJars=`ls lib/*.jar`
export RTI_HOME=`pwd`
export CLASSPATH=
for aJar in $mainJars; 
do
  export CLASSPATH=$aJar:$CLASSPATH
done

export CLASSPATH=build/classes:$CLASSPATH
echo $CLASSPATH

java -cp $CLASSPATH hlademo.HLADemo

