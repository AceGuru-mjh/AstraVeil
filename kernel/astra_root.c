// SPDX-License-Identifier: GPL-2.0
/*
 * AstraRoot P2 — kernel module (with policy table).
 *
 * Builds on P1 with:
 *   - Policy table (disabled by default; enabled in P5)
 *   - ASTRA_GET_CAPS fine-grained capability elevation
 *   - ASTRA_SET_POLICY policy loading
 *   - Audit logging (dmesg)
 *
 * Compatible with GKI 5.10 / 5.15 / 6.1 / 6.6.
 */

#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/init.h>
#include <linux/fs.h>
#include <linux/miscdevice.h>
#include <linux/uaccess.h>
#include <linux/cred.h>
#include <linux/capability.h>
#include <linux/sched.h>
#include <linux/slab.h>
#include <linux/mutex.h>
#include <linux/uidgid.h>

#define ASTRA_VERSION   2
#define ASTRA_DEV_NAME  "astra_root"
#define ASTRA_MAGIC     'A'

/* ioctl commands */
#define ASTRA_GET_ROOT    _IO(ASTRA_MAGIC, 1)
#define ASTRA_GET_CAPS    _IOW(ASTRA_MAGIC, 2, struct astra_caps)
#define ASTRA_SET_POLICY  _IOW(ASTRA_MAGIC, 3, struct astra_policy_entry)
#define ASTRA_GET_VERSION _IOR(ASTRA_MAGIC, 4, int)

/* -- data structures -- */

struct astra_caps {
        __u64 cap_effective;
        __u64 cap_permitted;
        __u32 target_uid;
        __u32 target_gid;
        __s32 result;
};

struct astra_policy_entry {
        __u32 uid;
        __u64 allowed_caps;
        __u32 allow_full_root;
        __u32 _pad;
};

/* -- policy table -- */

#define ASTRA_MAX_POLICIES 128
#define ALL_CAPS_MASK ((1ULL << (CAP_LAST_CAP + 1)) - 1)

static struct astra_policy_entry policy_table[ASTRA_MAX_POLICIES];
static int policy_count;
static DEFINE_MUTEX(policy_mutex);

/*
 * P2 phase: when the policy table is empty, allow by default (dev mode).
 * P5 phase: switch to fail-closed (empty policy table = deny all).
 *
 * Controlled via module parameter:
 *   insmod astra_root.ko enforce=1   -> fail-closed
 *   insmod astra_root.ko enforce=0   -> allow all (default)
 */
static int enforce = 0;
module_param(enforce, int, 0444);
MODULE_PARM_DESC(enforce, "1=fail-closed policy, 0=allow-all (dev mode)");

static int check_policy(uid_t uid, __u64 requested_caps, int want_full_root)
{
        int i, allowed = 0;

        /* dev mode: do not check policy */
        if (!enforce)
                return 0;

        mutex_lock(&policy_mutex);

        if (policy_count == 0) {
                mutex_unlock(&policy_mutex);
                return -EPERM;  /* fail-closed */
        }

        for (i = 0; i < policy_count; i++) {
                if (policy_table[i].uid == uid) {
                        if (want_full_root) {
                                allowed = policy_table[i].allow_full_root;
                        } else {
                                allowed = ((requested_caps &
                                           policy_table[i].allowed_caps)
                                           == requested_caps);
                        }
                        break;
                }
        }

        mutex_unlock(&policy_mutex);
        return allowed ? 0 : -EPERM;
}

/* -- ioctl -- */

