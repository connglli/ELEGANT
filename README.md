![ELEGANT](./.github/ELEGANT.png)

### Intro

ELEGANT is a tool us<u>e</u>d to <u>l</u>ocat<u>e</u> fra<u>g</u>ment<u>a</u>tion i<u>n</u>duced compa<u>t</u>ibility issues. A self-implementation of [FicFinder](http://sccpu2.cse.ust.hk/ficfinder/index.html).

### ELEGANT and elegant-cli

By default, we refer *ELEGANT* as a library, but we also provide a command line tool for convenience, i.e. *elegant-cli*. Both released as an uber-jar.

### Build

#### step 1: clone or download

```bash
$ git clone https://github.com/Leetsong/ELEGANT.git
```

#### step2: build with maven

ELEGANT is released by default as a uber-jar, because we modified codes of soot-infoflow-android.

``` bash
$ mvn package
```

You will find the uber-jar `ELEGANT.jar` and `elegant-cli.jar` respectively in the `elegant/target` folder and `elegant-cli/target` folder. If you insist use ELEGANT not
by an uber-jar, the jar file prefixed with `original-` is it (the dependencies, either modified or not,
are installed ahead of time to the m2 directory, you can find them in it).

#### step3: download dbs

Move the jar file to any place that you like, and enter that folder, then enter the following commands to download [ELEGANT-dbs](https://github.com/Leetsong/ELEGANT-dbs), the databases (or datasets, resources) `ELEGANT` and `elegant-cli` needs.

```bash
$ git clone https://github.com/Leetsong/ELEGANT-dbs.git dbs
$ cd dbs && rm -rf .git && cd ..
```

Then you can use it.

### Tutorials - elegant-cli

For easily use, we provide a tool *elegant-cli* that you can use from your command line. And `ELEGANT-cli` is a best practice of using `ELEGANT`. To use *ELEGANT-cli*,

```bash
usage: java -jar elegant-cli.jar [option ...] <apk>
option:
 -d3,--d3-algo <value>        algorithms used in 3rd party library
                              detection, <value> is one of: d3.none,
                              d3.whitelist, d3.libscout.
 -h,--help                    show help
 -m,--models <file>           custom api context models, in json format
 -o,--output <file>           redirect technique report output to <file>
 -p,--platforms <direcotry>   android platforms
 -V,--verbose                 print verbose information
 -v,--version                 show version
```

As shown above,

- `-d3` or `--d3-algo` designate the third party library detection algorithms. `d3.whitelist` as default, `d3.none` and `d3.libscout` are alternatives.
- `-m` or `--models` designate the models json file you want to use. The `model.json` provide in `res` directory is the default one.
- `-o` or `--output` designate the output file that the technique report will redirect to. `stdout` by default.
- `-p` or `--platforms` designate the android platforms directory. `$ANDROID_HOME/platforms` by default.
- `-V` or `—verbose` designate whether output the call chain details. `false` by default.

### Tutorials - ELEGANT

ELEGANT is a library, on top of [Soot]() [3], writing in Java. Having downloaded ELEGANT.jar according to [download](/download), there are 3 steps left to use ELEGANT.

#### 1. Create an `ELEGANT` instance

The first thing you do is to create an `ELEGANT` instance using the builder `ELEGANT.Builder`,

```java
String apkPath       = "some_directory/test.apk";
String modelsPath    = "some_directory/test.models.json";
String platformsPath = "your_android_home/platforms";
String d3Algo        = "d3.whitelist";

ELEGANT.Builder builder = new ELEGANT.Builder();
ELEGANT elegant = builder
  .withApkPath(apkPath)
  .withModelsPath(modelsPath)
  .withPlatformsPath(platformsPath)
  .withD3Algo(d3Algo)
  .build();
```

As shown above, the `ELEGANT.Builder` will guide you to construct a legal `ELEGANT` instance. If any *REQUIRED* field are missed, `ELEGANT.Builder` willfail. All fields are,

- `withApkPath` *REQUIRED* 
- `withModelsPath` *REQUIRED* you can use `models.json`  provided by *elegant-cli*, see details in section ELEGANT-cli
- `withPlatformsPath` *REQUIRED*
- `withD3Algo`  *OPTIONAL* alternatives are `d3.whitelist`, `d3.none` and `d3.libscout`.

#### 2. Watch issues

The second step is to write a issue handle to receive the issues emitted by `ELEGANT`, and watch it.

##### 2.1 Write a Issue Handle

Any issue handle you want to use must inherit from `PubSub.Handle`. `ELEGANT`, by far, reports 2 types of `Issue`, i.e. `PIssue` and `RIssue`, you receive them using your issue handle one by one and take care of them.

```java
public abstract class IssueHandle implements PubSub.Handle {

    @Override
    public void handle(PubSub.Message message) {
        if (!(message instanceof Issue)) {
            return ;
        } else if (message instanceof PIssue){
            System.out.println("PIssue: " + message.toString());
        } else if (message instanceof RIssue){
            System.out.println("RIssue: " + message.toString());
        }
    }
}
```

##### 2.2 Watch it

Then you should tell `ELEGANT` that you want to watch issues by your issue handle.

```java
elegant.watchIssues(new IssueHandle());
```

#### 3. Report issues

The last step is to report them. The report codes should inhabit in your issue handle, but the recommended way is to write your own reporter to take care of them separately and use your issue handle as a proxy from `ELEGANT` to your own reporter.