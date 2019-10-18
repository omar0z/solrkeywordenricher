package com.perezom.solrkeywordenricher;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ProposalsHandler {

    private static final Logger LOG = Logger.getLogger(ProposalsHandler.class.getName());

    private final Path contentsPath;
    private SolrClient client;


    ProposalsHandler() {
        this.contentsPath = FileSystems.getDefault().getPath("U:\\DorisRestricted\\Pepe\\partb");
        client = new HttpSolrClient.Builder("http://localhost:8983/solr/fet_proposals").build();
    }


    public String process() {

        LOG.info("Cleaning Solr...");
        cleanSolr();
        LOG.info("Fetching proposals...");
        List<Path> listProposals = getProposals();
        LOG.info("Proposals fetched - " + listProposals.size() + "proposals are ready to be processed...");
        Path[] proposalsToBeProcessed = new Path[10];

        int i = 0;
        while (listProposals.size() > 0) {
            i = 0;
            while (i < 10 ) {
                if (listProposals.size() == 0) break;
                proposalsToBeProcessed[i] = listProposals.remove(0);
                i++;
            }
            LOG.info("**** new batch being processed ***");
            try {
                sendFilesToSolr(prepareProposalsForIndex(proposalsToBeProcessed));
            } catch (IOException | SolrServerException e) {
                e.printStackTrace();
            }
            proposalsToBeProcessed = new Path[10];
        }

        return "OK";
    }

    private void cleanSolr() {
        try {
            UpdateRequest request = new UpdateRequest();
            request.setBasicAuthCredentials("doris", "@Doris1");
            request.deleteByQuery("*:*");
            request.setAction(AbstractUpdateRequest.ACTION.COMMIT, false, true, true);
            request.process(this.client);
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFilesToSolr(List<SolrInputDocument> solrDocs) throws IOException, SolrServerException {
        UpdateRequest request = new UpdateRequest();
        request.setBasicAuthCredentials("doris", "@Doris1");
        request.add(solrDocs);
        request.setAction(AbstractUpdateRequest.ACTION.COMMIT, false, true, true);
        request.process(this.client);
    }

    private List<SolrInputDocument> prepareProposalsForIndex(Path[] listProposals) {
        List<SolrInputDocument> docs = new ArrayList<>();
        Stream.of(listProposals).forEach(proposal -> {
            if (proposal == null) return;
            String fileName = proposal.getFileName().toString();
            LOG.info("Processing proposal: " + fileName);
            String proposalNumber = getProposalNumber(fileName);
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("proposalNumber", proposalNumber);
            try {

                doc.addField("content", getContentPropossal(proposal));

            } catch (IOException | TikaException e) {
                e.printStackTrace();
            }
            docs.add(doc);
        });

        return docs;
    }

    private String getContentPropossal(Path proposal) throws IOException, TikaException {
        Tika tika = new Tika();
        return tika.parseToString(proposal);
    }

    private String getProposalNumber(String fileName) {
        String proposalNumber = fileName.substring(0, fileName.indexOf("_"));
        return proposalNumber;
    }


    private List<Path> getProposals() {
        List<Path> proposals = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.contentsPath, "*.{pdf}")) {
            for (Path proposal : stream) {
                proposals.add(proposal);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return proposals;
    }
}
