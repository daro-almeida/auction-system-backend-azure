FROM docker.io/amazoncorreto:19
WORKDIR /usr/local/app
ENV JAVA_OPTS="-ea"
COPY modules/worker-user-delete/target/scc-worker-user-delete-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-cp", "app.jar", "scc.App"]