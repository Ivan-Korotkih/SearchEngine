package searchengine.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.response.Response;
import searchengine.model.SiteTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class CheckingSearchQuery {
    private final JdbcTemplate jdbcTemplate;
    @Autowired
    LemmaList lemmaList;
    @Autowired
    FormationResponseFromSearchQuery formationResponseFromSearchQuery;
    public String[] queryArray;
    public List<String> lemmaListOfQuery;
    public List<SiteTable> siteOfQueryList;

    public CheckingSearchQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Response getResponseFromSearch(String query, String site, int limit) {

        long start = System.currentTimeMillis();

        Response response = new Response();

        queryArray = query.toLowerCase(Locale.ROOT).replaceAll("([^а-яa-z\\s])", "").split("\\s+");

        if (queryArray.length > 5 || query.length() > 100) {
            response.setResult(false);
            response.setError("Задан слишком длинный запрос (максимальное количество слов - 5, " +
                    "max количество символов - 100)");
            return response;
        }
        if (query.trim().isEmpty()) {
            response.setResult(false);
            response.setError("Задан пустой поисковый запрос");
            return response;
        }
        if (isInvalidQuery(query)) {
            response.setResult(false);
            response.setError("Введённый поисковый запрос не содержит русских и английских слов");
            return response;
        }
        if (isIndexedSite(site)) {
            response = formationResponseFromSearchQuery.searchPagesThatMeetTheQuery(response, limit);
        } else {
            response.setResult(false);
            response.setError("Поиск невозможен сайты(сайт) имеют статус INDEXING или FAILED");
        }

        System.out.println("Время обработки запроса = " + (System.currentTimeMillis() - start));

        return response;
    }

    public boolean isInvalidQuery(String query) {
        lemmaList = new LemmaList();
        lemmaListOfQuery = new ArrayList<>(lemmaList.distributeTheWork(query));
        System.out.println(lemmaListOfQuery);
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
}
