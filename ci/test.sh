#!/usr/bin/env bash
# Автотест ЛР5 «Оценка надёжности по ГОСТ 28195-89».
# Считает фактор надёжности по input.txt и сравнивает результат с эталоном output.txt.
# Запуск: bash ci/test.sh <папка со скомпилированными классами>   (по умолчанию out)
set -uo pipefail

CP="${1:-out}"
ACTUAL=$(mktemp)

java -cp "$CP" ReliabilityCalculator input.txt "$ACTUAL"

if diff -u <(tr -d '\r' < output.txt) "$ACTUAL"; then
  echo "OK    результат совпадает с эталоном output.txt"
  grep "K_ф" "$ACTUAL"
else
  echo "FAIL  результат расчёта отличается от эталона output.txt"
  exit 1
fi
