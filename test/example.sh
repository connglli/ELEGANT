#! /bin/sh

JAVA_CP=../lib/soot-trunk.jar
readonly JAVA_CP

SOOT_CP=./example/out
readonly SOOT_CP

ENTRY=com.example.fictest.MainActivity
readonly ENTRY

PROCESS_DIR=./example/out
readonly PROCESS_DIR

CLASS_DIR=./example/out
readonly CLASS_DIR

JIMPLE_DIR=./example/output
readonly JIMPLE_DIR

# $1 is input format
# $2 is output format
# $3 is output dir
gen() {
  java \
    -cp ${JAVA_CP} soot.Main \
    -whole-program \
    -soot-classpath ${SOOT_CP} \
    -process-dir ${PROCESS_DIR} \
    -src-prec ${1} \
    -prepend-classpath \
    -allow-phantom-refs \
    -no-bodies-for-excluded \
    -keep-line-number \
    -output-format ${2} \
    -output-dir ${3} \
    -phase-option cg.spark enabled:true \
    ${ENTRY}
}

# $1 is pattern
# $2 is folder
clr() {
  for f in `find ${2} -name "${1}"`; do
    rm -f $f
  done
}

case ${1} in
  "gen-jimple" )
    gen class jimple ${JIMPLE_DIR}
    ;;
  "gen" )
    gen class jimple ${JIMPLE_DIR}
    ;;
  "clr-class" )
    clr "*.class" ${CLASS_DIR}
    rm -rf ${CLASS_DIR}
    ;;
  "clr-jimple" )
    clr "*.jimple" ${JIMPLE_DIR}
    rm -rf ${JIMPLE_DIR_DIR}
    ;;
  "clr" )
    clr "*.jimple" ${JIMPLE_DIR}
    clr "*.class" ${CLASS_DIR}
    rm -rf ${CLASS_DIR}
    rm -rf ${JIMPLE_DIR_DIR}
    rm example.apk
    rm models.json
    ;;
esac