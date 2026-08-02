// SPDX-License-Identifier: GPL-2.0
/*
 * AstraRoot P1 — minimal loadable kernel module.
 *
 * Registers /dev/astra_root; ioctl ASTRA_GET_ROOT elevates caller to UID 0.
 * Compatible with GKI 5.10 / 5.15 / 6.1 / 6.6 / 6.12.
 */

#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/init.h>
#include <linux/fs.h>
#include <linux/miscdevice.h>
#include <linux/uaccess.h>
#include <linux/cred.h>
#include <linux/sched.h>
#include <linux/uidgid.h>

#define ASTRA_VERSION   1
#define ASTRA_DEV_NAME  "astra_root"
#define ASTRA_MAGIC     'A'

#define ASTRA_GET_ROOT    _IO(ASTRA_MAGIC, 1)
#define ASTRA_GET_VERSION _IOR(ASTRA_MAGIC, 4, int)

/* -- ioctl -- */
static long astra_ioctl(struct file *file, unsigned int cmd, unsigned long arg)
{
	switch (cmd) {

	case ASTRA_GET_ROOT: {
		struct cred *new;
		kuid_t old_uid = current_uid();

		new = prepare_kernel_cred(NULL);
		if (!new)
			return -ENOMEM;
		commit_creds(new);

		pr_info("astra_root: pid %d elevated (uid %d -> 0)\n",
			current->pid, from_kuid(current_user_ns(), old_uid));
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

/* -- file_operations -- */
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

/* -- misc device -- */
static struct miscdevice astra_dev = {
	.minor = MISC_DYNAMIC_MINOR,
	.name  = ASTRA_DEV_NAME,
	.fops  = &astra_fops,
	.mode  = 0666,
};

/* -- init / exit -- */
static int __init astra_root_init(void)
{
	int ret = misc_register(&astra_dev);
	if (ret) {
		pr_err("astra_root: misc_register failed: %d\n", ret);
		return ret;
	}
	pr_info("astra_root: v%d loaded -> /dev/%s\n",
		ASTRA_VERSION, ASTRA_DEV_NAME);
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
MODULE_DESCRIPTION("AstraRoot kernel module");
MODULE_VERSION("1.0");
