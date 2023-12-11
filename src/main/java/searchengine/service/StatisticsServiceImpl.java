package searchengine.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteTable;
import searchengine.repository.SiteTableRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private SitesList sites;
    private JdbcTemplate jdbcTemplate;
    private SiteTableRepository siteTableRepository;

    public StatisticsServiceImpl(SitesList sites, JdbcTemplate jdbcTemplate, SiteTableRepository siteTableRepository) {
        this.sites = sites;
        this.jdbcTemplate = jdbcTemplate;
        this.siteTableRepository = siteTableRepository;
    }

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setPages(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pages", Integer.class));
        total.setLemmas(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lemmas", Integer.class));
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        Iterable<SiteTable> siteTable = siteTableRepository.findAll();
        for (SiteTable site : siteTable) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            String sqlPage = "SELECT COUNT(*) FROM pages WHERE site_id = " + site.getId();
            item.setPages(jdbcTemplate.queryForObject(sqlPage, Integer.class));
            String sqlLemma = "SELECT COUNT(*) FROM lemmas WHERE site_id = " + site.getId();
            item.setLemmas(jdbcTemplate.queryForObject(sqlLemma, Integer.class));
            item.setStatus(site.getStatus().toString());
            item.setStatusTime(site.getStatusTime());
            item.setError(site.getLastError());
            detailed.add(item);
        }

        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);

        StatisticsResponse response = new StatisticsResponse();
        response.setStatistics(data);
        response.setResult(true);

        return response;
    }
}
