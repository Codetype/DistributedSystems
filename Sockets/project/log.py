from datetime import datetime
import socket
import struct

multicast_address = '224.1.1.1'
multicast_port = 9999

#multicast socket with set socket level to reuse
logger_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, 0)
logger_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
logger_socket.bind((multicast_address, multicast_port))

#pack multicast adreess and join to multicast group
ip_mreqn = struct.pack('4sl', socket.inet_aton(multicast_address), socket.INADDR_ANY)
logger_socket.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, ip_mreqn)

print("Start python logger. Logs caught in multicast:")
logtxt = open("log.txt","w+")

while True:
    timestamp = datetime.now()
    data, addr = logger_socket.recvfrom(64)
    curr_log = str(timestamp) + ' ' + data.decode('utf-8') + '\n'
    logtxt.write(curr_log)
    print(curr_log)
