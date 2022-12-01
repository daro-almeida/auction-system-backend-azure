FROM docker.io/tomcat:10-jdk17-corretto
WORKDIR /usr/local/tomcat/webapps
ENV JAVA_OPTS="-ea"
COPY modules/backend-app/target/scc-backend-app-1.0-SNAPSHOT.war ROOT.war
EXPOSE 8080
