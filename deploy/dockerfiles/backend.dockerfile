FROM docker.io/tomcat:10-jdk17-corretto
WORKDIR /usr/local/tomcat/webapps
RUN curl https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.20.2/opentelemetry-javaagent.jar -L --output opentelemetry-javaagent.jar
ENV JAVA_OPTS="-ea -javaagent:opentelemetry-javaagent.jar"
COPY modules/backend-app/target/scc-backend-app-1.0-SNAPSHOT.war ROOT.war
EXPOSE 8080
