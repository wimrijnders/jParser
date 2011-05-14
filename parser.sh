#!/bin/bash
java -classpath "dist/lib/appointments.jar:dist/lib/parser.jar:lib/log4j-1.2.16.jar:build/generated" nl.axizo.appointment.AppointmentsMain $1 $2

