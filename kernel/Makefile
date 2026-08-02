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
