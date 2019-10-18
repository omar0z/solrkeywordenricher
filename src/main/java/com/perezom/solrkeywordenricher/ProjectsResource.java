package com.perezom.solrkeywordenricher;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("projects")
public class ProjectsResource {

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Path("keywords")
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        KeywordsClient keywordsClient = new KeywordsClient("fet-h2020-ria-noHPC-1", "projectAbstract");
        return keywordsClient.process().toString();
    }

    @GET
    @Path("partb")
    @Produces(MediaType.TEXT_PLAIN)
    public String enrichDocsWithProposalContent() {
        PartBClient partBClient = new PartBClient("http://localhost:8983/solr/fet-h2020-ria-noHPC-1", "projectId");
        return partBClient.process().toString();
    }
}
