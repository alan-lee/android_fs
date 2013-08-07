#ifndef _COMM_HEADER_
#define _COMM_HEADER_

#include <stdint.h>

int socket_connect(const char *ip, uint16_t port);

int32_t socket_read(int sock, void **buff, size_t size, uint32_t msecto);

int32_t socket_write(int sock, const void *buff, size_t size, uint32_t msecto);

int socket_close(int sock);

#endif






