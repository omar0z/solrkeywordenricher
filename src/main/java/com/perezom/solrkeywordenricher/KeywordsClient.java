/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.perezom.solrkeywordenricher;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.GenericSolrRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.json.simple.JSONObject;

/**
 * @author Omar
 */
class KeywordsClient {

    private SolrDocumentList documents;
    private String fieldName;
    private final ConcurrentUpdateSolrClient solrClient;
    private final List<SolrInputDocument> enrichedDocuments;
    private static final Logger LOG = Logger.getLogger(KeywordsClient.class.getName());
    private static final String KEYWORD_SERVICE_URI = "http://s-cnect-drive-in-d.cnect.cec.eu.int/api/service/keyword-extraction-legacy";

    KeywordsClient(String coreName, String fieldName) {
        this.fieldName = fieldName;
        this.documents = new SolrDocumentList();
        this.solrClient = new ConcurrentUpdateSolrClient.Builder("http://localhost:8983/solr/" + coreName)
                .withQueueSize(32)
                .withThreadCount(8).build();
        this.enrichedDocuments = new ArrayList();
    }

    public Response.ResponseBuilder process() {
        getDocumentsFromSolr();
        prepareDocs();
        sendToSolr();
        return Response.ok();
    }

    private void getDocumentsFromSolr() {
        LOG.info("Getting Solr documents from localhost...");
        SolrQuery query = new SolrQuery();
        query.setRequestHandler("/select");
        query.set("q", "*:*");
        query.set("rows", 15000);
        try {
            QueryRequest request = new QueryRequest(query);
            request.setBasicAuthCredentials("doris", "@Doris1");
            this.documents = request.process(this.solrClient).getResults();
            LOG.log(Level.INFO, "Request succeeded. Documents retrieved: {0} documents", this.documents.size());

        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(KeywordsClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void prepareDocs() {
        LOG.info("Preparing tasks...");

        Client client = ClientBuilder.newClient();
        for (SolrDocument doc : this.documents) {
            try {
                this.enrichedDocuments.add(enrichDocument(doc, client));
            } catch (IOException ex) {
                Logger.getLogger(KeywordsClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        LOG.info("Tasks ready to be executed.");
    }

    private SolrInputDocument enrichDocument(SolrDocument doc, Client client) throws IOException {
        LOG.info("Enriching document...");

        MultivaluedMap<String, String> parameters = new MultivaluedHashMap<>();
        parameters.add("text", (String) doc.get(this.fieldName));
        parameters.add("DORIS_API_KEY", "59b252b6511e69032defce27");

        Form form = new Form(parameters);
        Response response = client.target(KEYWORD_SERVICE_URI)
                .property(ClientProperties.PROXY_URI, "http://158.169.131.13")
                .property(ClientProperties.PROXY_USERNAME, "perezom")
                .property(ClientProperties.PROXY_PASSWORD, "Scoutpower1")
                .request(MediaType.APPLICATION_FORM_URLENCODED)
                .post(Entity.form(form));

        Gson gson = new Gson();
        KeywordsResponseEntity responseEntity = gson.fromJson(response.readEntity(String.class), KeywordsResponseEntity.class);

        List<String> keywords = new ArrayList<>();
        LOG.info(responseEntity.getKeywords().toString());
        if (responseEntity == null || responseEntity.getKeywords() == null || responseEntity.getKeywords().isEmpty()) {
            return convertToInputDoc(doc, new ArrayList<>());
        }
        responseEntity.getKeywords()
                .stream()
                .forEach((Keyword keywordObject) -> {
                    keywords.add(keywordObject.getLemmatized());
                });

        return convertToInputDoc(doc, keywords);

    }

    private SolrInputDocument convertToInputDoc(SolrDocument doc, List<String> keywords) {
        SolrInputDocument inputDoc = new SolrInputDocument();
        doc.getFieldNames().stream().forEach((name) -> {
            inputDoc.addField(name, doc.getFieldValue(name));
        });

        inputDoc.addField("keywords_ss", keywords);

        return inputDoc;
    }

    private void sendToSolr() {
        LOG.info("Executing tasks...");
        try {
            for (SolrInputDocument doc : this.enrichedDocuments) {
                LOG.log(Level.INFO, "Executing task on thread {0}", Thread.currentThread().getName());
                LOG.info(doc.get("keywords_ss").toString());
                UpdateRequest request = new UpdateRequest();
                request.setBasicAuthCredentials("doris", "@Doris1");
                request.add(doc);
                request.setAction(AbstractUpdateRequest.ACTION.COMMIT, false, true, true);
                request.process(this.solrClient);

                LOG.info("Executed task successfully!!");
            }
            //this.solrClient.commit();
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(KeywordsClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

