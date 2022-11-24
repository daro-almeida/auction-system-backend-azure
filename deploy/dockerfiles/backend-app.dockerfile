FROM docker.io/tomcat:10-jdk17-corretto
WORKDIR /usr/local/tomcat/webapps
COPY modules/backend-app/target/scc-backend-app-1.0-SNAPSHOT.war ROOT.war
EXPOSE 8080
