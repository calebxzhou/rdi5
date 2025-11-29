
FROM alpine/java:21-jdk
RUN apk add --no-cache tzdata bash util-linux su-exec fuse-overlayfs fuse3 shadow-uidmap
ENV TZ=Asia/Shanghai
RUN echo "LC_ALL=en_US.UTF-8" >> /etc/environment
RUN echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen
RUN echo "LANG=en_US.UTF-8" > /etc/locale.conf

#rdi mount point
# modpack - /mnt/modpack
# libraries - /mnt/lib
# tmpfs overlay layers - /mnt/overlay-{upper,work} each 64MB
RUN mkdir -p /mnt/modpack /mnt/lib /mnt/overlay/upper /mnt/overlay/work /opt/server

RUN addgroup -S rdi \
    && adduser -S -G rdi -u 1000 -h /home/rdi -s /bin/bash rdi \
    && mkdir -p /home/rdi

RUN chown -R rdi:rdi /home/rdi /mnt/overlay /opt/server

COPY start-server.sh /opt/server/start-server.sh

# Keep default working directory outside the overlay mount target.
WORKDIR /

# Configure Docker to send SIGTERM for graceful shutdown
STOPSIGNAL SIGTERM

# Mount the overlay and start the server via helper script.
RUN chmod +x /opt/server/start-server.sh
ENTRYPOINT ["/opt/server/start-server.sh"]
