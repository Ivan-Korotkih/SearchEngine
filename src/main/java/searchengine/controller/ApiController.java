package searchengine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.response.Response;
import searchengine.service.IndexingServiceImpl;
import searchengine.service.SearchServiceImpl;
import searchengine.service.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private IndexingServiceImpl indexingServiceImpl;
    @Autowired
    private SearchServiceImpl searchServiceImpl;
    private StatisticsService statisticsService;

    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Response> startIndexing() {
        return ResponseEntity.ok(indexingServiceImpl.getResponseFromStart());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Response> stopIndexing() {
        return ResponseEntity.ok(indexingServiceImpl.getResponseFromStop());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Response> indexPage(@RequestParam("url") String url) {
        return ResponseEntity.ok(indexingServiceImpl.getResponseFromIndexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<Response> search(@RequestParam("query") String content, String site, int offset, int limit) {
        System.out.println(content + "  " + site + "  " + offset + "  " + limit);
        limit = 20;
        return ResponseEntity.ok(searchServiceImpl.getResponse(content, site, offset, limit));
    }
}
