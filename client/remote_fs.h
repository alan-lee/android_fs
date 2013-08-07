#ifndef _REMOTE_FS_HEADER_
#define _REMOTE_FS_HEADER_

#include <pthread.h>
#include <stdint.h>
#include <limits.h>

#include "rfs_packet.h"

#define DEFAULT_BUFFER_SIZE 0x10000
#define FILE_STAT_SIZE 		60

typedef struct _rfs_res{
	void			*outbuff;
	uint32_t		outbuff_size;
	uint32_t		outbuff_leng;
	void			*inbuff;
	uint32_t		inbuff_size;
	uint32_t		inbuff_leng;
	
	pthread_mutex_t	mutex;
} rfs_res; 

rfs_res g_res = {0};

int g_fd = -1;

int rfs_getattr(const char *path, struct stat *stbuf);

int rfs_access(const char *path, int mask);

int rfs_readdir(const char *path, void *buf, fuse_fill_dir_t filler,
			off_t offset, struct fuse_file_info *fi);

int rfs_mknod(const char *path, mode_t mode, dev_t rdev);

int rfs_mkdir(const char *path, mode_t mode);

int rfs_rmdir(const char *path);

int rfs_rename(const char *from, const char *to);

int rfs_create(const char *path, mode_t mode, struct fuse_file_info *fi);

int rfs_open(const char *path, struct fuse_file_info *fi);

int rfs_read(const char *path, char *buf, size_t size, off_t offset,
			struct fuse_file_info *fi);
			
int rfs_truncate(const char *path, off_t size);

int rfs_write(const char *path, const char *buf, size_t size,
			off_t offset, struct fuse_file_info *fi);

int rfs_flush(const char *path, struct fuse_file_info *fi)
{
	return 0;
}

int rfs_release(const char *path, struct fuse_file_info *fi);

int rfs_chmod(const char *path, mode_t mode)
{
	return 0;
}

int rfs_chown(const char *path, uid_t uid, gid_t gid)
{
	return 0;
}

int rfs_fsync(const char *path, int isdatasync, struct fuse_file_info *fi)
{
	return 0;
}

int rfs_readlink(const char *path, char *buf, size_t size)
{
	return 0;
}

int rfs_link(const char *path, const char *to)
{
	return 0;
}

int rfs_unlink(const char *path);

int rfs_symlink(const char *from, const char *to)
{
	return 0;
}

void rfs_init(const char *server_ip, uint16_t port);

void rfs_term();

rfs_packet* send_and_recv_packet(int sock, rfs_res res, const rfs_packet *send_packet);

void serialize_stat(uint8_t *buffer, off_t offset, size_t size, struct stat *stbuf);

void get_relative_path(const char *base_path, const char *absolute_path, char *relavte_path);

#endif