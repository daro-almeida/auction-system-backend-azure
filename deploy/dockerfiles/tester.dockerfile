FROM docker.io/artilleryio/artillery
WORKDIR /usr/local/app
RUN npm install @faker-js/faker --save-dev
ADD tester/testing/artillery/*.yml .
ADD tester/testing/artillery/*.js .
COPY tester/artillery-entrypoint.sh .
ENTRYPOINT ["/bin/sh", "./artillery-entrypoint.sh"]