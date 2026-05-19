@echo off
jar cfe Gestion-Lot-NOZ.jar app.Controleur -C bin .
java -cp "Gestion-Lot-NOZ.jar;app\jar\poi-bin-5.2.3\*;app\jar\poi-bin-5.2.3\lib\*;app\jar\poi-bin-5.2.3\ooxml-lib\*" app.Controleur  
pause