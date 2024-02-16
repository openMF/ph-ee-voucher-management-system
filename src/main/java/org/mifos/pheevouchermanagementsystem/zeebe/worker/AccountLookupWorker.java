package org.mifos.pheevouchermanagementsystem.zeebe.worker;

import static org.mifos.connector.common.zeebe.ZeebeVariables.TRANSACTION_ID;
import static org.mifos.pheevouchermanagementsystem.zeebe.ZeebeVariables.CACHED_TRANSACTION_ID;
import static org.mifos.pheevouchermanagementsystem.zeebe.ZeebeVariables.CALLBACK;
import static org.mifos.pheevouchermanagementsystem.zeebe.ZeebeVariables.HOST;
import static org.mifos.pheevouchermanagementsystem.zeebe.ZeebeVariables.INITIATOR_FSP_ID;
import static org.mifos.pheevouchermanagementsystem.zeebe.ZeebeVariables.PAYEE_IDENTITY;
import static org.mifos.pheevouchermanagementsystem.zeebe.ZeebeVariables.PAYER_IDENTIFIER;
import static org.mifos.pheevouchermanagementsystem.zeebe.ZeebeVariables.PAYER_IDENTIFIER_TYPE;
import static org.mifos.pheevouchermanagementsystem.zeebe.ZeebeVariables.PAYMENT_MODALITY;
import static org.mifos.pheevouchermanagementsystem.zeebe.ZeebeVariables.REGISTERING_INSTITUTION_ID;
import static org.mifos.pheevouchermanagementsystem.zeebe.ZeebeVariables.REQUEST_ID;
import static org.mifos.pheevouchermanagementsystem.zeebe.worker.Worker.ACCOUNT_LOOKUP;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AccountLookupWorker extends BaseWorker {

    @Autowired
    private ZeebeClient zeebeClient;

    @Autowired
    private ProducerTemplate producerTemplate;
    @Autowired
    private CamelContext camelContext;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${identity-account-mapper.hostname}")
    private String identityMapperURL;
    @Value("${voucher.hostname}")
    private String voucherHostname;
    @Value("${payer.tenant}")
    private String payerTenant;
    @Value("${payer.identifier}")
    private String payerIdentifier;
    @Value("${payer.identifierType}")
    private String payerIdentifierType;
    @Value("${zeebe.client.evenly-allocated-max-jobs}")
    private int workerMaxJobs;
    @Value("${defaultPaymentModality}")
    private String paymentModality;
    private static final Logger logger = LoggerFactory.getLogger(AccountLookupWorker.class);

    @Override
    public void setup() {
        logger.info("## generating " + ACCOUNT_LOOKUP + "zeebe worker");
        zeebeClient.newWorker().jobType("payee-account-Lookup-voucher").handler((client, job) -> {
            logger.info("Job '{}' started from process '{}' with key {}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> existingVariables = job.getVariablesAsMap();
            existingVariables.put(CACHED_TRANSACTION_ID, job.getKey());
            existingVariables.put(PAYER_IDENTIFIER, payerIdentifier);
            existingVariables.put(PAYER_IDENTIFIER_TYPE, payerIdentifierType);

            existingVariables.put(INITIATOR_FSP_ID, payerTenant);
            existingVariables.put(REQUEST_ID, job.getKey());

            Exchange exchange = new DefaultExchange(camelContext);
            exchange.setProperty(HOST, identityMapperURL);
            exchange.setProperty(CALLBACK, identityMapperURL + "/accountLookupCallback");
            exchange.setProperty(TRANSACTION_ID, existingVariables.get(TRANSACTION_ID));
            exchange.setProperty(REQUEST_ID, job.getKey());
            exchange.setProperty(REGISTERING_INSTITUTION_ID, existingVariables.get("registeringInstitutionId").toString());
            exchange.setProperty(PAYEE_IDENTITY, existingVariables.get("payeeIdentity").toString());
            exchange.setProperty(PAYMENT_MODALITY, paymentModality);
            // exchange.setProperty("paymentModality", existingVariables.get("paymentModality").toString());
            producerTemplate.send("direct:send-account-lookup", exchange);

            client.newCompleteCommand(job.getKey()).variables(existingVariables).send();
        }).name("payee-account-Lookup-voucher").maxJobsActive(workerMaxJobs).open();

    }
}
