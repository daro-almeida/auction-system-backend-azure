FROM artilleryio/artillery
WORKDIR /usr/local/tester
ARG yml
COPY artillery.sh artillery.sh
ADD testing/artillery/ testing/artillery/
ENTRYPOINT ["./artillery.sh", "$yml"]