#include "udp_utils.h"

//constants

//program arguments
char *client_id;
in_addr_t neigh_ip;
int out_port;
int in_port;

//token
token one_token;
int present_token;

//socket
int self_socket;

//message
int is_waiting;
char waiting_msg_id[50];
char waiting_msg[100];


int main(int argc, char** argv){
    signal(SIGINT, _exit_);
    parse_args(argc, argv);

    /*printf("%s\n", client_id);
    printf("%d\n", neigh_ip);
    printf("%d\n", out_port);
    printf("%d\n", in_port);*/

    init_self_socket();

    join_to_ring();
    signal(SIGTSTP, send_message);

    one_token.type_of_message = FREE;
    while(1){
        forward_msg();
        recv_from_neigh();

        usleep(1000000);
    }

    return 0;
}

void init_self_socket(){
    self_socket = socket(AF_INET, SOCK_DGRAM, 0);
    if(self_socket == -1){
        printf("ERROR self socket failure");
        exit(1);
    }

    struct sockaddr_in addr_in;
    //simplify version of memset
    bzero(&addr_in, sizeof(addr_in));
    addr_in.sin_family = AF_INET;
    addr_in.sin_addr.s_addr = INADDR_ANY;
    addr_in.sin_port = htons((uint16_t) in_port); //convert port number to network byte order"

    if(bind(self_socket, (const struct sockaddr *) &addr_in, sizeof(addr_in)) == -1){
        printf("ERROR self socket binding");
        exit(1);
    }

    //printf("Socket initialised.\n");
}

void join_to_ring() {
    one_token.type_of_message = JOIN;

    sendto_neigh(); 

    recv_from_neigh();
    
    //printf("joined to %s %d\n", next_ip_address, ntohs(address.sin_port));
}

void sendto_neigh() {
    struct sockaddr_in addr_in;
    bzero(&addr_in, sizeof(addr_in));
    addr_in.sin_family = AF_INET;
    addr_in.sin_addr.s_addr = neigh_ip;
    addr_in.sin_port = htons((uint16_t) out_port);
    if(sendto(self_socket, &one_token, sizeof(one_token), 0, (const struct sockaddr*) &addr_in, sizeof(addr_in)) != sizeof(one_token)){
        printf("ERROR Message send failure");
        exit(1);
    }

    //printf("\nSend message to %s %d\n", inet_ntoa(addr_in.sin_addr), ntohs(addr_in.sin_port));
}

void recv_from_neigh() {
    struct sockaddr_in addr_in = receive_addr();

    switch(one_token.type_of_message) {
        case JOIN:
            join_msg(addr_in);
            recv_from_neigh();
            break;
        case JOIN_ACK:
            join_ack_msg(addr_in);
            break;
        case FREE:
            free_msg();
            break;
        case FULL:
            full_msg();
            break;
        case RETURN:
            return_msg();
            break;
    }
}

void send_message(int sig) {
    signal(SIGTSTP, send_message);

    if (is_waiting) {
        printf("\nMessage is still waiting to send");
        return;
    }
    is_waiting = 1;

    printf("\nclient name> \n");

    char *buff1;
    size_t size1 = 100;
    size_t char1;
    buff1 = (char *) malloc(size1 * sizeof(char));
    char1 = (size_t) getline(&buff1, &size1, stdin);
    buff1[char1 - 1] = 0;

    printf("\nmessage> \n");

    char *buff2;
    size_t size2 = 100;
    buff2 = (char *) malloc(size2 * sizeof(char));
    getline(&buff2, &size2, stdin);

    memset(waiting_msg_id, 0, 50);
    memset(waiting_msg, 0, 100);

    strcpy(waiting_msg_id, buff1);
    strcpy(waiting_msg, buff2);

    one_token.ttl = 3;
}

struct sockaddr_in receive_addr() {
    struct sockaddr_in addr_in;
    int len = sizeof(addr_in);
    if(recvfrom(self_socket, &one_token, sizeof(one_token), 0, (struct sockaddr *) &addr_in, (socklen_t *) &len) != sizeof(one_token)){
        printf("ERROR message receive failure");
        exit(1);
    }

    if(one_token.type_of_message == FREE || one_token.type_of_message == FULL){
        printf("%d.) from %s:%d\n", one_token.cnt, inet_ntoa(addr_in.sin_addr), ntohs(addr_in.sin_port));
    }

    return addr_in;
}

