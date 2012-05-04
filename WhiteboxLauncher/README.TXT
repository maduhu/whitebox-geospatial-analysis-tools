Welcome to Whitebox GIS!

Whitebox has been developed using Java and therefore requires the Java Runtime Environment (JRE) to be installed on the users machine.

To launch Whitebox double-click on the WhiteboxLauncher.jar file. This is the equivalent of a .exe file and should automatically run once double-clicked if the JRE is properly installed.

By default, Whitebox will only see 1GB of RAM. If your system has more available RAM, you may want to open the LaunchParameters.txt file and modify the line:

max heap size: 1Gb

to read something like:

max heap size: 3Gb

Whitebox can also be launched from the command-line by changing the working directory to the Whitebox directory (within the terminal application) and typing:

java -jar WhiteboxLauncher.jar 

Because Whitebox does not need an installation program, it will happily run off of a USB pen drive.