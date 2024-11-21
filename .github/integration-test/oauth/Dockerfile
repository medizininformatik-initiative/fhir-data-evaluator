# Image is used as base for a dummy container from which curl can request data within the same network as the other containers
FROM alpine
RUN apk add --no-cache ca-certificates
RUN apk add --no-cache curl
RUN mkdir -p /usr/local/share/ca-certificates/
ENTRYPOINT ["tail", "-f", "/dev/null"]
