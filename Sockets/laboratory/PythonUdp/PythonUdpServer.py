import socket;

serverPort = 9009
serverSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
serverSocket.bind(('', serverPort))
buff = []

print('PYTHON UDP SERVER')

while True:

    buff, address = serverSocket.recvfrom(1024)
    print("Client: " + str(buff, 'utf-8'))
    if(buff == "Java"):
        msg = "Hello Java Client!"
        serverSocket.send(bytes(msg,'utf-8'), (address, serverPort))
        
    



