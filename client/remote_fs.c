#include <fuse.h>
#include <fuse_opt.h>
#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>

#include "comm.h"
#include "remote_fs.h"
#include "rfs_packet.h"

void rfs_init(const char *server_ip, uint16_t port)
{
	g_res.outbuff = (uint8_t *)malloc(DEFAULT_BUFFER_SIZE);
	g_res.outbuff_size = DEFAULT_BUFFER_SIZE;
	g_res.inbuff = (uint8_t *)malloc(DEFAULT_BUFFER_SIZE);
	g_res.inbuff_size = DEFAULT_BUFFER_SIZE;
	
	pthread_mutex_init(&g_res.mutex, NULL);
	if((g_fd = socket_connect(server_ip, port)) < 0)
	{
		fprintf(stderr, "Cannot connect to server: %s(%d)\n", strerror(errno), errno);
	}
}

void rfs_term()
{
	free(g_res.outbuff);
	free(g_res.inbuff);
	pthread_mutex_destroy(&g_res.mutex);
	
	socket_close(g_fd);
}

void serialize_stat(uint8_t *buffer, off_t offset, size_t size, struct stat *stbuf)
{
	if(offset + FILE_STAT_SIZE > size)
	{
		fprintf(stderr, "buffer size is small");
		return;
	}
	
	stbuf->st_uid = getuid();
	stbuf->st_gid = getgid();
	memcpy(&stbuf->st_ino, buffer + (offset + 8), 8);
	memcpy(&stbuf->st_mode, buffer + (offset + 16), 4);
	memcpy(&stbuf->st_size, buffer + (offset + 20), 8);
	stbuf->st_blocks = (stbuf->st_size + 511) / 512;
	memcpy(&stbuf->st_nlink, buffer + (offset + 28), 8);
	memcpy(&stbuf->st_ctime, buffer + (offset + 36), 8);
	memcpy(&stbuf->st_atime, buffer + (offset + 44), 8);
	memcpy(&stbuf->st_mtime, buffer + (offset + 52), 8);
}

//The caller should handle thread security.
rfs_packet* send_and_recv_packet(int sock, rfs_res res, const rfs_packet *send_packet)
{	
	// if need to resend?
	uint8_t resend;
	for(resend = 2; resend > 0; --resend)
	{
		uint32_t packet_size = get_packet_size(send_packet);
		if(packet_size + 4 > res.outbuff_size)
		{
			free(res.outbuff);
			res.outbuff = (uint8_t *)malloc(packet_size + 4);
			res.outbuff_size = packet_size + 4;
		}
		memcpy(res.outbuff, &packet_size, 4);
		serialize(send_packet, res.outbuff, 4, res.outbuff_size);
		int ret = socket_write(sock, res.outbuff, packet_size + 4, 30000);
		if(ret <= 0)
		{
			fprintf(stderr, "send data error: %s(%d)\n", strerror(errno), errno);
			continue;
		}
		
		ret = socket_read(sock, &res.inbuff, res.inbuff_size, 30000);
		if(ret <= 0)
		{
			fprintf(stderr, "receive data error: %s(%d)\n", strerror(errno), errno);
			continue;
		}
		
		memcpy(&packet_size, res.inbuff, 4);
		rfs_packet *recv_packet = marshal(res.inbuff, 4, packet_size);

		return recv_packet;
	}
	
	return NULL;
}

int rfs_getattr(const char *path, struct stat *stbuf)
{
	rfs_packet *packet = create_packet(RFS_OP_GETATTR, 0, 0, 0, path, NULL, 0);
	pthread_mutex_lock(&g_res.mutex);
	
	rfs_packet *response = send_and_recv_packet(g_fd, g_res, packet);
	if(response == NULL || response->code != 0 || response->data_size < 52)
	{ 
		if(response == NULL)
		{
			fprintf(stderr, "I/O error!");
			errno = EIO;
		}
		else
		{
			fprintf(stderr, "Error: %s(%d)", strerror(response->code), response->code);
			errno = response->code;
		}
		
		free_packet(packet);
		free_packet(response);
		pthread_mutex_unlock(&g_res.mutex);
		return -errno;
	}
	
	pthread_mutex_unlock(&g_res.mutex);
	serialize_stat(response->data, 0, response->data_size, stbuf);
	
	free_packet(packet);
	free_packet(response);
	return 0;
}

