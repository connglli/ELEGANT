# ELEGANT

## Intro

A self-implementation of [FicFinder](http://sccpu2.cse.ust.hk/ficfinder/index.html).

## Build

#### step 1: clone or download this repo

```bash
$ git clone https://github.com/Leetsong/ELEGANT.git
```

#### step2: build it with maven

ELEGANT is released by default as a uber-jar, because we modified codes of soot-infoflow-android.

``` bash
$ mvn package
```

You will find the uber-jar `ELEGANT.jar` in the `target` folder. If you insist use ELEGANT not
by an uber-jar, the `original-ELEGANT.jar` is it (the dependencies, either modified or not,
are installed ahead of time to the m2 directory, you can find them in it).

#### step3: use it

Enter the folder that containing the jar file, and enter the following commands,

```bash
$ git clone https://github.com/Leetsong/ELEGANT-dbs.git dbs
$ cd dbs && rm -rf .git && cd ..
```

Then you can use it.