/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.perezom.solrkeywordenricher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author Omar
 */
class KeywordsClient {

    private SolrDocumentList documents;
    private final ConcurrentUpdateSolrClient solrClient;
    private final List<Callable<SolrDocument>> callables;
    private final ExecutorService executor;
    private static final Logger LOG = Logger.getLogger(KeywordsClient.class.getName());
    private static final String KEYWORD_SERVICE_URI = "http://s-cnect-doris-t.cnect.cec.eu.int:3000/keywords";

    public KeywordsClient() {
        this.documents = new SolrDocumentList();
        this.solrClient = new ConcurrentUpdateSolrClient("http://localhost:8983/solr/fet-h2020-ria-noHPC-1", 32, 8);
        this.callables = new ArrayList<>();
        this.executor = Executors.newWorkStealingPool();
    }

    public Response.ResponseBuilder process() {
        getDocumentsFromSolr();
        prepareTasks();
        executeTasks();
        return Response.ok();
    }

    private void getDocumentsFromSolr() {
        LOG.info("Getting Solr documents from localhost...");
        SolrQuery query = new SolrQuery();
        query.setRequestHandler("/select");
        query.set("q", "*:*");
        query.set("rows", 4000);
        try {
            this.documents = this.solrClient.query(query).getResults();
            LOG.log(Level.INFO, "Request succeeded. Documents retrieved: {0} documents", this.documents.size());

        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(KeywordsClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void prepareTasks() {
        LOG.info("Preparing tasks...");
        this.documents.stream()
                .forEach(doc -> {
                    this.callables.add(prepareCallable(doc));
                });
        LOG.info("Tasks ready to be executed.");
    }

    private Callable<SolrDocument> prepareCallable(SolrDocument doc) {
        Callable<SolrDocument> task = () -> {

            Client client = ClientBuilder.newClient();

            Response response = client.target(KEYWORD_SERVICE_URI)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(new KeywordsEntity((String) doc.get("projectAbstract")), MediaType.APPLICATION_JSON));

            KeywordsResponseEntity responseEntity = response.readEntity(KeywordsResponseEntity.class);

            //client.close();

            List<String> keywords = new ArrayList<>();
            responseEntity.getKeywords()
                    .stream()
                    .forEach(keywordObject -> {
                        keywords.add(keywordObject.getLemmatized());
                    });

            doc.addField("keywords_ss", keywords);

            return doc;
        };
        return task;
    }

    private void executeTasks() {
        LOG.info("Executing tasks...");
        try {
            executor.invokeAll(callables)
                    .stream()
                    .parallel()
                    .map((Future<SolrDocument> future) -> {
                        try {
                            return future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new IllegalStateException(e);
                        }
                    }).forEach((SolrDocument doc) -> {
                        LOG.log(Level.INFO, "Executing task on thread {0}", Thread.currentThread().getName());
                        try {
                            SolrInputDocument inputDoc = new SolrInputDocument();
                            doc.getFieldNames().stream().forEach((name) -> {
                                inputDoc.addField(name, doc.getFieldValue(name));
                            });
                            this.solrClient.add(inputDoc);
                            this.solrClient.commit();
                         LOG.info("Executed task successfully!!");
                        } catch (SolrServerException | IOException ex) {
                            Logger.getLogger(KeywordsClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
            });
        } catch (InterruptedException ex) {
            Logger.getLogger(KeywordsClient.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex.getMessage());
        }

    }

}
