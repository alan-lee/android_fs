all: remote_fs.o rfs_packet.o comm.o
	cc -lfuse -g -o rfs_mount remote_fs.o rfs_packet.o comm.o

remote_fs.o: remote_fs.c rfs_packet.h remote_fs.h comm.h
	cc -I/usr/local/include/fuse/ -DFUSE_USE_VERSION=29 -D_FILE_OFFSET_BITS=64  -g -c remote_fs.c -o remote_fs.o

rfs_packet.o: rfs_packet.c rfs_packet.h
	cc -g -c rfs_packet.c -o rfs_packet.o

comm.o: comm.c comm.h
	cc -g -c comm.c -o comm.o

test: test_main.c remote_fs.o rfs_packet.o comm.o
    cc -DFUSE_USE_VERSION=29 -D_FILE_OFFSET_BITS=64 \
    -lfuse -g test_main.c remote_fs.o rfs_packet.o comm.o -o rfs_test

clean:
	rm rfs_mount remote_fs.o rfs_packet.o comm.o rfs_test
