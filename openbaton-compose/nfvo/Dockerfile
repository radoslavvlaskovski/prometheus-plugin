FROM openbaton/nfvo:latest

COPY application.properties /etc/openbaton/openbaton-nfvo.properties

ENTRYPOINT ["java", "-jar", "/nfvo.jar", "--spring.config.location=file:/etc/openbaton/openbaton-nfvo.properties"]
