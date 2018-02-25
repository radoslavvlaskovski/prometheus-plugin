FROM openbaton/vnfm-docker-go

ADD config.toml .

ENTRYPOINT ./goapp -conf config.toml -swarm true
