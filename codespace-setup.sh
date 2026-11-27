#!/usr/bin/env bash
# One-time setup for a GitHub Codespace so you can compile the Nuvio app's Kotlin there.
# Run from the repo root:   bash codespace-setup.sh
# Takes about 5 to 10 minutes. Safe to run again; it skips what is already installed.
#
# Fix: Codespaces ships Java 25, and Gradle 8.13 cannot run on it (it fails with
# "What went wrong: 25.0.5.0.2"). The old version of this script installed Java 17 but
# still pointed JAVA_HOME at Java 25. This one pins everything to Java 17 explicitly,
# and replaces the old block it wrote into ~/.bashrc.
set -euo pipefail

SDK="$HOME/android-sdk"
GO_VERSION="1.27.3"          # same as .github/workflows/*-steps.yml
NDK_VERSION="29.0.14206865"  # the app's ndkVersion in app/build.gradle.kts

echo "== 1/5 Java 17"
find_jdk17() { ls -d /usr/lib/jvm/java-17-openjdk-* 2>/dev/null | head -1 || true; }
JAVA_HOME_DIR="$(find_jdk17)"
if [ -z "$JAVA_HOME_DIR" ] || [ ! -x "$JAVA_HOME_DIR/bin/javac" ]; then
  sudo apt-get update -y
  sudo apt-get install -y openjdk-17-jdk-headless unzip curl
  JAVA_HOME_DIR="$(find_jdk17)"
fi
[ -x "$JAVA_HOME_DIR/bin/javac" ] || { echo "Java 17 did not install"; exit 1; }
echo "Using $JAVA_HOME_DIR"

echo "== 2/5 Android SDK command line tools"
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  mkdir -p "$SDK/cmdline-tools"
  curl -fsSL -o /tmp/cmdtools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q -o /tmp/cmdtools.zip -d "$SDK/cmdline-tools"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
fi

echo "== 3/5 Android platform, build tools and NDK"
export JAVA_HOME="$JAVA_HOME_DIR"
export PATH="$JAVA_HOME/bin:$PATH"
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --licenses > /dev/null || true
"$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" \
  "platform-tools" "platforms;android-36" "build-tools;36.0.0" "ndk;$NDK_VERSION"

echo "== 4/5 Go $GO_VERSION (the app builds a small native part with it)"
if ! /usr/local/go/bin/go version 2>/dev/null | grep -q "go$GO_VERSION"; then
  curl -fsSL -o /tmp/go.tgz "https://go.dev/dl/go${GO_VERSION}.linux-amd64.tar.gz"
  sudo rm -rf /usr/local/go
  sudo tar -C /usr/local -xzf /tmp/go.tgz
fi

echo "== 5/5 Environment"
printf 'sdk.dir=%s\n' "$SDK" > local.properties

# Gradle itself: always Java 17, whatever the shell says.
mkdir -p "$HOME/.gradle"
touch "$HOME/.gradle/gradle.properties"
sed -i '/^org.gradle.java.home=/d' "$HOME/.gradle/gradle.properties"
echo "org.gradle.java.home=$JAVA_HOME_DIR" >> "$HOME/.gradle/gradle.properties"

# Replace the block an earlier run wrote (it pointed JAVA_HOME at Java 25).
sed -i '/# NUVIO_SETUP/,/^export PATH=.*\/usr\/local\/go\/bin/d' "$HOME/.bashrc" 2>/dev/null || true
cat >> "$HOME/.bashrc" <<RC

# NUVIO_SETUP
export JAVA_HOME="$JAVA_HOME_DIR"
export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
export PATH="\$JAVA_HOME/bin:/usr/local/go/bin:\$ANDROID_HOME/platform-tools:\$PATH"
RC
chmod +x gradlew
./gradlew --stop >/dev/null 2>&1 || true
echo
echo "Done. Open a NEW terminal (or run: source ~/.bashrc), then run:  bash check.sh"
