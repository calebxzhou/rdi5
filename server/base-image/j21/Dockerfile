
FROM eclipse-temurin:21-jdk
RUN apt-get update \
    && apt-get install -y --no-install-recommends tzdata bash gosu locales \
    && rm -rf /var/lib/apt/lists/*
ENV TZ=Asia/Shanghai
RUN echo "LC_ALL=en_US.UTF-8" >> /etc/environment \
    && echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen \
    && echo "LANG=en_US.UTF-8" > /etc/locale.conf \
    && locale-gen
#rdi mount point
# modpack - /mnt/modpack
# libraries - /mnt/lib
# tmpfs overlay layers - /mnt/overlay-{upper,work} each 64MB
RUN mkdir -p /opt/server /data
RUN mkdir -p /tmp && chmod 1777 /tmp
RUN groupadd -r rdi \
    && useradd -r -g rdi -m -d /home/rdi -s /bin/bash rdi

COPY start-server.sh /usr/local/bin/start-server.sh
RUN chmod +x /usr/local/bin/start-server.sh



# Keep default working directory outside the overlay mount target.
WORKDIR /

# Configure Docker to send SIGTERM for graceful shutdown
STOPSIGNAL SIGTERM
 
ENTRYPOINT ["/usr/local/bin/start-server.sh"]
