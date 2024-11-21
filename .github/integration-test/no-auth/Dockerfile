# Image is uses as base for a dummy container from which curl can request data within the same network as the other containers
FROM alpine
RUN apk add --no-cache curl
CMD ["sh", "-c", "while true; do sleep 3600; done"]
