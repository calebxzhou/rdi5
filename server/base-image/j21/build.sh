docker build \
 --build-arg HTTP_PROXY="http://192.168.1.88:10808" \
 --build-arg HTTPS_PROXY="http://192.168.1.88:10808" \
 --build-arg NO_PROXY="127.0.0.1,localhost" \
 -t rdi:j21 .
