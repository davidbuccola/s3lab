# s3lab

A tool to help with Activity Platform development and testing.

## Prerequisites.
To use this tool the target org needs certain things configured. The configuration is summarized in
 [Configuring my EAC/HVS Test Org](https://docs.google.com/document/d/1-bMCsu86yoN9xfcTW5H15vC7KwJiAgGDXP0eWDSGMQs/view).
The configuration includes licenses and org perms and preferences to support certain data generation features. There's a 
DOT containing this configuration at 



[daves-244-manywho-sharing.dot.zip](https://drive.google.com/file/d/1X12DQd2BfdkCXX9Pz-hnaOh4dSEuEuqG/view).

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
```.../build/install/activitytool/...```. Since this location is in the build output directories it is just a temporary
install. If you want to place it somewhere more permanent you can take the distribution zip and extract
it anywhere you like.

## Running
You probably want to place the distribution bin directory in your PATH for more convenient use
```
export PATH=$PATH:<projectroot>/build/install/activitytool/bin
```

To learn a little about the command line use:
```bash
activitytool --help
activitytool createSalesContext --help
```
Here's some sample commands that uses defaults for many of the options:
```bash
activitytool createSalesContext --server=https://coreserver:port --username=admin@somewhere.net --password=test1234
activitytool createActivities --server=https://coreserver:port --username=admin@somewhere.net --password=test1234
```

# s3lab
