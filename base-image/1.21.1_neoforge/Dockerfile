
FROM alpine/java:21-jdk
RUN apk add --no-cache tzdata bash su-exec
ENV TZ=Asia/Shanghai
RUN echo "LC_ALL=en_US.UTF-8" >> /etc/environment
RUN echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen
RUN echo "LANG=en_US.UTF-8" > /etc/locale.conf
#rdi mount point
# modpack - /mnt/modpack
# libraries - /mnt/lib
# tmpfs overlay layers - /mnt/overlay-{upper,work} each 64MB
RUN mkdir -p /opt/server /data

RUN addgroup -S rdi \
    && adduser -S -G rdi -u 1000 -h /home/rdi -s /bin/bash rdi \
    && mkdir -p /home/rdi

COPY start-server.sh /usr/local/bin/start-server.sh
RUN chmod +x /usr/local/bin/start-server.sh \
    && chown -R rdi:rdi /home/rdi /opt/server /data



# Keep default working directory outside the overlay mount target.
WORKDIR /

# Configure Docker to send SIGTERM for graceful shutdown
STOPSIGNAL SIGTERM
 
ENTRYPOINT ["/usr/local/bin/start-server.sh"]