static long astra_ioctl(struct file *file, unsigned int cmd,
                        unsigned long arg)
{
        struct cred *new;
        int ret;

        switch (cmd) {

        case ASTRA_GET_ROOT: {
                uid_t caller = from_kuid(current_user_ns(), current_uid());

                ret = check_policy(caller, 0, 1);
                if (ret != 0) {
                        pr_warn("astra_root: GET_ROOT denied uid=%d\n", caller);
                        return ret;
                }

                new = prepare_kernel_cred(NULL);
                if (!new)
                        return -ENOMEM;
                commit_creds(new);

                pr_info("astra_root: ROOT pid=%d uid=%d->0\n",
                        current->pid, caller);
                return 0;
        }

        case ASTRA_GET_CAPS: {
                struct astra_caps caps;
                uid_t caller;
                int cap;

                if (copy_from_user(&caps, (void __user *)arg, sizeof(caps)))
                        return -EFAULT;

                caller = from_kuid(current_user_ns(), current_uid());

                ret = check_policy(caller, caps.cap_effective, 0);
                if (ret != 0) {
                        caps.result = ret;
                        if (copy_to_user((void __user *)arg, &caps, sizeof(caps)))
                                return -EFAULT;
                        pr_warn("astra_root: GET_CAPS denied uid=%d caps=0x%llx\n",
                                caller, caps.cap_effective);
                        return ret;
                }

                new = prepare_creds();
                if (!new)
                        return -ENOMEM;

                cap_clear(new->cap_effective);
                cap_clear(new->cap_permitted);
                cap_clear(new->cap_inheritable);
                cap_clear(new->cap_bset);

                for (cap = 0; cap <= CAP_LAST_CAP; cap++) {
                        if (caps.cap_effective & (1ULL << cap))
                                cap_raise(new->cap_effective, cap);
                        if (caps.cap_permitted & (1ULL << cap))
                                cap_raise(new->cap_permitted, cap);
                }

                if (caps.target_uid != 0) {
                        kuid_t kuid = make_kuid(current_user_ns(),
                                                caps.target_uid);
                        new->uid = kuid;
                        new->euid = kuid;
                        new->suid = kuid;
                }
                if (caps.target_gid != 0) {
                        kgid_t kgid = make_kgid(current_user_ns(),
                                                caps.target_gid);
                        new->gid = kgid;
                        new->egid = kgid;
                        new->sgid = kgid;
                }

                commit_creds(new);

                caps.result = 0;
                if (copy_to_user((void __user *)arg, &caps, sizeof(caps)))
                        return -EFAULT;

                pr_info("astra_root: CAPS pid=%d caps=0x%llx uid->%d\n",
                        current->pid, caps.cap_effective, caps.target_uid);
                return 0;
        }

        case ASTRA_SET_POLICY: {
                struct astra_policy_entry entry;

                /* only root may set policy */
                if (!uid_eq(current_uid(), GLOBAL_ROOT_UID))
                        return -EPERM;

                if (copy_from_user(&entry, (void __user *)arg, sizeof(entry)))
                        return -EFAULT;

                mutex_lock(&policy_mutex);
                if (policy_count < ASTRA_MAX_POLICIES) {
                        policy_table[policy_count++] = entry;
                        pr_info("astra_root: policy uid=%d caps=0x%llx full=%d (total=%d)\n",
                                entry.uid, entry.allowed_caps,
                                entry.allow_full_root, policy_count);
                } else {
                        mutex_unlock(&policy_mutex);
                        return -ENOSPC;
                }
                mutex_unlock(&policy_mutex);
                return 0;
        }

        case ASTRA_GET_VERSION: {
                int ver = ASTRA_VERSION;
                if (copy_to_user((void __user *)arg, &ver, sizeof(ver)))
                        return -EFAULT;
                return 0;
        }

        default:
                return -ENOTTY;
        }
}

/* -- device -- */

static int astra_open(struct inode *inode, struct file *file)
{
        return 0;
}

static int astra_release(struct inode *inode, struct file *file)
{
        return 0;
}

static const struct file_operations astra_fops = {
        .owner          = THIS_MODULE,
        .open           = astra_open,
        .release        = astra_release,
        .unlocked_ioctl = astra_ioctl,
        .compat_ioctl   = astra_ioctl,
};

static struct miscdevice astra_dev = {
        .minor = MISC_DYNAMIC_MINOR,
        .name  = ASTRA_DEV_NAME,
        .fops  = &astra_fops,
        .mode  = 0666,
};

/* -- init / exit -- */

static int __init astra_root_init(void)
{
        int ret;

        ret = misc_register(&astra_dev);
        if (ret) {
                pr_err("astra_root: misc_register failed: %d\n", ret);
                return ret;
        }

        mutex_lock(&policy_mutex);
        policy_count = 0;
        memset(policy_table, 0, sizeof(policy_table));
        mutex_unlock(&policy_mutex);

        pr_info("astra_root: v%d loaded -> /dev/%s (enforce=%d)\n",
                ASTRA_VERSION, ASTRA_DEV_NAME, enforce);

        if (!enforce)
                pr_info("astra_root: DEV MODE — all ioctl allowed\n");
        else
                pr_info("astra_root: ENFORCE MODE — policy required\n");

        return 0;
}

static void __exit astra_root_exit(void)
{
        misc_deregister(&astra_dev);
        pr_info("astra_root: unloaded\n");
}

module_init(astra_root_init);
module_exit(astra_root_exit);

MODULE_LICENSE("GPL");
MODULE_AUTHOR("AstraVeil");
MODULE_DESCRIPTION("AstraRoot kernel privilege module");
MODULE_VERSION("2.0");