int rfs_unlink(const char *path)
{
	rfs_packet *packet = create_packet(RFS_OP_UNLINK, 0, 0, 0, path, NULL, 0);
	pthread_mutex_lock(&g_res.mutex);
	
	rfs_packet *response = send_and_recv_packet(g_fd, g_res, packet);
	if(response == NULL || response->code != 0)
	{ 
		if(response == NULL)
		{
			fprintf(stderr, "I/O error!");
			errno = EIO;
		}
		else
		{
			fprintf(stderr, "Error: %s(%d)", strerror(response->code), response->code);
			errno = response->code;
		}
		
		free_packet(packet);
		free_packet(response);
		pthread_mutex_unlock(&g_res.mutex);
		return -errno;
	}

	pthread_mutex_unlock(&g_res.mutex);
	free_packet(packet);
	free_packet(response);
	return 0;
}

int rfs_access(const char *path, int mask)
{
	rfs_packet *packet = create_packet(RFS_OP_ACCESS, 0, 0, 0, path, &mask, 4);
	pthread_mutex_lock(&g_res.mutex);
	
	rfs_packet *response = send_and_recv_packet(g_fd, g_res, packet);
	if(response == NULL || response->code != 0)
	{ 
		if(response == NULL)
		{
			fprintf(stderr, "I/O error!");
			errno = EIO;
		}
		else
		{
			fprintf(stderr, "Error: %s(%d)", strerror(response->code), response->code);
			errno = response->code;
		}
		
		free_packet(packet);
		free_packet(response);
		pthread_mutex_unlock(&g_res.mutex);
		return -errno;
	}

	pthread_mutex_unlock(&g_res.mutex);
	free_packet(packet);
	free_packet(response);
	return 0;
}

int rfs_mknod(const char *path, mode_t mode, dev_t rdev)
{
	rfs_packet *packet = create_packet(RFS_OP_MKNOD, 0, 0, 0, path, &mode, 4);
	pthread_mutex_lock(&g_res.mutex);
	
	rfs_packet *response = send_and_recv_packet(g_fd, g_res, packet);
	if(response == NULL || response->code != 0)
	{ 
		if(response == NULL)
		{
			fprintf(stderr, "I/O error!");
			errno = EIO;
		}
		else
		{
			fprintf(stderr, "Error: %s(%d)", strerror(response->code), response->code);
			errno = response->code;
		}
		
		free_packet(packet);
		free_packet(response);
		pthread_mutex_unlock(&g_res.mutex);
		return -errno;
	}

	pthread_mutex_unlock(&g_res.mutex);
	free_packet(packet);
	free_packet(response);
	return 0;
}

int rfs_readdir(const char *path, void *buf, fuse_fill_dir_t filler,
			off_t offset, struct fuse_file_info *fi)
{	
	rfs_packet *packet = create_packet(RFS_OP_READDIR, 0, 0, 0, path, NULL, 0);
	pthread_mutex_lock(&g_res.mutex);
	
	rfs_packet *response = send_and_recv_packet(g_fd, g_res, packet);
	if(response == NULL || response->code != 0 || response->data_size <= 0)
	{ 
		if(response == NULL)
		{
			fprintf(stderr, "I/O error!");
			errno = EIO;
		}
		else
		{
			fprintf(stderr, "Error: %s(%d)", strerror(response->code), response->code);
			errno = response->code;
		}
		
		free_packet(packet);
		free_packet(response);
		pthread_mutex_unlock(&g_res.mutex);
		return -errno;
	}
	
	pthread_mutex_unlock(&g_res.mutex);
	uint8_t  *guantee= response->data + response->data_size;
	uint8_t *cusor = response->data;
	while(cusor < guantee)
	{
		char *file_name = cusor;
		cusor += strlen(file_name) + 1;
		struct stat st;
		memset(&st, 0, sizeof(struct stat));
		serialize_stat(cusor, 0, 60, &st);
		cusor += FILE_STAT_SIZE;
		filler(buf, file_name, &st, 0);
	}
	free_packet(packet);
	free_packet(response);
	return 0;
}

int rfs_mkdir(const char *path, mode_t mode)
{
	rfs_packet *packet = create_packet(RFS_OP_MKDIR, 0, 0, 0, path, &mode, 4);
	pthread_mutex_lock(&g_res.mutex);
	
	rfs_packet *response = send_and_recv_packet(g_fd, g_res, packet);
	if(response == NULL || response->code != 0)
	{ 
		if(response == NULL)
		{
			fprintf(stderr, "I/O error!");
			errno = EIO;
		}
		else
		{
			fprintf(stderr, "Error: %s(%d)", strerror(response->code), response->code);
			errno = response->code;
		}
		
		free_packet(packet);
		free_packet(response);
		pthread_mutex_unlock(&g_res.mutex);
		return -errno;
	}

	pthread_mutex_unlock(&g_res.mutex);
	free_packet(packet);
	free_packet(response);
	return 0;
}

