FROM justb4/jmeter:5.5

USER root
RUN apk add --no-cache jq bc

COPY report.sh /opt/report.sh
RUN chmod +x /opt/report.sh

RUN mkdir -p /opt/jmeter-test
COPY DialogTestPlan.jmx /opt/jmeter-test/

RUN echo '#!/bin/sh' > /opt/entrypoint.sh && \
    echo 'export STORAGE_TYPE=${STORAGE_TYPE:-tarantool}' >> /opt/entrypoint.sh && \
    echo 'jmeter -n -t /opt/jmeter-test/DialogTestPlan.jmx -Jhost=${HOST:-z-social-network-hw7-app} -Jport=${PORT:-8085} -l /results/results.jtl -j /results/jmeter.log -e -o /results/dashboard -Jjmeter.save.saveservice.output_format=csv -f' >> /opt/entrypoint.sh && \
    echo 'sleep 2' >> /opt/entrypoint.sh && \
    echo '/opt/report.sh' >> /opt/entrypoint.sh && \
    chmod +x /opt/entrypoint.sh

ENTRYPOINT ["/opt/entrypoint.sh"]