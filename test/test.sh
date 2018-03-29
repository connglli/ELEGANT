#! /bin/bash

apk_path=${1}
readonly apk_path

apk_name=$(echo ${apk_path} | cut -d '/' -f 2 | cut -d '.' -f 1)
readonly apk_name

echo -e "\033[1;32m========> ${apk_name}\033[0m"

java -jar ELEGANT.jar \
  --models=models.json \
  --apk="${apk_path}" \
  --output="techrep/${apk_name}.tech.rep" \
  > "errrep/${apk_name}.err.rep" 2>&1 &&

echo -e "  \033[1;32msucceeded\033[0m in analylssing"