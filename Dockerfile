FROM openjdk:11

RUN mkdir -p /tmp/one-free/grupi
RUN chmod -R a+rwx /tmp/one-free/grupi

ARG JAR_FILE=target/grupi-file-stager*.jar
#COPY ${JAR_FILE} GrupiFileStager.jar

#ENTRYPOINT ["java","-jar","/GrupiFileStager.jar"]
