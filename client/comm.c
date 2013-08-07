#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/poll.h>
#include <errno.h>

#include "comm.h"

int socket_connect(const char *ip, uint16_t port)
{
	int sock = socket(AF_INET, SOCK_STREAM, 0);

	struct sockaddr_in serv_addr;
	memset(&serv_addr, 0, sizeof(struct sockaddr_in));
	serv_addr.sin_family = AF_INET;
	serv_addr.sin_port = htons(port);
	if(inet_pton(AF_INET, ip, &serv_addr.sin_addr) <= 0)
	{
		return -1;
	}

	if(connect(sock, (struct sockaddr *)&serv_addr, sizeof(struct sockaddr_in)) >= 0)
	{
		return sock;
	}
	
	if(errno == EINPROGRESS)
	{
		return -EINPROGRESS;
	}
	
	return -1;
}

int32_t socket_read(int sock, void **buff, size_t size, uint32_t msecto)
{
	uint32_t rcvd = 0, total = 0, expected = 0;
	struct pollfd pfd;
	pfd.fd = sock;
	pfd.events = POLLIN;
	while (total < size) 
	{
		pfd.revents = 0;
		if (poll(&pfd, 1, msecto) < 0) 
		{
			return -1;
		}
		if (pfd.revents & POLLIN) 
		{
			rcvd = read(sock, ((uint8_t*)(*buff)) + total, size - total);
			if (rcvd < 0) 
			{
				return rcvd;
			}
			else if(rcvd == 0)
			{
				break;
			}
			else
			{
				if(!expected)
				{
					if(rcvd >= 4)
					{
						memcpy(&expected, *buff, 4);
						if(size < expected + 4)
						{
							uint8_t *new_buff = malloc(expected + 4);
							memcpy(new_buff, buff, size);
							free(*buff);
							*buff = new_buff; 
						}
					}
					else
					{
						errno = EIO;
						return -1;
					}
				}
				total += rcvd;
				
				if(total >= expected)
				{
					break;
				}
			}
		} 
		else 
		{
			errno = ETIMEDOUT;
			return -1;
		}
	}
	
	return total;
}

int32_t socket_write(int sock, const void *buff, size_t size, uint32_t msecto)
{
	struct pollfd pfd;
	pfd.fd = sock;
	pfd.events = POLLOUT;
	pfd.revents = 0;
	
	if (poll(&pfd, 1, msecto) < 0) 
	{
		return -1;
	}
	if (pfd.revents & POLLOUT) 
	{ 
		int send = write(sock, ((uint8_t*)buff), size);
		return send;
	} 
	else 
	{
		errno = ETIMEDOUT;
	}
	 -1;
}

int socket_close(int sock)
{
	shutdown(sock, SHUT_RDWR);
	return close(sock);
}


