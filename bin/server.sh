#!/bin/bash
port=5000

prog=100
end_prog=100

while [ $prog -le $end_prog ]
do
	ver=1
	end_ver=2
	
	while [ $ver -le $end_ver ]
	do
		inst=1
		end_inst=2
		
		while [ $inst -le $end_inst ]
		do
			java TestServer $prog $ver $port &
			port=`expr $port + 2`
			inst=`expr $inst + 1`
		done
		
		ver=`expr $ver + 1`
	done
	
	prog=`expr $prog + 100`
done
