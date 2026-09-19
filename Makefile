.DEFAULT_GOAL := help

NIX := nix develop -c
EMU := scripts/emulator.sh

.PHONY: help run run-headless start start-headless install build test test-scripts test-device stop status mirror screenshot logs check

help:
	@printf '%s\n' \
	  'make run            Boot with a window, build, install and launch Yealth' \
	  'make run-headless   Boot headless, build, install and launch Yealth' \
	  'make start          Boot the emulator with a window' \
	  'make start-headless Boot the emulator without a window' \
	  'make install        Rebuild and install on the running emulator' \
	  'make build          Build the debug APK' \
	  'make test           Run JVM unit tests' \
	  'make test-scripts   Test emulator orchestration without an SDK' \
	  'make test-device    Boot headless and run Android tests' \
	  'make check          Run JVM and script tests' \
	  'make stop/status/mirror/screenshot/logs'

run:
	$(NIX) $(EMU) up --window

run-headless:
	$(NIX) $(EMU) up

start:
	$(NIX) $(EMU) start --window

start-headless:
	$(NIX) $(EMU) start

install:
	$(NIX) $(EMU) install

build:
	$(NIX) ./gradlew :app:assembleDebug

test:
	$(NIX) ./gradlew :app:testDebugUnitTest

test-scripts:
	python3 scripts/test_emulator.py

test-device:
	$(NIX) bash -c '$(EMU) start && $(EMU) test'

check: test test-scripts

stop:
	$(NIX) $(EMU) stop

status:
	$(NIX) $(EMU) status

mirror:
	$(NIX) $(EMU) mirror

screenshot:
	$(NIX) $(EMU) shot

logs:
	$(NIX) $(EMU) logcat

