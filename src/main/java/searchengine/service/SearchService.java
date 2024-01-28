package searchengine.service;

import searchengine.dto.statistics.response.Response;

public interface SearchService {
    Response getResponse(String query, String site, int offset, int limit);
}
