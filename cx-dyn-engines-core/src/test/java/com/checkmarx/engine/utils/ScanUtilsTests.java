package com.checkmarx.engine.utils;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.checkmarx.engine.rest.model.ProgramLanguage;
import com.checkmarx.engine.rest.model.Project;
import com.checkmarx.engine.rest.model.ScanRequest;
import com.checkmarx.engine.rest.model.Stage;
import com.google.common.collect.Lists;

public class ScanUtilsTests {

    private static final Logger log = LoggerFactory.getLogger(ScanUtilsTests.class);

    @Test
    public void testSortQueue() {
        List<ScanRequest> queue = Lists.newArrayList();

        queue.add(createRequest(10, "01/01/2019 00:00:12", "01/01/2019 00:00:12"));
        queue.add(createRequest(9, "01/01/2019 00:00:09", "01/01/2019 00:00:10"));
        queue.add(createRequest(8, "01/01/2019 00:00:08", "01/01/2019 00:00:10"));
        queue.add(createRequest(7, "01/01/2019 00:00:06", "01/01/2019 00:00:07"));
        queue.add(createRequest(6, "01/01/2019 00:00:05", "01/01/2019 00:00:06"));
        queue.add(createRequest(5, null, "01/01/2019 00:00:04"));
        queue.add(createRequest(4, "01/01/2019 00:00:03", null));
        queue.add(createRequest(3, null, "01/01/2019 00:00:02"));
        queue.add(createRequest(2, "01/01/2019 00:00:01", null));
        queue.add(createRequest(1, null, null));

        log.debug("Unsorted: ");
        print(queue);

        ScanUtils.sortQueue(queue);

        log.debug("Sorted: ");
        print(queue);
    }

    private void print(List<ScanRequest> queue) {
        for (ScanRequest scan : queue) {
            log.debug("{}", scan.toString());
        }
    }

    private ScanRequest createRequest(int num, String createdOn, String queuedOn) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
        DateTime dateCreated = createdOn==null ? null : formatter.parseDateTime(createdOn);
        DateTime dateQueued = queuedOn==null ? null : formatter.parseDateTime(queuedOn);

        Project project = new Project(num, "Project"+num);
        Stage stage = new Stage(num, "Queued");
        ProgramLanguage[] languages = new ProgramLanguage[0];

        return new ScanRequest(num, "runId"+num, "team"+num, project, stage,
                10000+num, false, true, "Portal", languages, dateCreated, dateQueued, null);
    }

}