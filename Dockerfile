FROM kbase/sdkbase2 as build

WORKDIR /tmp/ah

# dependencies take a while to D/L, so D/L & cache before the build so code changes don't cause
# a new D/L
# can't glob *gradle because of the .gradle dir
COPY build.gradle gradlew settings.gradle /tmp/ah/
COPY gradle/ /tmp/ah/gradle/
RUN ./gradlew dependencies

# Now build the code
COPY deployment/ /tmp/ah/deployment/
COPY jettybase/ /tmp/ah/jettybase/
COPY src /tmp/ah/src/
COPY war /tmp/ah/war/
# for the git commit
COPY .git /tmp/ah/.git/
RUN ./gradlew war


FROM kbase/kb_jre:latest

# These ARGs values are passed in via the docker build command
ARG BUILD_DATE
ARG VCS_REF
ARG BRANCH=develop

COPY --from=build /tmp/ah/deployment/ /kb/deployment/
COPY --from=build /tmp/ah/jettybase/ /kb/deployment/jettybase/
COPY --from=build /tmp/ah/build/libs/AssemblyHomologyService.war /kb/deployment/jettybase/webapps/ROOT.war

# The BUILD_DATE value seem to bust the docker cache when the timestamp changes, move to
# the end
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.vcs-url="https://github.com/jgi-kbase/AssemblyHomologyService.git" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.schema-version="1.0.0-rc1" \
      us.kbase.vcs-branch=$BRANCH \
      maintainer="Steve Chan sychan@lbl.gov"

WORKDIR /kb/deployment/jettybase
ENV ASSEMBLY_HOMOLOGY_CONFIG=/kb/deployment/conf/deployment.cfg
ENV PATH=/bin:/usr/bin:/kb/deployment/bin
ENV JETTY_HOME=/usr/local/jetty

# TODO BUILD update to no longer use dockerize and take env vars (e.g. like Collections).
# TODO BUILD Use subsections in the ini file / switch to TOML

RUN wget https://github.com/marbl/Mash/releases/download/v2.0/mash-Linux64-v2.0.tar && \
    tar xf mash-Linux64-v2.0.tar && \
    cp mash-Linux64-v2.0/mash /usr/bin/mash

RUN chmod -R a+rwx /kb/deployment/conf /kb/deployment/jettybase/
ENTRYPOINT [ "/kb/deployment/bin/dockerize" ]

# Here are some default params passed to dockerize. They would typically
# be overidden by docker-compose at startup
CMD [  "-template", "/kb/deployment/conf/.templates/deployment.cfg.templ:/kb/deployment/conf/deployment.cfg", \
       "java", "-Djetty.home=/usr/local/jetty", "-jar", "/usr/local/jetty/start.jar" ]