int rfs_rmdir(const char *path)
{
	rfs_packet *packet = create_packet(RFS_OP_RMDIR, 0, 0, 0, path, NULL, 0);
	pthread_mutex_lock(&g_res.mutex);
	
	rfs_packet *response = send_and_recv_packet(g_fd, g_res, packet);
	if(response == NULL || response->code != 0)
	{ 
		if(response == NULL)
		{
			fprintf(stderr, "I/O error!");
			errno = EIO;
		}
		else
		{
			fprintf(stderr, "Error: %s(%d)", strerror(response->code), response->code);
			errno = response->code;
		}
		
		free_packet(packet);
		free_packet(response);
		pthread_mutex_unlock(&g_res.mutex);
		return -errno;
	};

	pthread_mutex_unlock(&g_res.mutex);
	free_packet(packet);
	free_packet(response);
	return 0;
}

int rfs_rename(const char *from, const char *to)
{
	rfs_packet *packet = create_packet(RFS_OP_RENAME, 0, 0, 0, from, to, strlen(to));
	pthread_mutex_lock(&g_res.mutex);
	
	rfs_packet *response = send_and_recv_packet(g_fd, g_res, packet);
	if(response == NULL || response->code != 0)
	{ 
		if(response == NULL)
		{
			fprintf(stderr, "I/O error!");
			errno = EIO;
		}
		else
		{
			fprintf(stderr, "Error: %s(%d)", strerror(response->code), response->code);
			errno = response->code;
		}
		
		free_packet(packet);
		free_packet(response);
		pthread_mutex_unlock(&g_res.mutex);
		return -errno;
	}

	pthread_mutex_unlock(&g_res.mutex);
	free_packet(packet);
	free_packet(response);
	return 0;
}

int rfs_create(const char *path, mode_t mode, struct fuse_file_info *fi)
{
	uint8_t send_bytes[8];
	memcpy(send_bytes, &mode, 4);
	memcpy(send_bytes + 4, &fi->flags, 4);
	
	rfs_packet *packet = create_packet(RFS_OP_CREATE, 0, 0, 0, path, send_bytes, 8);
	pthread_mutex_lock(&g_res.mutex);
	
	rfs_packet *response = send_and_recv_packet(g_fd, g_res, packet);
	if(response == NULL || response->code != 0)
	{ 
		if(response == NULL)
		{
			fprintf(stderr, "I/O error!");
			errno = EIO;
		}
		else
		{
			fprintf(stderr, "Error: %s(%d)", strerror(response->code), response->code);
			errno = response->code;
		}
		
		free_packet(packet);
		free_packet(response);
		pthread_mutex_unlock(&g_res.mutex);
		return -errno;
	}
	
	pthread_mutex_unlock(&g_res.mutex);
	fi->fh = response->trans_id;
	fi->direct_io = 1;
	fi->keep_cache = 0;

	free_packet(packet);
	free_packet(response);
	return 0;
}

int rfs_open(const char *path, struct fuse_file_info *fi)
{
	rfs_packet *packet = create_packet(RFS_OP_OPEN, 0, 0, 0, path, &fi->flags, 4);
	pthread_mutex_lock(&g_res.mutex);
	
	rfs_packet *response = send_and_recv_packet(g_fd, g_res, packet);
	if(response == NULL || response->code != 0)
	{ 
		if(response == NULL)
		{
			fprintf(stderr, "I/O error!");
			errno = EIO;
		}
		else
		{
			fprintf(stderr, "Error: %s(%d)", strerror(response->code), response->code);
			errno = response->code;
		}
		
		free_packet(packet);
		free_packet(response);
		pthread_mutex_unlock(&g_res.mutex);
		return -errno;
	}
	
	pthread_mutex_unlock(&g_res.mutex);
	fi->fh = response->trans_id;
	fi->direct_io = 1;
	fi->keep_cache = 0;

	free_packet(packet);
	free_packet(response);
	return 0;
}

int rfs_read(const char *path, char *buf, size_t size, off_t offset,
			struct fuse_file_info *fi)
{
	int read_bytes = 0;
	rfs_packet *packet = create_packet(RFS_OP_READ, fi->fh, offset, size, path, NULL, 0);
	pthread_mutex_lock(&g_res.mutex);
	
