/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
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
import com.codenvy.analytics.Utils;
import com.codenvy.analytics.datamodel.LongValueData;
import com.codenvy.analytics.datamodel.SetValueData;
import com.codenvy.analytics.datamodel.StringValueData;
import com.codenvy.analytics.datamodel.ValueData;
import com.codenvy.analytics.metrics.*;
import com.codenvy.analytics.pig.PigServer;
import com.codenvy.analytics.pig.scripts.util.Event;
import com.codenvy.analytics.pig.scripts.util.LogGenerator;

import org.apache.pig.data.Tuple;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.testng.Assert.*;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
public class TestProductUsageFactorySessions extends BaseTest {

    private Map<String, String> params;

    @BeforeClass
    public void init() throws IOException {
        params = Utils.newContext();

        List<Event> events = new ArrayList<>();
        events.add(Event.Builder.createSessionFactoryStartedEvent("id1", "tmp-1", "user1", "true", "brType")
                        .withDate("2013-02-10").withTime("10:00:00").build());
        events.add(Event.Builder.createSessionFactoryStoppedEvent("id1", "tmp-1", "user1")
                        .withDate("2013-02-10").withTime("10:05:00").build());

        events.add(Event.Builder.createSessionFactoryStartedEvent("id2", "tmp-2", "user1", "true", "brType")
                        .withDate("2013-02-10").withTime("10:20:00").build());
        events.add(Event.Builder.createSessionFactoryStoppedEvent("id2", "tmp-2", "user1")
                        .withDate("2013-02-10").withTime("10:30:00").build());

        events.add(Event.Builder.createSessionFactoryStartedEvent("id3", "tmp-3", "anonymoususer_1", "false", "brType")
                        .withDate("2013-02-10").withTime("11:00:00").build());
        events.add(Event.Builder.createSessionFactoryStoppedEvent("id3", "tmp-3", "anonymoususer_1")
                        .withDate("2013-02-10").withTime("11:15:00").build());

        events.add(Event.Builder.createFactoryProjectImportedEvent("tmp-1", "user1", "project", "type")
                        .withDate("2013-02-10").withTime("10:05:00").build());

        events.add(
                Event.Builder.createFactoryUrlAcceptedEvent("tmp-1", "factoryUrl1", "referrer1", "org1", "affiliate1")
                     .withDate("2013-02-10").withTime("11:00:00").build());
        events.add(
                Event.Builder.createFactoryUrlAcceptedEvent("tmp-2", "factoryUrl1", "referrer2", "org2", "affiliate1")
                     .withDate("2013-02-10").withTime("11:00:01").build());
        events.add(
                Event.Builder.createFactoryUrlAcceptedEvent("tmp-3", "factoryUrl1", "referrer3", "org3", "affiliate2")
                     .withDate("2013-02-10").withTime("11:00:02").build());


        File log = LogGenerator.generateLog(events);

        Parameters.FROM_DATE.put(params, "20130210");
        Parameters.TO_DATE.put(params, "20130210");
        Parameters.USER.put(params, Parameters.USER_TYPES.ANY.name());
        Parameters.WS.put(params, Parameters.WS_TYPES.ANY.name());
        Parameters.STORAGE_TABLE.put(params, "testproductusagefactorysessions_acceptedfactories");
        Parameters.LOG.put(params, log.getAbsolutePath());
        PigServer.execute(ScriptType.ACCEPTED_FACTORIES, params);

        Parameters.WS.put(params, Parameters.WS_TYPES.TEMPORARY.name());
        Parameters.STORAGE_TABLE.put(params, "testproductusagefactorysessions");
        Parameters.STORAGE_TABLE_ACCEPTED_FACTORIES.put(params, "testproductusagefactorysessions_factories");
        PigServer.execute(ScriptType.PRODUCT_USAGE_FACTORY_SESSIONS, params);
    }

