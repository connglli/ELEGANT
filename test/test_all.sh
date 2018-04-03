#! /bin/bash

TESTS_HOME=`pwd`
readonly TESTS_HOME

APKS_DIR="apks"
readonly APKS_DIR

test_apks_list=`ls ${APKS_DIR}`
readonly test_apks_list

for apk_name in ${test_apks_list[@]}; do
  echo -e "\033[1;32m========> ${apk_name}\033[0m"
  java -jar ELEGANT.jar \
    --models=models.json \
    --apk="${APKS_DIR}/${apk_name}" \
    --output="techrep/${apk_name}.tech.rep" \
    > "errrep/${apk_name}.err.rep" 2>&1 &&
    echo -e "  \033[1;32msucceeded\033[0m in analysing"
done