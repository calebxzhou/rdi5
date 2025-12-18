docker build --build-arg HTTP_PROXY=http://192.168.1.20:10808 --build-arg HTTPS_PROXY=http://192.168.1.20:10808 --build-arg  NO_PROXY=127.0.0.1,localhost -t rdi:1.21.1_neoforge .
@pause