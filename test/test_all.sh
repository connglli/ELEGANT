#! /bin/bash

# TESTS_HOME=`pwd`
# readonly TESTS_HOME

# APKS_DIR="apks"
# readonly APKS_DIR

# test_apks_list=`ls ${APKS_DIR}`
# for apk_name in ${test_apks_list}; do
#   echo -e "\033[1;32m========> ${apk_name}\033[0m"
#   java -Xmx2g -jar ELEGANT.jar \
#     --models=models.json \
#     --apk="${APKS_DIR}/${apk_name}" \
#     > "techrep/${apk_name}.tech.rep" 2>&1 &&
#     echo -e "  \033[1;32msucceeded\033[0m in analysing"
# done

test_apks_list=(
  "IrssiNotifier-free.apk"
  "IrssiNotifier-pro.apk"
  "PactrackDroid.apk"
  "transdoid-full.apk"
  "transdoid-lite.apk"
  "AntennaPod.apk"
  "Kore.apk"
  "k-9.apk"
  "android-bankdroid.apk"
  "AnySoftKeyboard.apk"
  "QKSMS-noAnalytics.apk"
  "PocketHub.apk"
)
readonly test_apks_list

TESTS_HOME=`pwd`
readonly TESTS_HOME

APKS_DIR="apks"
readonly APKS_DIR

for apk_name in ${test_apks_list[@]}; do
  echo -e "\033[1;32m========> ${apk_name}\033[0m"
  java -jar ELEGANT.jar \
    --models=models.json \
    --apk="${APKS_DIR}/${apk_name}" \
    --output="techrep/${apk_name}.tech.rep" \
    > "errrep/${apk_name}.err.rep" 2>&1 &&
    echo -e "  \033[1;32msucceeded\033[0m in analysing"
done