	rfs_packet *response = send_and_recv_packet(g_fd, g_res, packet);
	if(response == NULL || response->code != 0)
	{ 
		if(response == NULL)
		{
			fprintf(stderr, "I/O error!");
			errno = EIO;
		}
		else
		{
			fprintf(stderr, "Error: %s(%d)", strerror(response->code), response->code);
			errno = response->code;
		}
		
		free_packet(packet);
		free_packet(response);
		pthread_mutex_unlock(&g_res.mutex);
		return -errno;
	}
	
	pthread_mutex_unlock(&g_res.mutex);
	
	read_bytes = response->data_size;
	memcpy(buf, response->data, read_bytes);
	
	free_packet(packet);
	free_packet(response);
	return read_bytes;
}

int rfs_write(const char *path, const char *buf, size_t size,
			off_t offset, struct fuse_file_info *fi)
{
	rfs_packet *packet = create_packet(RFS_OP_WRITE, fi->fh, offset, size, path, buf, size);
	pthread_mutex_lock(&g_res.mutex);
	
	rfs_packet *response = send_and_recv_packet(g_fd, g_res, packet);
	if(response == NULL || response->code != 0)
	{ 
		if(response == NULL)
		{
			fprintf(stderr, "I/O error!");
			errno = EIO;
		}
		else
		{
			fprintf(stderr, "Error: %s(%d)", strerror(response->code), response->code);
			errno = response->code;
		}
		
		free_packet(packet);
		free_packet(response);
		pthread_mutex_unlock(&g_res.mutex);
		return -errno;
	}

	pthread_mutex_unlock(&g_res.mutex);
	
	free_packet(packet);
	free_packet(response);
	return size;
}

int rfs_release(const char *path, struct fuse_file_info *fi)
{
	rfs_packet *packet = create_packet(RFS_OP_RELEASE, fi->fh, 0, 0, path, NULL, 0);
	pthread_mutex_lock(&g_res.mutex);
	
	rfs_packet *response = send_and_recv_packet(g_fd, g_res, packet);
	if(response == NULL || response->code != 0)
	{ 
		if(response == NULL)
		{
			fprintf(stderr, "I/O error!");
			errno = EIO;
		}
		else
		{
			fprintf(stderr, "Error: %s(%d)", strerror(response->code), response->code);
			errno = response->code;
		}
		
		free_packet(packet);
		free_packet(response);
		pthread_mutex_unlock(&g_res.mutex);
		return -errno;
	}
	
	pthread_mutex_unlock(&g_res.mutex);
	
	free_packet(packet);
	free_packet(response);
	return 0;
}

int rfs_utimens(const char *path, const struct timespec ts[2])
{
	uint64_t sec = ts[0].tv_sec; //use 64bit int, discard nanosec
	rfs_packet *packet = create_packet(RFS_OP_UTIMENS, 0, 0, 0, path, &sec, 8);
	pthread_mutex_lock(&g_res.mutex);
	
	rfs_packet *response = send_and_recv_packet(g_fd, g_res, packet);
	if(response == NULL || response->code != 0)
	{ 
		if(response == NULL)
		{
			fprintf(stderr, "I/O error!");
			errno = EIO;
		}
		else
		{
			fprintf(stderr, "Error: %s(%d)", strerror(response->code), response->code);
			errno = response->code;
		}
		
		free_packet(packet);
		free_packet(response);
		pthread_mutex_unlock(&g_res.mutex);
		return -errno;
	}
	
	pthread_mutex_unlock(&g_res.mutex);
	
	free_packet(packet);
	free_packet(response);
	return 0;
}

int rfs_truncate(const char *path, off_t size)
{
	uint64_t length = size; //use 64bit int
	rfs_packet *packet = create_packet(RFS_OP_TRUNCATE, 0, 0, 0, path, &length, 8);
	pthread_mutex_lock(&g_res.mutex);
	
	rfs_packet *response = send_and_recv_packet(g_fd, g_res, packet);
	if(response == NULL || response->code != 0)
	{ 
		if(response == NULL)
		{
			fprintf(stderr, "I/O error!");
			errno = EIO;
		}
		else
		{
			fprintf(stderr, "Error: %s(%d)", strerror(response->code), response->code);
			errno = response->code;
		}
		
		free_packet(packet);
		free_packet(response);
		pthread_mutex_unlock(&g_res.mutex);
		return -errno;
	}
	
	pthread_mutex_unlock(&g_res.mutex);
	
	free_packet(packet);
	free_packet(response);
	return 0;
}

