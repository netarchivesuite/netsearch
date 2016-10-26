#!/bin/bash
java -Xmx10g -DArtikaPropertyFile=/home/summanet/arctika/arctika.properties -Dlog4j.configuration=file:/home/summanet/arctika/log4j.properties -jar netarchive-arctika-1.2.jar </dev/null >>nohup.out 2>&1 &
                                                                                                                                                                                                                                

