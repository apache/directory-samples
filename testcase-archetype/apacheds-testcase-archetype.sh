#!/bin/sh

mvn archetype:generate -DarchetypeGroupId=org.apache.directory.server -DarchetypeArtifactId=apacheds-testcase-archetype -DarchetypeVersion=1.5.5-SNAPSHOT -DgroupId=$1 -DartifactId=$2 -Dversion=1.0-SNAPSHOT