struct fuse_operations rfs_oper = {
	.getattr	= rfs_getattr,
	.readlink	= rfs_readlink,
	.mknod		= rfs_mknod,
	.mkdir		= rfs_mkdir,
	.unlink		= rfs_unlink,
	.rmdir		= rfs_rmdir,
	.symlink	= rfs_symlink,
	.rename		= rfs_rename,
	.link       = rfs_link,
	.chmod      = rfs_chmod,
    .chown      = rfs_chown,
	.open		= rfs_open,
	.read		= rfs_read,
	.write		= rfs_write,
	.flush		= rfs_flush,
	.release	= rfs_release,
	.readdir	= rfs_readdir,
	.access		= rfs_access,
	.create		= rfs_create,
	.utimens	= rfs_utimens,
	.truncate	= rfs_truncate,
};

struct rfs_param
{
	char 		*host;
	uint16_t 	port;
	uint8_t 	log_level;
	char 		*log_file;
	uint8_t		help;
};

#define RFS_OPT(t, p, v) { t, offsetof(struct rfs_param, p), v }

struct fuse_opt rfs_opts[] = {
	RFS_OPT("-h %s",			host, 0),
	RFS_OPT("--host=%s",		host, 0),
	RFS_OPT("-p %u",			port, 0),
	RFS_OPT("--port=%u",		port, 0),
	RFS_OPT("--log_level=%u",	log_level, 0),
	RFS_OPT("--log_file=%s",	log_file, 0),
	FUSE_OPT_KEY("--help",		0),
	FUSE_OPT_END
};

void usage(const char *program)
{
	fprintf(stderr, "Usage: %s mountpoint --host=HOST --port=PORT [options] \n\n", program);
	fprintf(stderr, "RFS options:\n");
	fprintf(stderr, "  --help                  Print help message\n");
	fprintf(stderr, "  --host=HOST, -h HOST    RFS server host address\n");
	fprintf(stderr, "  --port=PORT, -p PORT    RFS server port\n");
	fprintf(stderr, "  --log_level=N           Debug log level, 0=INFO, 1=DEBUG, 2=WARN, 3=ERROR\n");
	fprintf(stderr, "  --log_file=FILE         Debug log File\n");
}

int rfs_process_arg(void *data, const char *arg, int key, struct fuse_args *outargs)
{
	struct rfs_param *param = data;

	(void)outargs;
	(void)arg;

	switch (key) 
	{
	case FUSE_OPT_KEY_OPT:
		return 1;
	case FUSE_OPT_KEY_NONOPT:
		return 1;
	case 0:
		usage(outargs->argv[0]);
		param->help = 1;
		return fuse_opt_add_arg(outargs, "-ho");
	default:
		return 1;
	}
}

int mainloop(struct fuse_args *args)
{
	struct fuse_chan *ch;
	struct fuse *fuse;
	char *mountpoint;
	int res, mt, fg;
	
	if (fuse_parse_cmdline(args, &mountpoint,&mt,&fg)<0) 
	{
		fprintf(stderr,"see: %s -h for help\n",args->argv[0]);
		return -1;
	}

	ch = fuse_mount(mountpoint, args);
	if (!ch) {
		fprintf(stderr, "Cannot mount RFS\n");
		return -1;
	}
	
	do
	{
		fuse = fuse_new(ch, args, &rfs_oper, sizeof(rfs_oper), NULL);
		if (fuse == NULL)
			break;

		res = fuse_daemonize(fg);
		if (res == -1)
			break;

		res = fuse_set_signal_handlers(fuse_get_session(fuse));
		if (res == -1)
			break;
		
		if (mt)
			res = fuse_loop_mt(fuse);
		else
			res = fuse_loop(fuse);

		fuse_teardown(fuse, mountpoint);
		if (res == -1)
			return 1;

		return 0;
	}
	while(0);
	
	fuse_unmount(mountpoint, ch);
	free(mountpoint);
	if (fuse)
		fuse_destroy(fuse);
	return -1;
	
}


int main(int argc, char *argv[])
{
	int mt, fg;
	char *mountpoint;
	struct fuse_args args = FUSE_ARGS_INIT(argc, argv);
	struct rfs_param param = { NULL, 0, 0, NULL, 0 };

	if (fuse_opt_parse(&args, &param, rfs_opts, rfs_process_arg)) 
	{
		fprintf(stderr,"see: %s -h for help\n",argv[0]);
		return 1;
	}
	
	if(param.help)
	{
		return 0;
	}

	
	if (!(param.host && param.port))
	{
		fprintf(stderr,"see: %s --help for help\n",argv[0]);
		return 1;	
	}
	
	if (param.log_level)
	{
		param.log_level = 3;
	}
	
	if (param.log_file)
	{
		param.log_file = "/var/log/rfs-mount.log";
	}
	
	rfs_init(param.host, param.port);
	
	int ret = mainloop(&args);
	rfs_term();
	return ret;
}
  
