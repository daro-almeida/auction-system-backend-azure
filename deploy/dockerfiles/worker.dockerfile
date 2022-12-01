FROM docker.io/amazoncorretto:19
WORKDIR /usr/local/app
ENV JAVA_OPTS="-ea"
COPY modules/worker-assembly/target/scc-worker-assembly-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar
ENTRYPOINT ["java", "-cp", "app.jar"]