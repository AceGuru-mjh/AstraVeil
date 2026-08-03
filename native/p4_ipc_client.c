/*
 * AstraRoot P4 — IPC protocol test client.
 *
 * Sends binary-framed requests to the daemon's Unix domain socket and
 * prints the JSON response. This mirrors the framing that AstraDaemonClient
 * (Kotlin) uses:
 *
 *   Request frame:  [4-byte big-endian length][1-byte type][UTF-8 body]
 *   Response frame: [4-byte big-endian length][1-byte type echo][UTF-8 JSON]
 *
 * Compile (host, for testing against a host-built astrad):
 *   gcc -o p4_ipc_client p4_ipc_client.c
 *
 * Cross-compile (for on-device testing):
 *   aarch64-linux-gnu-gcc -static -O2 -o p4_ipc_client p4_ipc_client.c
 *
 * Usage:
 *   ./p4_ipc_client <socket_path> <type_hex> [body]
 *   ./p4_ipc_client /dev/astra/astrad.sock 04           # ping
 *   ./p4_ipc_client /dev/astra/astrad.sock 05           # get_capability_matrix
 *   ./p4_ipc_client /dev/astra/astrad.sock 03 'id'      # execute
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <arpa/inet.h>
#include <errno.h>

static int connect_socket(const char *path) {
    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) {
        fprintf(stderr, "socket: %s\n", strerror(errno));
        return -1;
    }
    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    strncpy(addr.sun_path, path, sizeof(addr.sun_path) - 1);
    if (connect(fd, (struct sockaddr *)&addr, sizeof(addr)) < 0) {
        fprintf(stderr, "connect(%s): %s\n", path, strerror(errno));
        close(fd);
        return -1;
    }
    return fd;
}

static int send_frame(int fd, unsigned char type, const char *body) {
    size_t body_len = body ? strlen(body) : 0;
    size_t payload_len = 1 + body_len;
    uint32_t net_len = htonl((uint32_t)payload_len);

    /* Send 4-byte length prefix */
    if (write(fd, &net_len, 4) != 4) {
        fprintf(stderr, "write length: %s\n", strerror(errno));
        return -1;
    }
    /* Send type byte */
    if (write(fd, &type, 1) != 1) {
        fprintf(stderr, "write type: %s\n", strerror(errno));
        return -1;
    }
    /* Send body */
    if (body_len > 0) {
        if (write(fd, body, body_len) != (ssize_t)body_len) {
            fprintf(stderr, "write body: %s\n", strerror(errno));
            return -1;
        }
    }
    return 0;
}

static char *recv_frame(int fd, size_t *out_len) {
    uint32_t net_len = 0;
    if (read(fd, &net_len, 4) != 4) {
        fprintf(stderr, "read length: %s\n", strerror(errno));
        return NULL;
    }
    uint32_t len = ntohl(net_len);
    if (len == 0) {
        *out_len = 0;
        return strdup("");
    }
    if (len > 16 * 1024 * 1024) {
        fprintf(stderr, "response too large: %u bytes\n", len);
        return NULL;
    }
    char *buf = malloc(len + 1);
    if (!buf) return NULL;
    size_t total = 0;
    while (total < len) {
        ssize_t n = read(fd, buf + total, len - total);
        if (n <= 0) {
            fprintf(stderr, "read body: %s\n", strerror(errno));
            free(buf);
            return NULL;
        }
        total += (size_t)n;
    }
    buf[len] = '\0';
    *out_len = len;
    return buf;
}

int main(int argc, char *argv[]) {
    if (argc < 3) {
        fprintf(stderr,
            "Usage: %s <socket_path> <type_hex> [body]\n"
            "\n"
            "Types (hex):\n"
            "  01  GetCapability\n"
            "  02  GetProvider\n"
            "  03  Execute (body = command)\n"
            "  04  Ping\n"
            "  05  GetCapabilityMatrix\n"
            "\n"
            "Examples:\n"
            "  %s /dev/astra/astrad.sock 04\n"
            "  %s /dev/astra/astrad.sock 05\n"
            "  %s /dev/astra/astrad.sock 03 'id'\n",
            argv[0], argv[0], argv[0], argv[0]);
        return 1;
    }

    const char *socket_path = argv[1];
    unsigned int type_val = 0;
    if (sscanf(argv[2], "%x", &type_val) != 1 || type_val > 0xFF) {
        fprintf(stderr, "Invalid type: %s (expected hex byte like 04)\n", argv[2]);
        return 1;
    }
    unsigned char type = (unsigned char)type_val;
    const char *body = (argc > 3) ? argv[3] : "";

    int fd = connect_socket(socket_path);
    if (fd < 0) return 1;

    if (send_frame(fd, type, body) < 0) {
        close(fd);
        return 1;
    }

    size_t resp_len = 0;
    char *resp = recv_frame(fd, &resp_len);
    close(fd);

    if (!resp) return 1;

    /* Skip the type-echo byte (first byte) and print the JSON body */
    if (resp_len > 0) {
        fwrite(resp + 1, 1, resp_len - 1, stdout);
        fputc('\n', stdout);
    } else {
        printf("(empty response)\n");
    }

    free(resp);
    return 0;
}
