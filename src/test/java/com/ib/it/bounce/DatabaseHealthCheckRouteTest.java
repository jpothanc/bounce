package com.ib.it.bounce;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@CamelSpringBootTest
@SpringBootTest
public class DatabaseHealthCheckRouteTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    private AtomicBoolean emailSent;

    @BeforeEach
    public void setup() throws Exception {
        emailSent = new AtomicBoolean(false);

        // Modify the route to use a mock email endpoint
        AdviceWith.adviceWith(camelContext, "check-mysql-health", routeBuilder -> {
            routeBuilder.replaceFromWith("direct:testCheckDatabase");
            routeBuilder.weaveByToUri("direct:sendEmailToDev").replace().to("mock:emailDev");
            routeBuilder.weaveByToUri("direct:sendEmailToDBSupport").replace().to("mock:emailDB");
            routeBuilder.weaveByToUri("direct:sendRecoveryEmail").replace().to("mock:emailRecovery");
        });
    }

    @Test
    public void testMySQLUp_NoEmailSent() throws Exception {
        MockEndpoint emailDev = camelContext.getEndpoint("mock:emailDev", MockEndpoint.class);
        MockEndpoint emailDB = camelContext.getEndpoint("mock:emailDB", MockEndpoint.class);
        emailDev.expectedMessageCount(0);
        emailDB.expectedMessageCount(0);

        // Simulate MySQL being UP
        producerTemplate.sendBody("direct:testCheckDatabase", null);

        emailDev.assertIsSatisfied();
        emailDB.assertIsSatisfied();
    }

    @Test
    public void testMySQLFails_FirstEmailSent() throws Exception {
        MockEndpoint emailDB = camelContext.getEndpoint("mock:emailDB", MockEndpoint.class);
        emailDB.expectedMessageCount(1);

        // Simulate MySQL failure
        Exchange exchange = producerTemplate.request("direct:testCheckDatabase", e -> {
            e.setProperty(Exchange.EXCEPTION_CAUGHT, new RuntimeException("Connection refused"));
        });

        emailDB.assertIsSatisfied();
        assertTrue(emailSent.get(), "Email should have been sent on first failure.");
    }

    @Test
    public void testMySQLRemainsDown_NoDuplicateEmails() throws Exception {
        MockEndpoint emailDB = camelContext.getEndpoint("mock:emailDB", MockEndpoint.class);
        emailDB.expectedMessageCount(1); // Only first failure sends an email

        // Simulate MySQL failure twice
        producerTemplate.request("direct:testCheckDatabase", e -> {
            e.setProperty(Exchange.EXCEPTION_CAUGHT, new RuntimeException("Connection refused"));
        });

        producerTemplate.request("direct:testCheckDatabase", e -> {
            e.setProperty(Exchange.EXCEPTION_CAUGHT, new RuntimeException("Connection refused"));
        });

        emailDB.assertIsSatisfied();
    }

    @Test
    public void testMySQLRecovers_RecoveryEmailSent() throws Exception {
        MockEndpoint emailDB = camelContext.getEndpoint("mock:emailDB", MockEndpoint.class);
        MockEndpoint emailRecovery = camelContext.getEndpoint("mock:emailRecovery", MockEndpoint.class);

        emailDB.expectedMessageCount(1);
        emailRecovery.expectedMessageCount(1);

        // Simulate MySQL failure
        producerTemplate.request("direct:testCheckDatabase", e -> {
            e.setProperty(Exchange.EXCEPTION_CAUGHT, new RuntimeException("Connection refused"));
        });

        // Simulate MySQL recovery
        producerTemplate.sendBody("direct:testCheckDatabase", null);

        emailDB.assertIsSatisfied();
        emailRecovery.assertIsSatisfied();
        assertFalse(emailSent.get(), "Email flag should reset after recovery.");
    }
}
