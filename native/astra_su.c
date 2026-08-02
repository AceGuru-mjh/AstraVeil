/*
 * AstraRoot P2 — su binary.
 *
 * Escalates via /dev/astra_root kernel module, then execs target command.
 * Statically linked, no external deps, can run from any path.
 *
 * Compile:
 *   aarch64-linux-android31-clang -static -O2 -o astra_su astra_su.c
 *
 * Usage:
 *   astra_su                    -> interactive root shell
 *   astra_su -c "command"       -> execute single command as root
 *   astra_su 0                  -> shell as UID 0
 *   astra_su 10123              -> shell as UID 10123 (future: policy controlled)
 *   astra_su -c "id" 10123      -> execute command as specific UID
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <pwd.h>
#include <grp.h>

#define ASTRA_DEV       "/dev/astra_root"
#define ASTRA_MAGIC     'A'
#define ASTRA_GET_ROOT    _IO(ASTRA_MAGIC, 1)
#define ASTRA_GET_VERSION _IOR(ASTRA_MAGIC, 4, int)

#define SHELL_PATH "/system/bin/sh"

static void usage(const char *prog)
{
    fprintf(stderr,
        "AstraRoot su — kernel-level privilege escalation\n"
        "\n"
        "Usage:\n"
        "  %s                     Interactive root shell\n"
        "  %s -c \"command\"        Execute command as root\n"
        "  %s -c \"command\" [uid]  Execute command as specific UID\n"
        "  %s [uid]               Shell as specific UID\n"
        "\n"
        "Examples:\n"
        "  %s -c \"id\"\n"
        "  %s -c \"cat /data/adb/magisk/config\"\n"
        "  %s -c \"getprop ro.product.model\"\n"
        "  %s\n",
        prog, prog, prog, prog, prog, prog, prog, prog);
}

static int escalate(void)
{
    int fd = open(ASTRA_DEV, O_RDWR);
    if (fd < 0) {
        fprintf(stderr, "astra_su: cannot open %s: %s\n",
                ASTRA_DEV, strerror(errno));
        fprintf(stderr, "astra_su: is astra_root.ko loaded?\n");
        fprintf(stderr, "astra_su: try: insmod /data/local/tmp/astra_root.ko\n");
        return -1;
    }

    int ret = ioctl(fd, ASTRA_GET_ROOT);
    if (ret != 0) {
        fprintf(stderr, "astra_su: GET_ROOT denied: %s\n", strerror(errno));
        fprintf(stderr, "astra_su: check dmesg for details\n");
        close(fd);
        return -1;
    }

    close(fd);

    if (getuid() != 0) {
        fprintf(stderr, "astra_su: internal error — uid still %d after ioctl\n",
                getuid());
        return -1;
    }

    return 0;
}

static void setup_env(uid_t uid)
{
    /* Set up root environment */
    setenv("PATH",
           "/sbin:/system/bin:/system/xbin:/vendor/bin:"
           "/system/sbin:/product/bin:/apex/com.android.runtime/bin",
           1);
    setenv("HOME", uid == 0 ? "/data" : "/data", 1);
    setenv("USER", uid == 0 ? "root" : "shell", 1);
    setenv("SHELL", SHELL_PATH, 1);
    setenv("TERM", "xterm-256color", 1);

    /* Set UID/GID and supplemental groups */
    if (uid == 0) {
        setgid(0);
        setuid(0);
        gid_t groups[] = {0, 1003, 1004, 1007, 1011, 1015,
                          1028, 1078, 3001, 3002, 3003, 3006,
                          9997};
        setgroups(13, groups);
    }
}

int main(int argc, char *argv[])
{
    int cmd_mode = 0;
    const char *command = NULL;
    uid_t target_uid = 0;
    int arg_offset = 1;

    /* Parse args */
    if (argc > 1 && strcmp(argv[1], "-h") == 0) {
        usage(argv[0]);
        return 0;
    }

    if (argc > 1 && strcmp(argv[1], "-c") == 0) {
        if (argc < 3) {
            fprintf(stderr, "astra_su: -c requires a command\n");
            return 1;
        }
        cmd_mode = 1;
        command = argv[2];
        arg_offset = 3;
    }

    /* Optional UID argument */
    if (argc > arg_offset) {
        target_uid = (uid_t)atoi(argv[arg_offset]);
    }

    /* Escalate */
    if (escalate() != 0)
        return 1;

    /* Set up environment */
    setup_env(target_uid);

    /* Drop to target UID if non-zero */
    if (target_uid != 0) {
        setgid(target_uid);
        setuid(target_uid);
    }

    /* Execute */
    if (cmd_mode) {
        /* su -c "command" */
        char *args[] = {SHELL_PATH, "-c", (char *)command, NULL};
        execv(SHELL_PATH, args);
    } else {
        /* interactive shell */
        char *args[] = {SHELL_PATH, NULL};
        execv(SHELL_PATH, args);
    }

    /* exec does not return unless it failed */
    fprintf(stderr, "astra_su: exec %s failed: %s\n",
            SHELL_PATH, strerror(errno));
    return 1;
}
