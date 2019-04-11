#include "udp_utils.h"
#include "tcp_utils.h"

#define TCP 0
#define UDP 1

//program arguments
char *client_id;
in_addr_t neigh_ip;
int out_port;
int in_port;
int protocol;

void parse_args(int argc, char** argv){
    if(argc < 6){
        printf("ERROR Invalid number of arguments!\n");
        printf("USAGE: <prog_name> <client_id> <neighbour_ip_address> <neighbour_port> <self_port> <protocol> <token_value>\n");
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

    strcmp(argv[5], "tcp") == 0 ? protocol = TCP : protocol = UDP;

    tmp = argv[6];
    if(strtol(tmp, &tmp, 10) == 0) present_token = 0;
    else present_token = 1;
    if(present_token){
        one_token.cnt = 0;
        one_token.port = 0;
        one_token.address = 0;
    }

}

int main(int argc, char** argv) {
    return 0;
}