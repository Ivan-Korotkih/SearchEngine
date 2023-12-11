package searchengine.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.component.CheckingSearchQuery;
import searchengine.component.FormationResponseFromSearchQuery;
import searchengine.dto.statistics.response.Response;

@Service
public class Search {
    @Autowired
    CheckingSearchQuery checkingSearchQuery;
    @Autowired
    FormationResponseFromSearchQuery formationResponseFromSearchQuery;

    public Response getResponse(String query, String site, int offset, int limit) {
        Response response = new Response();

        response = offset == 0 ? (checkingSearchQuery.getResponseFromSearch(query, site, limit)) :
                                 (formationResponseFromSearchQuery.scrollingThroughPages(offset, limit));
        return response;
    }
}
