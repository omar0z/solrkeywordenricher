/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.perezom.solrkeywordenricher;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Omar
 */
class PartBClient {

    private SolrDocumentList documents, proposalDocuments;
    private HttpSolrClient solrClient;
    private final List<SolrInputDocument> enrichedDocuments;
    private final String baseUrl;
    private final String solrField;
    private static final Logger LOG = Logger.getLogger(PartBClient.class.getName());

    PartBClient(String baseUrl, String field) {
        this.documents = new SolrDocumentList();
        this.solrClient = new HttpSolrClient.Builder("http://localhost:8983/solr/fet_proposals" ).build();
        this.enrichedDocuments = new ArrayList();
        this.baseUrl = baseUrl;
        this.solrField = field;
    }

    public Response.ResponseBuilder process() {
        getDocumentsFromSolr();
        prepareDocs();
        sendToSolr();
        return Response.ok();
    }

    private void getDocumentsFromSolr() {
        LOG.info("Getting Solr documents from localhost...");

        try {
            this.proposalDocuments = processQueryRequest();
            this.solrClient.setBaseURL(this.baseUrl);
            this.documents = processQueryRequest();
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(KeywordsClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private SolrDocumentList processQueryRequest() throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.setRequestHandler("/select");
        query.set("q", "*:*");
        query.set("rows", 15000);
        QueryRequest request = new QueryRequest(query);
        request.setBasicAuthCredentials("doris", "@Doris1");
        SolrDocumentList docs = request.process(this.solrClient).getResults();
        LOG.log(Level.INFO, "Request succeeded. Documents retrieved: {0} documents", docs.size());
        return docs;
    }

    private void prepareDocs() {
        LOG.info("Preparing tasks...");
        for(SolrDocument proposal: this.proposalDocuments){
            for (SolrDocument doc : this.documents){
                if(proposal.get("proposalNumber").equals(doc.get(this.solrField))){
                    SolrInputDocument inputDoc = new SolrInputDocument();
                    doc.getFieldNames().stream().forEach((name) -> {
                        inputDoc.addField(name, doc.getFieldValue(name));
                    });
                    inputDoc.addField("content_t", proposal.get("content"));
                    this.enrichedDocuments.add(inputDoc);
                    break;
                }
            }
        }
        LOG.info("Tasks ready to be executed.");
    }


    private void sendToSolr() {
        LOG.info("Executing tasks...");
        try {
            for (SolrInputDocument doc : this.enrichedDocuments) {
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