    @Test
    public void testExecute() throws Exception {
        Iterator<Tuple> iterator = PigServer.executeAndReturn(ScriptType.PRODUCT_USAGE_FACTORY_SESSIONS, params);

        assertTrue(iterator.hasNext());
        Tuple tuple = iterator.next();
        assertEquals(tuple.get(0), timeFormat.parse("20130210 10:00:00").getTime());
        assertEquals(tuple.get(1).toString(), "(time,300)");

        assertTrue(iterator.hasNext());
        tuple = iterator.next();
        assertEquals(tuple.get(0), timeFormat.parse("20130210 10:20:00").getTime());
        assertEquals(tuple.get(1).toString(), "(time,600)");

        assertTrue(iterator.hasNext());
        tuple = iterator.next();
        assertEquals(tuple.get(0), timeFormat.parse("20130210 11:00:00").getTime());
        assertEquals(tuple.get(1).toString(), "(time,900)");

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testSingleDateFilter() throws Exception {
        Map<String, String> context = Utils.newContext();
        Parameters.FROM_DATE.put(context, "20130210");
        Parameters.TO_DATE.put(context, "20130210");

        Metric metric = new TestFactorySessionsProductUsageTotal();
        assertEquals(metric.getValue(context), new LongValueData(30));

        metric = new TestFactorySessions();
        assertEquals(metric.getValue(context), new LongValueData(3));

        metric = new TestAuthenticatedFactorySessions();
        assertEquals(metric.getValue(context), new LongValueData(2));

        metric = new TestConvertedFactorySessions();
        assertEquals(metric.getValue(context), new LongValueData(1));
    }

    @Test
    public void testUserFilter() throws Exception {
        Map<String, String> context = Utils.newContext();
        Parameters.FROM_DATE.put(context, "20130210");
        Parameters.TO_DATE.put(context, "20130210");
        MetricFilter.REFERRER.put(context, "referrer1");

        Metric metric = new TestFactorySessionsProductUsageTotal();
        assertEquals(metric.getValue(context), new LongValueData(5));
    }

    @Test
    public void testAbstractFactorySessions() throws Exception {
        Map<String, String> context = Utils.newContext();
        Parameters.FROM_DATE.put(context, "20130210");
        Parameters.TO_DATE.put(context, "20130210");

        Metric metric = new TestAbstractFactorySessions("testproductusagefactorysessions", 0, 600, true, true);
        assertEquals(metric.getValue(context), new LongValueData(2));
    }

    @Test
    public void testShouldReturnAllTemporaryWorkspaces() throws Exception {
        Map<String, String> context = Utils.newContext();
        Parameters.FROM_DATE.put(context, "20130210");
        Parameters.TO_DATE.put(context, "20130210");

        Metric metric = new TestActiveTemporaryWorkspacesSet();
        SetValueData valueData = (SetValueData)metric.getValue(context);

        assertEquals(valueData.size(), 3);
        assertEquals(valueData, new SetValueData(Arrays.asList(new ValueData[]{
                new StringValueData("tmp-1"),
                new StringValueData("tmp-2"),
                new StringValueData("tmp-3")})));
    }

    @Test
    public void testShouldReturnAllTemporaryWorkspacesForSpecificOrgId() throws Exception {
        Map<String, String> context = Utils.newContext();
        Parameters.FROM_DATE.put(context, "20130210");
        Parameters.TO_DATE.put(context, "20130210");
        MetricFilter.ORG_ID.put(context, "org1");

        Metric metric = new TestActiveTemporaryWorkspacesSet();
        SetValueData valueData = (SetValueData)metric.getValue(context);

        assertEquals(valueData.size(), 1);
        assertEquals(valueData, new SetValueData(Arrays.asList(new ValueData[]{new StringValueData("tmp-1")})));
    }

    @Test
    public void testShouldReturnAllTemporaryWorkspacesForSpecificAffiliateId() throws Exception {
        Map<String, String> context = Utils.newContext();
        Parameters.FROM_DATE.put(context, "20130210");
        Parameters.TO_DATE.put(context, "20130210");
        MetricFilter.AFFILIATE_ID.put(context, "affiliate1");

        Metric metric = new TestActiveTemporaryWorkspacesSet();
        SetValueData valueData = (SetValueData)metric.getValue(context);

        assertEquals(valueData.size(), 2);
        assertEquals(valueData, new SetValueData(Arrays.asList(new ValueData[]{
                new StringValueData("tmp-1"),
                new StringValueData("tmp-2")})));
    }

    @Test
    public void testShouldReturnAllTemporaryWorkspacesForSpecificReferrer() throws Exception {
        Map<String, String> context = Utils.newContext();
        Parameters.FROM_DATE.put(context, "20130210");
        Parameters.TO_DATE.put(context, "20130210");
        MetricFilter.REFERRER.put(context, "referrer2");

        Metric metric = new TestActiveTemporaryWorkspacesSet();
        SetValueData valueData = (SetValueData)metric.getValue(context);

        assertEquals(valueData.size(), 1);
        assertEquals(valueData, new SetValueData(Arrays.asList(new ValueData[]{new StringValueData("tmp-2")})));
    }


    @Test
    public void testShouldReturnAllTemporaryWorkspacesForSpecificFactory() throws Exception {
        Map<String, String> context = Utils.newContext();
        Parameters.FROM_DATE.put(context, "20130210");
        Parameters.TO_DATE.put(context, "20130210");
        MetricFilter.FACTORY.put(context, "factoryUrl1");

        Metric metric = new TestActiveTemporaryWorkspacesSet();
        SetValueData valueData = (SetValueData)metric.getValue(context);

        assertEquals(valueData.size(), 3);
        assertEquals(valueData, new SetValueData(Arrays.asList(new ValueData[]{
                new StringValueData("tmp-1"),
                new StringValueData("tmp-2"),
                new StringValueData("tmp-3")})));
    }

    private class TestActiveTemporaryWorkspacesSet extends ActiveTemporaryWorkspacesSet {
        @Override
        public String getStorageTableBaseName() {
            return "testproductusagefactorysessions_acceptedfactories-raw";
        }
    }

    private class TestFactorySessions extends FactorySessions {
        @Override
        public String getStorageTableBaseName() {
            return "testproductusagefactorysessions";
        }
    }

    private class TestFactorySessionsProductUsageTotal extends FactorySessionsProductUsageTotal {

        @Override
        public String getStorageTableBaseName() {
            return "testproductusagefactorysessions";
        }
    }

    private class TestAuthenticatedFactorySessions extends AuthenticatedFactorySessions {
        @Override
        public String getStorageTableBaseName() {
            return "testproductusagefactorysessions-raw";
        }
    }

    private class TestConvertedFactorySessions extends ConvertedFactorySessions {
        @Override
        public String getStorageTableBaseName() {
            return "testproductusagefactorysessions-raw";
        }
    }


    private class TestAbstractFactorySessions extends AbstractFactorySessions {

        protected TestAbstractFactorySessions(String metricName, long min, long max, boolean includeMin,
                                              boolean includeMax) {
            super(metricName, min, max, includeMin, includeMax);
        }

        @Override
        public String getStorageTableBaseName() {
            return "testproductusagefactorysessions";
        }

        @Override
        public String getDescription() {
            return null;
        }
    }
}
