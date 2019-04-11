#include "tcp_utils.h"

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
int out_socket;

int monitor_fd;

//message
int is_waiting;
char waiting_msg_id[50];
char waiting_msg[100];

int main(int argc, char **argv) {
    signal(SIGINT, _exit_);
    parse_args(argc, argv);
    init_self_socket();
    init_monitor();
    init_out_socket();

    join_to_ring();
    signal(SIGTSTP, send_message);

    one_token.type_of_message = FREE;
    struct epoll_event event;
    forward_msg();
    while(1){
        if(epoll_wait(monitor_fd, &event, 1, -1) == -1){
            printf("ERROR epoll wait failure");
            exit(1);
        }

        if(event.data.fd < 0){
            register_socket(-event.data.fd);
        }else{
            receive_message(event.data.fd);
        }
    }
}


void send_message(int sig) {
    signal(SIGTSTP, send_message);

    if (is_waiting) {
        printf("\nThere is already waiting message to send\n");
        return;
    }
    is_waiting = 1;

    printf("\nclient name>\n");

    char *buff1;
    size_t size1 = 100;
    size_t char1;
    buff1 = (char *) malloc(size1 * sizeof(char));
    char1 = (size_t) getline(&buff1, &size1, stdin);
    buff1[char1 - 1] = 0;
    printf("\nmessage>\n");

    char *buff2;
    size_t size2 = 100;
    buff2 = (char *) malloc(size2 * sizeof(char));
    getline(&buff2, &size2, stdin);

    memset(waiting_msg_id, 0, 100);
    memset(waiting_msg, 0, 100);

    strcpy(waiting_msg_id, buff1);
    strcpy(waiting_msg, buff2);

    one_token.ttl = 3;
}

