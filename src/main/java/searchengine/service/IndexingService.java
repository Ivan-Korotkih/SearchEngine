package searchengine.service;

import searchengine.dto.statistics.response.Response;

public interface IndexingService {
    Response getResponseFromStart();
    Response getResponseFromStop();
    Response getResponseFromIndexPage(String url);
}
