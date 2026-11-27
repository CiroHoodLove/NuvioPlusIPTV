#!/usr/bin/env bash
# Compile the app's Kotlin (no APK) and show only what matters. Run from the repo root:  bash check.sh
# The first run takes a long time (downloads everything). Later runs are much faster.
set +e

# Gradle 8.13 cannot run on Codespaces' default Java 25, so pin Java 17 here as well.
JDK17="$(ls -d /usr/lib/jvm/java-17-openjdk-* 2>/dev/null | head -1)"
if [ -z "$JDK17" ]; then echo "Java 17 is missing. Run: bash codespace-setup.sh"; exit 1; fi
export JAVA_HOME="$JDK17"
export PATH="$JAVA_HOME/bin:$PATH"
echo "Java: $(java -version 2>&1 | head -1)"
./gradlew --stop >/dev/null 2>&1

./gradlew :app:compileFullDebugKotlin --build-cache --continue 2>&1 | tee compile.log > /dev/null
code=${PIPESTATUS[0]}
echo "================ ERRORS ================"
grep -E "^e: |What went wrong" -A6 compile.log | head -150
echo "================ RESULT ================"
if [ "$code" = "0" ]; then echo "COMPILES (gradle exit 0)"; else echo "DID NOT COMPILE (gradle exit $code). Copy everything under ERRORS and send it to Claude."; fi
