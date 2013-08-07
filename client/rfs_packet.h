#ifndef _RFS_PACKET_HEADER_
#define _RFS_PACKET_HEADER_

#include<stdint.h>

#define RFS_OP_READDIR	10
#define RFS_OP_MKDIR	11
#define RFS_OP_MKNOD	12
#define RFS_OP_GETATTR	13
#define RFS_OP_ACCESS	14
#define	RFS_OP_RMDIR	15
#define RFS_OP_RENAME	16
#define RFS_OP_OPEN		17
#define RFS_OP_READ		18
#define	RFS_OP_WRITE	19
#define RFS_OP_RELEASE	20
#define RFS_OP_CREATE	21
#define RFS_OP_UNLINK	22
#define RFS_OP_TRUNCATE	23
#define RFS_OP_UTIMENS	24

#define RFS_PACKET_FIX_PART_SIZE	26

typedef struct _rfs_packet
{
	uint8_t 	op;
	uint8_t		code;
	uint64_t	trans_id;
	uint32_t	offset;
	uint32_t	size;
	uint32_t	path_size;
	uint8_t		*path;
	uint32_t	data_size;
	uint8_t		*data;
} rfs_packet;

rfs_packet* marshal(const uint8_t *buffer, off_t offset, size_t size);

int serialize(const rfs_packet* packet, uint8_t *buffer, off_t offset, size_t size);

int free_packet(rfs_packet* packet);

rfs_packet* create_packet(uint8_t op, uint64_t trans_id, off_t offset, size_t size,
					const char *path, const void* data, size_t data_size);
					
uint32_t get_packet_size(const rfs_packet *packet);
					
#endif
