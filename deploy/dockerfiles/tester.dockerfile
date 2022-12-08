FROM artilleryio/artillery
WORKDIR /usr/local/app
RUN npm install @faker-js/faker --save-dev
ADD testing/artillery/*.yml .
ADD testing/artillery/*.js .