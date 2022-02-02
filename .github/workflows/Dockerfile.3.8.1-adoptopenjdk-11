FROM maven:3.8.1-adoptopenjdk-11

ADD . /usr/src/mymaven

ADD ./repository $MAVEN_CONFIG/repository

RUN cd /usr/src/mymaven && \
    mvn -v && \
# Command after || really just adds some debug output to help troubleshooting
    mvn -X -B -f sniffy-compatibility-tests/pom.xml -P qemu test || { find sniffy-compatibility-tests -type f ; cat sniffy-compatibility-tests/*/target/surefire-reports/* ; exit 1; }
# TODO: why don't we run the whole build inside docker ?
#    mvn -B -f sniffy-compatibility-tests/pom.xml -P qemu test || mvn -B -f pom.xml -P qemu test