package searchengine.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.response.Response;
import searchengine.model.SiteTable;
import searchengine.service.FormationTableLemmas;

import java.util.ArrayList;
import java.util.List;
@Component
public class CheckingSearchQuery {
    private final JdbcTemplate jdbcTemplate;
    @Autowired
    FormationTableLemmas formationTableLemmas;
    @Autowired
    FormationResponseFromSearchQuery formationResponseFromSearchQuery;
    public String[] queryArray;
    public List<String> lemmaListOfQuery;
    public List<SiteTable> siteOfQueryList;

    public CheckingSearchQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Response getResponseFromSearch(String query, String site, int offset, int limit) {
        Response response = new Response();

        queryArray = query.split("\\s+");

        if (queryArray.length > 5) {
            response.setResult(false);
            response.setError("Задан слишком длинный запрос (максимальное количество слов - 5)");
            return response;
        }
        if (query.trim().isEmpty()) {
            response.setResult(false);
            response.setError("Задан пустой поисковый запрос");
            return response;
        }
        if (isInvalidQuery(query)) {
            response.setResult(false);
            response.setError("Введённый поисковый запрос не содержит русских слов");
            return response;
        }
        if (isIndexedSite(site)) {
            response = formationResponseFromSearchQuery.searchPagesThatMeetTheQuery(response, limit);
        } else {
            response.setResult(false);
            response.setError("Поиск невозможен сайты(сайт) имеют статус INDEXING или FAILED");
        }
        return response;
    }

    public boolean isInvalidQuery(String query) {
        lemmaListOfQuery = new ArrayList<>(createLemmaList(query));
        return lemmaListOfQuery.isEmpty();
    }

    public boolean isIndexedSite(String site) {
        siteOfQueryList = new ArrayList<>();
        String sqlCount = site == null ? "SELECT count(*) FROM sites WHERE status = 'INDEXED'" :
                "SELECT count(*) FROM sites WHERE status = 'INDEXED' AND url = '" + site + "'";
        if (jdbcTemplate.queryForObject(sqlCount, Integer.class) == 0) {
            return false;
        }
        String sqlSites = site == null ? "SELECT * FROM sites WHERE status = 'INDEXED'" :
                "SELECT * FROM sites WHERE status = 'INDEXED' AND url = '" + site + "'";
        siteOfQueryList = jdbcTemplate.query(sqlSites, (rs, rowNum) -> {
            SiteTable siteTable = new SiteTable();
            siteTable.setId(rs.getInt("id"));
            siteTable.setUrl(rs.getString("url"));
            siteTable.setName(rs.getString("name"));
            return siteTable;
        });
        System.out.println("количество обрабатываеммых сайтов " + siteOfQueryList.size());
        return true;
    }

    public List<String> createLemmaList(String query) {
        return formationTableLemmas.createListLemmas(query);
    }
}
