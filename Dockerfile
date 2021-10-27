ARG BASE_IMAGE=alpine:3.14.2

FROM $BASE_IMAGE
RUN apk add npm bash perl openjdk11
WORKDIR /workdir
COPY package*.json ./
RUN npm install
ENV PATH /workdir/node_modules/cloc/lib:$PATH:$PATH
COPY docs /docs
RUN adduser --uid 2004 --disabled-password --gecos "" docker
COPY target/universal/stage/ /workdir/
RUN chmod +x /workdir/bin/codacy-metrics-cloc
USER docker
ENTRYPOINT ["bin/codacy-metrics-cloc"]
