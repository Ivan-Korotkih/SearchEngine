package searchengine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.response.Response;
import searchengine.service.FormationTableLemmas;
import searchengine.service.FormationTableSites;
import searchengine.service.Search;
import searchengine.service.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private FormationTableSites formationTableSites;
    @Autowired
    private FormationTableLemmas formationTableLemmas;
    @Autowired
    private Search search;
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
        return ResponseEntity.ok(formationTableSites.getResponseToStartIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Response> stopIndexing() {
        return ResponseEntity.ok(formationTableSites.getResponseFromStopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Response> indexPage(@RequestParam("url") String url) {
        return ResponseEntity.ok(formationTableLemmas.getResponseFromIndexPage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<Response> search(@RequestParam("query") String content, String site, int offset, int limit) {
        System.out.println(content + "  " + site + "  " + offset + "  " + limit);
        limit = 20;
        return ResponseEntity.ok(search.getResponse(content, site, offset, limit));
    }
}
