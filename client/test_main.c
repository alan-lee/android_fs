#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <fuse.h>
#include <unistd.h>

extern void rfs_init(const char *server_ip, uint16_t port);

extern void rfs_term();

extern int rfs_readdir(const char *path, void *buf, fuse_fill_dir_t filler,
			off_t offset, struct fuse_file_info *fi);

int print_fuse_fill_dir (void *buf, const char *name, const struct stat *stbuf, off_t off)
{
	printf("file name:%s\n", name);
	printf("size: %lu\n", stbuf->st_size);
	printf("mode: %u\n", stbuf->st_mode);
	printf("link: %lu\n", stbuf->st_nlink);
	printf("ino: %lu\n", stbuf->st_ino);
	printf("ctime: %lu\n", stbuf->st_ctime);
	printf("atime: %lu\n", stbuf->st_atime);
	printf("mtime: %lu\n", stbuf->st_mtime);
	printf("uid: %u\n", stbuf->st_uid);
	printf("gid: %u\n", stbuf->st_gid);
	printf("file type:%o\n", (stbuf->st_mode) & S_IFMT);
}

void test_readdir()
{
	fuse_fill_dir_t test_filler = &print_fuse_fill_dir;
	char *path = "dhcp";
	
	rfs_readdir(path, NULL, test_filler, 0, NULL);
}

void test_mkdir()
{
	char *path = "dhcp/test3";
	
	rfs_mkdir(path, 0755);
}

void test_rmdir()
{
	char *path = "dhcp/test2";
	rfs_rmdir(path);
}

void test_mknod()
{
	char *path = "dhcp/test2/test.txt";
	rfs_mknod(path, 0755, NULL);
}

void test_access()
{
	char *path = "dhcp/test2/test.txt";
	struct fuse_file_info fi = {0};
	
	fi.flags = O_RDWR | O_APPEND;
	rfs_open(path, &fi);
	char *buf = "Hello World!!!";
	char read_buff[20];
	memset(read_buff, 0, 20);
	rfs_write(path, buf, strlen(buf), 0, &fi);
	rfs_read(path, read_buff, 10, 20, &fi);
	printf("%s\n", read_buff);
	rfs_release(path, &fi);
}

void test_stat()
{
	char *path = "/test/pick.c";
	struct stat stbuf;
	memset(&stbuf, 0, sizeof(struct stat));
	rfs_getattr(path, &stbuf);
}

/*int main(int argc, char *argv[])
{
	rfs_init("10.1.23.17", 8888);
	if(argc < 2)
	{
		test_stat();
		//test_access();
		//test_mkdir();
	}
	rfs_term();
}*/