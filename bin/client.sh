#!/bin/bash

i=1
end=1000

while [ $i -le $end ]
do
	java Client 100 1 &
	java Client 100 2 &
	i=`expr $i + 1`
done
