FROM ubuntu

USER root
WORKDIR /root

COPY ENTRYPOINT.sh /
COPY nstopo.py /root
COPY nstopo /root
COPY goldeneye /root
COPY res /root
COPY util /root

RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    iproute2 \
    iputils-ping \
    openvswitch-switch \
    mininet \
    net-tools \
    tcpdump \
    vim \
    x11-xserver-utils \
    xterm \ 
    git \
 && rm -rf /var/lib/apt/lists/* \
 && chmod +x /ENTRYPOINT.sh \
 && chmod +x /root/nstopo \
 && chmod +x /root/goldeneye


EXPOSE 6633 6653 6640

ENTRYPOINT ["/ENTRYPOINT.sh"]