# Image is used as base for a dummy container from which curl can request data within the same network as the other containers
FROM alpine
RUN apk update && apk add bash
RUN apk add --no-cache ca-certificates
RUN apk add --no-cache curl
RUN apk add jq
RUN mkdir -p /usr/local/share/ca-certificates/
RUN mkdir -p /app/output/
ENTRYPOINT ["tail", "-f", "/dev/null"]
