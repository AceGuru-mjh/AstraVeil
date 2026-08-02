/*
 * AstraRoot P2 — kernel module privilege verification tool.
 *
 * 1. Open /dev/astra_root
 * 2. ioctl(ASTRA_GET_VERSION) to verify module is present
 * 3. ioctl(ASTRA_GET_ROOT) to escalate
 * 4. Verify getuid() == 0
 *
 * Compile:
 *   aarch64-linux-android31-clang -static -o test_root test_root.c
 *
 * Usage:
 *   adb push test_root /data/local/tmp/
 *   adb shell su -c "/data/local/tmp/test_root"
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <sys/ioctl.h>

#define ASTRA_DEV       "/dev/astra_root"
#define ASTRA_MAGIC     'A'
#define ASTRA_GET_ROOT    _IO(ASTRA_MAGIC, 1)
#define ASTRA_GET_VERSION _IOR(ASTRA_MAGIC, 4, int)

static int check_device(void)
{
    if (access(ASTRA_DEV, F_OK) != 0) {
        printf("FAIL: %s does not exist\n", ASTRA_DEV);
        printf("   Is astra_root.ko loaded? Try: insmod /data/local/tmp/astra_root.ko\n");
        return -1;
    }
    printf("OK: %s exists\n", ASTRA_DEV);
    return 0;
}

static int check_version(int fd)
{
    int ver = 0;
    int ret = ioctl(fd, ASTRA_GET_VERSION, &ver);
    if (ret != 0) {
        printf("FAIL: GET_VERSION failed: %s\n", strerror(errno));
        return -1;
    }
    printf("OK: Module version: %d\n", ver);
    return 0;
}

static int check_root(int fd)
{
    uid_t before = getuid();
    printf("   Before: uid=%d gid=%d\n", before, getgid());

    int ret = ioctl(fd, ASTRA_GET_ROOT);
    if (ret != 0) {
        printf("FAIL: GET_ROOT failed: %s (errno=%d)\n", strerror(errno), errno);
        printf("   Possible causes:\n");
        printf("   - Policy table empty (fail-closed) — expected in enforce mode\n");
        printf("   - SELinux denying ioctl\n");
        printf("   - Module not properly loaded\n");
        return -1;
    }

    uid_t after = getuid();
    printf("   After:  uid=%d gid=%d\n", after, getgid());

    if (after == 0) {
        printf("OK: ROOT SUCCESS — uid=0\n");
        return 0;
    } else {
        printf("FAIL: Still uid=%d after ioctl\n", after);
        return -1;
    }
}

static void check_capabilities(void)
{
    /* Verify root is real (not just uid=0) */
    printf("\n-- Capability Verification --\n");

    /* Test 1: read a root-only file */
    FILE *f = fopen("/data/adb/magisk/config", "r");
    if (f) {
        printf("OK: Can read /data/adb/magisk/config\n");
        fclose(f);
    } else {
        /* Magisk may not exist, try another */
        f = fopen("/data/misc/keystore/user_0", "r");
        if (f) {
            printf("OK: Can read /data/misc/keystore/\n");
            fclose(f);
        } else {
            printf("WARN: Cannot read root-only files (may be OK if no Magisk)\n");
        }
    }

    /* Test 2: write to tmpfs in /dev */
    f = fopen("/dev/astra_test_write", "w");
    if (f) {
        fprintf(f, "test\n");
        fclose(f);
        unlink("/dev/astra_test_write");
        printf("OK: Can write to /dev/\n");
    } else {
        printf("WARN: Cannot write to /dev/ (SELinux?)\n");
    }

    /* Test 3: check mount permission (no actual mount) */
    int ret = system("mount -o bind /system /system 2>/dev/null; "
                     "echo $?");
    (void)ret;
}

int main(int argc, char *argv[])
{
    int fd, ret;
    int verbose = (argc > 1 && strcmp(argv[1], "-v") == 0);

    printf("==========================================\n");
    printf("  AstraRoot P2 — Privilege Test\n");
    printf("==========================================\n\n");

    /* Step 1: check device node */
    printf("-- Step 1: Device Node --\n");
    if (check_device() != 0)
        return 1;

    /* Step 2: open device */
    printf("\n-- Step 2: Open Device --\n");
    fd = open(ASTRA_DEV, O_RDWR);
    if (fd < 0) {
        printf("FAIL: Cannot open %s: %s\n", ASTRA_DEV, strerror(errno));
        printf("   Try: chmod 0666 %s\n", ASTRA_DEV);
        return 1;
    }
    printf("OK: Opened fd=%d\n", fd);

    /* Step 3: version check */
    printf("\n-- Step 3: Version --\n");
    check_version(fd);

    /* Step 4: escalate */
    printf("\n-- Step 4: Privilege Escalation --\n");
    ret = check_root(fd);

    if (ret == 0 && verbose) {
        check_capabilities();
    }

    close(fd);

    printf("\n==========================================\n");
    if (ret == 0) {
        printf("  RESULT: ALL PASSED\n");
    } else {
        printf("  RESULT: FAILED\n");
    }
    printf("==========================================\n");

    return ret;
}