void parse_args(int argc, char **argv) {
    if(argc < 6){
        printf("ERROR Invalid number of arguments!\n");
        printf("USAGE: <prog_name> <client_id> <neighbour_ip_address> <neighbour_port> <self_port> <token_value>\n");
        exit(1);
    }

    client_id = argv[1];

    inet_aton(argv[2], (struct in_addr *) &neigh_ip);
    printf("%d\n", neigh_ip);
    if (neigh_ip == INADDR_NONE){
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

void init_self_socket() {

    self_socket = socket(AF_INET, SOCK_STREAM, 0);
    if (self_socket == -1){
        printf("ERROR self socket failure");
        exit(1);
    }

    int true = 1;
    setsockopt(self_socket, SOL_SOCKET, SO_REUSEADDR, &true, sizeof(int));

    struct sockaddr_in in_address;
    bzero(&in_address, sizeof(in_address));
    in_address.sin_family = AF_INET;
    in_address.sin_addr.s_addr = INADDR_ANY;
    in_address.sin_port = htons((uint16_t) in_port);

    if (bind(self_socket, (const struct sockaddr *) &in_address, sizeof(in_address)) == -1){
        printf("ERROR self socket binding");
        exit(1);
    }

    if (listen(self_socket, 4) == -1){ //set current max number of clients
        printf("ERROR self socket listening");
        exit(1);
    }

    //printf("In socket initialised\n");
}

void init_out_socket() {

    if (out_socket != 0){
        if (shutdown(out_socket, SHUT_RDWR) == -1){
            printf("ERROR out socket shutdown failure");
            exit(1);
        }
        if (close(out_socket) == -1){
            printf("ERROR out socket close failure");
            exit(1);
        }
    }

    out_socket = socket(AF_INET, SOCK_STREAM, 0);
    if (out_socket == -1){
        printf("ERROR out socket creation failure");
        exit(1);
    }
    int true = 1;
    setsockopt(out_socket, SOL_SOCKET, SO_REUSEADDR, &true, sizeof(int));

    struct sockaddr_in address;
    bzero(&address, sizeof(address));
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = neigh_ip;
    address.sin_port = htons((uint16_t) out_port);

    if (connect(out_socket, (const struct sockaddr *) &address, sizeof(address)) == -1){
        printf("out socket connect failure");
        exit(1);
    }

    //printf(stderr, "out socket initialised\n");
}

void init_monitor(){
    monitor_fd= epoll_create1(0);
    if (monitor_fd == -1){
        printf("ERROR monitor_fd monitor failure");
        exit(1);
    }

    struct epoll_event event;
    event.events = EPOLLIN | EPOLLPRI;
    event.data.fd = -self_socket;

    if (epoll_ctl(monitor_fd, EPOLL_CTL_ADD, self_socket, &event) == -1){
        printf("ERROR during adding socket to monitor");
        exit(1);
    }

}

void register_socket(int socket){
    int new_socket = accept(socket, NULL, NULL);
    if (new_socket == -1){
        printf("ERROR client registration failure");
        exit(1);
    }

    struct epoll_event event;
    event.events = EPOLLIN | EPOLLPRI;
    event.data.fd = new_socket;

    if(epoll_ctl(monitor_fd, EPOLL_CTL_ADD, new_socket, &event) == -1){
        printf("ERROR during adding to monitor");
        exit(1);
    }

}

void join_to_ring() {
    one_token.type_of_message = JOIN;
    one_token.port = (in_port_t) in_port;
    tcp_send();
}

void forward_msg() {
    if (!present_token) {
        printf("Out of token\n");
        return;
    }

    tcp_send();
    present_token = 0;
}

void receive_message(int socket) {
    struct sockaddr_in address = tcp_receive(socket);
    if (address.sin_port == 0)
        return;

    switch (one_token.type_of_message) {
        case JOIN:
            join_msg(address);
            break;
        case JOIN_ACK:
            join_ack_msg(address);
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
    forward_msg();
    usleep(500000);

}

void join_msg(struct sockaddr_in addr) {
    //printf("Received join request from %s %d\n", inet_ntoa(addr.sin_addr), ntohs(addr.sin_port));

    in_port_t new_out_port = one_token.port;

    one_token.type_of_message = JOIN_ACK;
    one_token.address = neigh_ip;
    one_token.port = (in_port_t) out_port;

    neigh_ip = addr.sin_addr.s_addr;
    out_port = new_out_port;
    init_out_socket();
    tcp_send();
}

void join_ack_msg(struct sockaddr_in addr) {
    //printf("Received join_ack from %s %d\n", inet_ntoa(addr.sin_addr), ntohs(addr.sin_port));

    neigh_ip = one_token.address;
    out_port = one_token.port;
    init_out_socket();
}

void free_msg() {
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
    present_token = 1;

    if (strcmp(one_token.dst, client_id) == 0) {
        handle_message();
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
    present_token = 1;

    if (strcmp(client_id, one_token.src) == 0) {
        //printf("\nMessage delivered successfully\n");

        memset(one_token.src, 0, 100);
        one_token.cnt++;
        one_token.type_of_message = FREE;
    }
}

void handle_message() {
    //printf("\n\nThis is message for me: %s\n", one_token.msg);

    memset(one_token.dst, 0, 100);
    memset(one_token.msg, 0, 100);

    one_token.type_of_message = RETURN;
}

void error_exit(char *error_message) {
    perror(error_message);
    exit(EXIT_FAILURE);
}

void tcp_send() {

    if (write(out_socket, &one_token, sizeof(one_token)) != sizeof(one_token)) {
        printf("ERROR during message send");
        exit(1);
    }

    struct sockaddr_in in_address;
    bzero(&in_address, sizeof(in_address));
    in_address.sin_family = AF_INET;
    in_address.sin_addr.s_addr = neigh_ip;
    in_address.sin_port = htons((uint16_t) in_port);
    printf("\nSend message to %s %d\n", inet_ntoa(in_address.sin_addr), out_port);
}

struct sockaddr_in tcp_receive(int socket) {
    struct sockaddr_in address;
    int len = sizeof(address);
    if (read(socket, &one_token, sizeof(one_token)) != sizeof(one_token)){
        remove_socket(socket);
        address.sin_port = 0;
        return address;
    }

    getpeername(socket, (struct sockaddr *) &address, (socklen_t *) &len);

    if (one_token.type_of_message == FREE || one_token.type_of_message == FULL || one_token.type_of_message == RETURN)
        fprintf(stderr, "\nReceived token: %d from %s %d\n", one_token.cnt, inet_ntoa(address.sin_addr),
                ntohs(address.sin_port));
    return address;
}

void _exit_() {


    if(close(monitor_fd) == -1){
        printf("ERROR during closing epoll");
        exit(1);
    }

    if (shutdown(out_socket, SHUT_RDWR) == -1) {
        printf("ERROR during shutdown out socket");
        exit(1);
    }

    if (close(out_socket) == -1) {
        printf("ERROR during closing out socket");
        exit(1);
    }

    if (close(self_socket) == -1) {
        printf("ERROR closing in socket failure");
        exit(1);
    }


    printf("\nGood Bye!\n");
    exit(0);
}

void remove_socket(int socket){
    if(epoll_ctl(monitor_fd, EPOLL_CTL_DEL, socket, NULL) == -1){
        printf("ERROR monitor failure");
        exit(1);
    }
    if(shutdown(socket, SHUT_RDWR) == -1){
        printf("ERROR during socket shutdown");
        exit(1);
    }
    if(close(socket) == -1){
        printf("ERROR socket close failure");
        exit(1);
    }
}
