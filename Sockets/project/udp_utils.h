#ifndef COMMUNICATION_SETTINGS_H
#define COMMUNICATION_SETTINGS_H

#include <stdlib.h>
#include <stdio.h>
#include <memory.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/un.h>
#include <sys/epoll.h>
#include <pthread.h>
#include <zconf.h>
#include <signal.h>
#include <jmorecfg.h>

//shared structs
typedef enum message {
    JOIN = 0,
    JOIN_ACK = 1,
    FREE = 2,
    FULL = 3,
    RETURN = 4,
} message;

typedef struct token {
    char src[100];
    char dst[100];
    char msg[100];
    message type_of_message;
    in_addr_t address;
    in_port_t port;
    int cnt;
    int ttl;
} token;

//functions to udp & tcp
void init_self_socket();
void join_to_ring();

void sendto_neigh();

void recv_from_neigh();
struct sockaddr_in receive_addr();

void send_message(int sig);
void forward_msg();

void join_msg(struct sockaddr_in addr);
void join_ack_msg(struct sockaddr_in addr);
void free_msg();
void full_msg();
void return_msg();

void handle_msg();

void send_multicast();

void parse_args(int argc, char **argv);

//atexit function
void _exit_();


#endif