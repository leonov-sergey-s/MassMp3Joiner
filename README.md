# MassMp3Joiner

MassMp3Joiner this is simple util to recursive find mp3 files in input directory and joined this files to one long mp3 file to output folder. 

Using third party tools:
+ [Mp3Wrap](http://mp3wrap.sourceforge.net/) is a free independent alternative to AlbumWrap. It's a command-line utility that wraps quickly two or more mp3 files in one single large playable mp3, without losing filenames and ID3 informations (and without need of decoding/encoding). 
+ [MP3val](http://mp3val.sourceforge.net/) is a small, high-speed, free software tool for checking MPEG audio files' integrity. 

```
usage: massMp3Joiner
 -c,--chunk-size <arg>   number of mp3 files to join
 -i,--input-dir <arg>    directory to recursive scan
 -mp3val <arg>           path to mp3val util
 -mp3wrap <arg>          path to mp3wrap util
 -o,--output-dir <arg>   directory to write joined mp3 files
 -s,--shuffle            shuffle

NOTE: All mp3 files in output directory will be deleted before start mp3wrap!

```

## Build

```bash
./gradlew clean build
```
After this using zip or tar archives with distribution from  `build/distributions` folder.

## Run

```bash
./gradlew run
```

### config.properties 

```
mp3wrap=../utils/mp3wrap-0.5/mp3wrap.exe
mp3val=../utils/mp3val-0.1.8/mp3val.exe
chunk-size=7
shuffle=true
input-dir=C:/Music/input
output-dir=C:/Music/output
```