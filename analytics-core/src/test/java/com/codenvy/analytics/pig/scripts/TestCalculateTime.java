/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2014] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.analytics.pig.scripts;

import com.codenvy.analytics.BaseTest;
import com.codenvy.analytics.metrics.Context;
import com.codenvy.analytics.metrics.Parameters;
import com.codenvy.analytics.pig.scripts.util.Event;
import com.codenvy.analytics.pig.scripts.util.LogGenerator;

import org.apache.pig.data.Tuple;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Alexander Reshetnyak
 */
public class TestCalculateTime extends BaseTest {

    private Context context;

    @BeforeClass
    public void prepare() throws Exception {
        List<Event> events = new ArrayList<>();

        // 6m
        events.add(
                Event.Builder.createRunStartedEvent("user1@gmail.com", "ws1", "project", "type", "")
                             .withDate("2013-01-01")
                             .withTime("19:00:00").build());
        events.add(
                Event.Builder.createRunFinishedEvent("user1@gmail.com", "ws1", "project", "type", "")
                             .withDate("2013-01-01")
                             .withTime("19:06:00").build());

        // failed session, there is no 'run-finished' event
        events.add(
                Event.Builder.createRunStartedEvent("user1@gmail.com", "ws1", "project", "type", "id2")
                             .withDate("2013-01-01")
                             .withTime("19:07:00").build());

        // 2m
        events.add(
                Event.Builder.createRunStartedEvent("user2@gmail.com", "ws1", "project", "type", "id3")
                             .withDate("2013-01-01")
                             .withTime("19:08:00").build());
        events.add(
                Event.Builder.createRunFinishedEvent("user2@gmail.com", "ws1", "project", "type", "id3")
                             .withDate("2013-01-01")
                             .withTime("19:10:00").build());

        // 1m
        events.add(
                Event.Builder.createRunStartedEvent("user1@gmail.com", "ws1", "project", "type", "id4")
                             .withDate("2013-01-01")
                             .withTime("19:11:00").build());
        events.add(
                Event.Builder.createRunFinishedEvent("user1@gmail.com", "ws1", "project", "type", "id4")
                             .withDate("2013-01-01")
                             .withTime("19:12:00").build());

        // failed session, there is no 'run-started' event
        events.add(
                Event.Builder.createRunFinishedEvent("user1@gmail.com", "ws1", "project", "type", "id5")
                             .withDate("2013-01-01")
                             .withTime("19:13:00").build());


        File log = LogGenerator.generateLog(events);

        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20130101");
        builder.put(Parameters.TO_DATE, "20130101");
        builder.put(Parameters.USER, Parameters.USER_TYPES.ANY.name());
        builder.put(Parameters.WS, Parameters.WS_TYPES.ANY.name());
        builder.put(Parameters.STORAGE_TABLE, "fake");
        builder.put(Parameters.LOG, log.getAbsolutePath());
        context = builder.build();
    }

    @Test
    public void testExecuteTestScript() throws Exception {
        Set<String> actual = new HashSet<>();

        Iterator<Tuple> iterator = pigServer.executeAndReturn(ScriptType.TEST_CALCULATE_TIME, context);
        while (iterator.hasNext()) {
            actual.add(iterator.next().toString());
        }

        Set<String> expected = new HashSet<>();
        expected.add("(user1@gmail.com,60000)");
        expected.add("(user2@gmail.com,120000)");
        expected.add("(user1@gmail.com,360000)");

        assertEquals(expected, actual);
    }
}

