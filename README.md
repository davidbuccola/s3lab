# s3lab

A tool to help with Activity Platform development and testing.

## Building
Build with gradle using:
```bash
 ./gradlew build
 ```
The build can generate a distribution directory structure for executing the tool from the command line. This can be done 
with:
```bash
./gradlew installDist
```
The ```installDist``` target builds a distribution zip file and installs it (unzips it) at 
```.../build/install/s3lab/...```. Since this location is in the build output directories it is just a temporary
install. If you want to place it somewhere more permanent you can take the distribution zip and extract
it anywhere you like.

## Running
You probably want to place the distribution bin directory in your PATH for more convenient use
```
export PATH=$PATH:<projectroot>/build/install/s3lab/bin
```

To learn a little about the command line use:
```bash
s3lab --help
s3lab testPooling --help
```
Here's some sample commands:
```bash
s3lab testPooling --idle-duration=1 --busy-duration=1 --max-connections=200
```

# s3lab