void join_msg(struct sockaddr_in addr_in){
    //printf("Received join request from %s %d \n", inet_ntoa(addr_in.sin_addr), ntohs(addr_in.sin_port));

    one_token.type_of_message = JOIN_ACK;
    one_token.address = neigh_ip;
    one_token.port = (in_port_t) out_port;

    neigh_ip = addr_in.sin_addr.s_addr;
    out_port = ntohs(addr_in.sin_port);
    sendto_neigh();
}

void join_ack_msg(struct sockaddr_in addr_in) {
    //printf("Received join_ack from %s %d\n", inet_ntoa(addr_in.sin_addr), ntohs(addr_in.sin_port));

    neigh_ip = one_token.address;
    out_port = one_token.port;
}

void free_msg() {
    send_multicast();
    present_token = 1;
    if (is_waiting) {
        is_waiting = 0;
        memset(one_token.dst, 0, 100);
        memset(one_token.src, 0, 100);
        memset(one_token.msg, 0, 100);
        strcpy(one_token.dst, waiting_msg_id);
        strcpy(one_token.src, client_id);
        strcpy(one_token.msg, waiting_msg);
        memset(waiting_msg_id, 0, 100);
        memset(waiting_msg, 0, 100);
        one_token.type_of_message = FULL;
    }
    one_token.cnt++;
}

void full_msg() {
    send_multicast();
    present_token = 1;
    if (strcmp(one_token.dst, client_id) == 0) {
        handle_msg();
    }
    if (strcmp(client_id, one_token.src) == 0) {
        one_token.ttl--;
        if (one_token.ttl <= 0) {
            //printf("\nMessage to %s deleted due to TTL\n", one_token.dst);
            memset(one_token.dst, 0, 100);
            memset(one_token.msg, 0, 100);
            memset(one_token.src, 0, 100);
            one_token.type_of_message = FREE;
        }
    }

    one_token.cnt++;
}

void return_msg() {
    send_multicast();
    present_token = 1;
    if (strcmp(client_id, one_token.src) == 0) {
        //printf("\nMessage delivered successfully\n");
        memset(one_token.src, 0, 100);
        one_token.cnt++;
        one_token.type_of_message = FREE;
    }
}

void handle_msg() {
    //printf("\n\nThis is message for me: %s\n", one_token.msg);
    memset(one_token.dst, 0, 100);
    memset(one_token.msg, 0, 100);
    one_token.type_of_message = RETURN;
}

void forward_msg() {
    if (!present_token) {
        printf("Out of token\n");
        return;
    }

    sendto_neigh();
    present_token = 0;
}

void send_multicast() {
    struct sockaddr_in addr_in;
    bzero(&addr_in, sizeof(addr_in));

    addr_in.sin_family = AF_INET;
    addr_in.sin_addr.s_addr = inet_addr("224.1.1.1");;
    addr_in.sin_port = htons((uint16_t) 9999);

    if (sendto(self_socket, client_id, strlen(client_id), 0, (const struct sockaddr *) &addr_in, sizeof(addr_in)) != strlen(client_id)) {
        printf("ERROR in multicast send failure");
        exit(1);
    }
}

void _exit_() {

    if (close(self_socket) == -1) {
        printf("ERROR closing in socket failure");
    }

    printf("\nGood Bye!\n");
    exit(0);

}

void parse_args(int argc, char** argv){
    if(argc < 6){
        printf("ERROR Invalid number of arguments!\n");
        printf("USAGE: <prog_name> <client_id> <neighbour_ip_address> <neighbour_port> <self_port> <token_value>\n");
        exit(1);
    }

    client_id = argv[1];

    inet_aton(argv[2], (struct in_addr *) &neigh_ip);
    if(neigh_ip == INADDR_NONE){
        printf("ERROR Invalid IP address.");
        exit(1);
    }

    char* tmp = argv[3];
    out_port = (int) strtol(tmp, &tmp, 10);
    if(*tmp != '\0' && out_port < 1024 || out_port > 60999){
        printf("ERROR Invalid out port number");
        exit(1);
    }

    tmp = argv[4];
    in_port = (int) strtol(tmp, &tmp, 10);
    if(*tmp != '\0' && in_port < 1024 || out_port > 60999){
        printf("ERROR Invalid in port number");
        exit(1);
    }

    tmp = argv[5];
    if(strtol(tmp, &tmp, 10) == 0) present_token = 0;
    else present_token = 1;
    if(present_token){
        one_token.cnt = 0;
        one_token.port = 0;
        one_token.address = 0;
    }

}