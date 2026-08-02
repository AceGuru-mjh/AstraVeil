obj-m += astra_root.o

# KDIR is provided externally; points to the GKI kernel headers / build tree.
KDIR ?= /opt/gki-headers

all:
	make -C $(KDIR) M=$(PWD) modules \
		ARCH=arm64 \
		CROSS_COMPILE=aarch64-linux-gnu- \
		KBUILD_MODPOST_WARN=1

clean:
	make -C $(KDIR) M=$(PWD) clean

# Dev mode load (no policy check)
load-dev:
	adb push astra_root.ko /data/local/tmp/
	adb shell su -c "rmmod astra_root 2>/dev/null; insmod /data/local/tmp/astra_root.ko enforce=0"
	adb shell su -c "dmesg | tail -3"

# Production mode load (fail-closed)
load-prod:
	adb push astra_root.ko /data/local/tmp/
	adb shell su -c "rmmod astra_root 2>/dev/null; insmod /data/local/tmp/astra_root.ko enforce=1"
	adb shell su -c "dmesg | tail -3"

# Unload
unload:
	adb shell su -c "rmmod astra_root"
