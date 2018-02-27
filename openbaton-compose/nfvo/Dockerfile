FROM openbaton/nfvo:5.2.0

COPY application.properties /etc/openbaton/openbaton-nfvo.properties

ENTRYPOINT ["java", "-jar", "/nfvo.jar", "--spring.config.location=file:/etc/openbaton/openbaton-nfvo.properties"]
