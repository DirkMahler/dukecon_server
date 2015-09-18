package org.dukecon.server.service;

import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.dukecon.model.Talk;
import org.dukecon.server.business.TalkProvider;
import org.springframework.stereotype.Component;

/**
 * @author Niko Köbler, http://www.n-k.de, @dasniko
 */
@Component
@Path("talks")
public class TalkService {

    @Inject
    TalkProvider talkProvider;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTalks() {
        Collection<Talk> talks = talkProvider.getAllTalks();
        return Response.ok().entity(talks).build();
    }
}
