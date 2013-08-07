#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <arpa/inet.h>

#include "rfs_packet.h"

rfs_packet* marshal(const uint8_t *buffer, off_t offset, size_t size)
{	
	if(buffer == NULL)
	{
		fprintf(stderr, "buffer is NULL!");
		return NULL;
	}
	
	if(offset + RFS_PACKET_FIX_PART_SIZE > size)
	{
		fprintf(stderr, "buffer is too small!");
		return NULL;
	}

	rfs_packet *packet = (rfs_packet*)malloc(sizeof(rfs_packet));
	if(packet == NULL)
	{
		fprintf(stderr, "cannot allocate memory!");
		return NULL;
	}
	memset(packet, 0, sizeof(rfs_packet));
	
	//we use little endian
	memcpy(&packet->op, buffer + offset, 1);
	memcpy(&packet->code, buffer + (offset + 1), 1);
	memcpy(&packet->trans_id, buffer + (offset + 2), 8);
	memcpy(&packet->offset, buffer + (offset + 10), 4);
	memcpy(&packet->size, buffer + (offset + 14), 4);
	memcpy(&packet->path_size, buffer + (offset + 18), 4);

	if(packet->path_size > 0)
	{
		packet->path = (uint8_t*)malloc(packet->path_size);
		memcpy(packet->path, buffer + (offset + 22), packet->path_size);
	}
	
	memcpy(&packet->data_size, buffer + (offset + 22 + packet->path_size), 4);

	if(packet->data_size > 0)
	{
		packet->data = (uint8_t*)malloc(packet->data_size);
		memcpy(packet->data, buffer + (offset + 26 + packet->path_size), packet->data_size);
	}

	return packet;
}

int serialize(const rfs_packet *packet, uint8_t *buffer, off_t offset, size_t size)
{
	if(packet == NULL)
	{
		fprintf(stderr, "Invalid packet!");
		return -1;
	}
	
	if(offset + get_packet_size(packet) > size)
	{
		fprintf(stderr, "the buffer is too small");
		return -1;
	}
	
	//we use little endian
	memcpy(buffer + offset, &packet->op, 1);
	memcpy(buffer + (offset + 1), &packet->code, 1);
	memcpy(buffer + (offset + 2), &packet->trans_id, 8);
	memcpy(buffer + (offset + 10), &packet->offset, 4);
	memcpy(buffer + (offset + 14), &packet->size, 4);
	memcpy(buffer + (offset + 18), &packet->path_size, 4);
	
	if(packet->path_size > 0)
	{
		memcpy(buffer + (offset + 22), packet->path, packet->path_size);
	}
	memcpy(buffer + (offset + 22 + packet->path_size), &packet->data_size, 4);
	if(packet->data_size > 0)
	{
		memcpy(buffer + (offset + 26 + packet->path_size), packet->data, packet->data_size);
	}
	
	return 0;
}

int free_packet(rfs_packet *packet)
{
	if(packet == NULL)
	{
		return 0;
	}

	if(packet->path != NULL)
	{
		free(packet->path);
	}

	if(packet->data != NULL)
	{
		free(packet->data);
	}

	free(packet);
	
	return 0;
}
	
rfs_packet* create_packet(uint8_t op, uint64_t trans_id, off_t offset, size_t size,
					const char *path, const void* data, size_t data_size)
{
	rfs_packet *packet = (rfs_packet *)malloc(sizeof(rfs_packet));
	memset(packet, 0, sizeof(rfs_packet));

	packet->op = op;
	packet->trans_id = trans_id;
	packet->offset = offset;
	packet->size = size;
	if(path != NULL)
	{
		packet->path_size = strlen(path);
		packet->path = (uint8_t *)malloc(packet->path_size);
		memcpy(packet->path, path, packet->path_size);
	}
	if(data != NULL)
	{
		packet->data_size = data_size;
		packet->data = (uint8_t *)malloc(packet->data_size);
		memcpy(packet->data, data, data_size);
	}
	
	return packet;
}

uint32_t get_packet_size(const rfs_packet *packet)
{
	if(packet != NULL)
	{
		return RFS_PACKET_FIX_PART_SIZE + packet->path_size + packet->data_size;
	}
	
	return 0;
}