all:
	build

build:
	gcc -o udp_client udp_client.c
	gcc -o tcp_client client_tcp.c

logger:
	python communication_logger.py

udpclient1:
	./udp_client client1 127.0.0.1 8001 8001 0

udpclient2:
	./udp_client client2 127.0.0.1 8001 8002 1

udpclient3:
	./udp_client client3 127.0.0.1 8001 8003 0

tcpclient1:
	./tcp_client client1 127.0.0.1 8001 8001 0

tcpclient2:
	./tcp_client client2 127.0.0.1 8001 8002 1

tcpclient3:
	./tcp_client client3 127.0.0.1 8001 8003 0

clean:
	rm -f *.o *.a main tcp_client udp_client
