FROM openbaton/driver-docker-go:latest

ADD config.toml .

ENTRYPOINT ./goapp -conf config.toml -level DEBUG -swarm

