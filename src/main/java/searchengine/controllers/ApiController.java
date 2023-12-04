package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.IndexRepository;
import searchengine.model.LemmaRepository;
import searchengine.model.PageRepository;
import searchengine.model.SiteTableRepository;
import searchengine.response.Response;
import searchengine.services.FormationLemmasAndIndexes;
import searchengine.services.FormationSitesAndPages;
import searchengine.services.Search;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private SiteTableRepository siteTableRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private SitesList sitesList;
    @Autowired
    private FormationSitesAndPages formationSitesAndPages;
    @Autowired
    private FormationLemmasAndIndexes formationLemmasAndIndexes;
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
    public ResponseEntity<Response> startIndexing(){
        return ResponseEntity.ok(formationSitesAndPages.getResponseToStartIndexing());
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<Response> stopIndexing(){
        return ResponseEntity.ok(formationSitesAndPages.getResponseFromStopIndexing());
    }
    @PostMapping("/indexPage")
    public ResponseEntity<Response> indexPage(@RequestParam ("url") String url){
        return ResponseEntity.ok(formationLemmasAndIndexes.getResponseFromIndexPage(url));
    }
    @GetMapping("/search")
    public ResponseEntity<Response> search(@RequestParam ("query") String content, String site, int offset, int limit){
        System.out.println(content + "  " + site + "  " + offset + "  " + limit);
        limit = 20;
        return offset == 0 ? ResponseEntity.ok(search.getResponseFromSearch(content, site, offset, limit)) :
                ResponseEntity.ok(search.scrollingThroughPages(offset, limit));
    }
}
