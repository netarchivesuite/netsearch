#!/bin/bash
java -Xmx10g -DArtikaPropertyFile=/home/summanet/arctika/arctika.properties -Dlog4j.configuration=file:/home/summanet/arctika/log4j.properties -cp netarchive-arctika-1.0-jar-with-dependencies.jar dk.statsbiblioteket.netarchivesuite.arctika.builder.IndexBuilder </dev/null >>nohup.out 2>&1 &
