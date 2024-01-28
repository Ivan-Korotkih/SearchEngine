package searchengine.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.utils.LemmaList;
import searchengine.utils.SearchSnippet;
import searchengine.dto.statistics.response.Data;
import searchengine.dto.statistics.response.Response;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteTableRepository;

import java.util.*;

@Service
public class SearchServiceImpl implements SearchService, Comparator<Lemma>{
    private final JdbcTemplate jdbcTemplate;
    private final SiteTableRepository siteTableRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    @Autowired
    LemmaList lemmaList;
    @Autowired
    SearchSnippet searchSnippet;
    public List<String> lemmaListOfQuery;
    public List<SiteTable> siteOfQueryList;
    public String[] queryArray;
    private List<List<Lemma>> sortedAscendingLemmaList;
    private String answerForCheckLemma;
    private int pageIdListSize;
    private List<Data> dataList;

    public SearchServiceImpl(JdbcTemplate jdbcTemplate, SiteTableRepository siteTableRepository,
                             LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.siteTableRepository = siteTableRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Override
    public Response getResponse(String query, String site, int offset, int limit) {
        Response response = new Response();
        response = offset == 0 ? (creatingResponse(query, site, limit)) : (scrollingDataList(offset, limit));
        return response;
    }

    private Response creatingResponse(String query, String site, int limit) {

        long start = System.currentTimeMillis();

        Response response = new Response();
        queryArray = query.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-яёЁa-z\\s])", "")
                .split("\\s+");

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
        if (checkingForWords(query)) {
            response.setResult(false);
            response.setError("Введённый поисковый запрос не содержит русских и английских слов");
            return response;
        }
        if (checkingStatusSites(site)) {
            response = searchingPages(response, limit);
        } else {
            response.setResult(false);
            response.setError("Поиск невозможен сайты(сайт) имеют статус INDEXING или FAILED");
        }
        System.out.println("Время обработки запроса = " + (System.currentTimeMillis() - start));
        return response;
    }

    private boolean checkingForWords(String query) {
        lemmaListOfQuery = new ArrayList<>(lemmaList.getLemmaList(query));
        System.out.println(lemmaListOfQuery);
        return lemmaListOfQuery.isEmpty();
    }

    private boolean checkingStatusSites(String site) {
        siteOfQueryList = new ArrayList<>();
        if (site == null) {
            siteOfQueryList = siteTableRepository.findByStatus(SiteTableEnum.INDEXED);
        } else {
            siteOfQueryList = siteTableRepository.findByUrlAndStatus(site, SiteTableEnum.INDEXED);
        }
        System.out.println("количество обрабатываеммых сайтов " + siteOfQueryList.size());
        return !siteOfQueryList.isEmpty();
    }

    public Response searchingPages(Response response, int limit) {

        if (creatingSortLemmaList()) {
            List<Integer> pageIdList = creatingPageIdList();
            if (!pageIdList.isEmpty()) {
                pageIdListSize = pageIdList.size();
                List<Map.Entry<Integer, Float>> sortedPageIdList = createSortedPageIdList(pageIdList);
                createDataList(sortedPageIdList);
                List<Data> dataListResponse = new ArrayList<>();
                for (int i = 0; i < Math.min(limit, dataList.size()); i++) {
                    dataListResponse.add(dataList.get(i));
                }
                response.setResult(true);
                response.setCount(pageIdList.size());
                response.setData(dataListResponse);
            } else {
                response.setResult(false);
                response.setError("Не найдено ни одной страницы с такой комбинацией слов");
            }
        } else {
            response.setResult(false);
            response.setError(answerForCheckLemma);
        }
        return response;
    }

    public boolean creatingSortLemmaList() {

        // проверяем есть ли в таблице Lemmas нужные нам леммы и кол-во страниц где они встречаются
        // создаём список леммы и сортируем его по возрастанию в зависимости от frequency для каждого сайта
        answerForCheckLemma = "";
        sortedAscendingLemmaList = new ArrayList<>();

        for (SiteTable site : new ArrayList<>(siteOfQueryList)) {
            List<Lemma> lemmaListSort = new ArrayList<>();
            for (String lemmaOfQuery : new ArrayList<>(lemmaListOfQuery)) {
                if (lemmaRepository.findByLemmaAndSite(lemmaOfQuery, site) == null) {
                    siteOfQueryList.remove(site);
                    lemmaListSort.clear();
                    answerForCheckLemma = "Введённые слова или некоторые из них не найдены";
                    break;
                } else {
                    lemmaListSort.add(lemmaRepository.findByLemmaAndSite(lemmaOfQuery, site));
                    int count = lemmaRepository.findByLemmaAndSite(lemmaOfQuery, site).getFrequency();
                    if (count > 550) {
                        lemmaListSort.remove(lemmaRepository.findByLemmaAndSite(lemmaOfQuery, site));
                        lemmaListOfQuery.remove(lemmaOfQuery);
                        answerForCheckLemma = "Введённые слова находятся на большом количестве страниц - "
                                + count + " поэтому поиск прекращён";
                    }
                }
            }
            if (!lemmaListSort.isEmpty()){
                lemmaListSort.sort((this));
                sortedAscendingLemmaList.add(lemmaListSort);
            }
        }
        if (lemmaListOfQuery.isEmpty() || siteOfQueryList.isEmpty()) {
            return false;
        }
        System.out.println("Все или часть лемм найдены");
        return true;
    }

    public List<Integer> creatingPageIdList() {

        //создаем список страниц (pageId), на которых встречаются все леммы
        List<Integer> pageIdList = new ArrayList<>();
        for (List<Lemma> lemmaList : sortedAscendingLemmaList) {
            List<Integer> pageList = new ArrayList<>();
            for (int i = 0; i < lemmaList.size(); i++) {
                List<Integer> pages = jdbcTemplate.queryForList
                        ("SELECT page_id FROM indexes WHERE lemma_id = " + lemmaList.get(i).getId(), Integer.class);
                if (i == 0) {
                    pageList.addAll(pages);
                } else {
                    pageList.retainAll(pages);
                }
            }
            pageIdList.addAll(pageList);
        }
        System.out.println("Найдено страниц сайта " + pageIdList.size());
        return pageIdList;
    }

    public List<Map.Entry<Integer, Float>> createSortedPageIdList(List<Integer> pageIdList) {

        //подсчитываем абсолютную релевантность для каждой страницы (сумму всех rank всех найденных на странице лемм)
        HashMap<Integer, Float> pageIdMap = new HashMap<>();

        List<Integer> lemmaId1 = new ArrayList<>();
        List<Lemma> l = new ArrayList<>();
        for (String lemmaOfQuery : lemmaListOfQuery) {
            l.addAll(lemmaRepository.findByLemma(lemmaOfQuery));
        }
        for (Lemma lemma : l) {
            lemmaId1.add(lemma.getId());
        }

        List<Index> i = new ArrayList<>();
        for (int lemmaId : lemmaId1) {
            i.addAll(indexRepository.findByLemmaId(lemmaId));
        }
        for (Index index : i) {
            if (pageIdList.contains(index.getPageId())) {
                if (pageIdMap.containsKey(index.getPageId())) {
                    pageIdMap.put(index.getPageId(), pageIdMap.get(index.getPageId())
                            + index.getRank());
                } else {
                    pageIdMap.put(index.getPageId(), index.getRank());
                }
            }
        }

        // подсчитываем максимальную абсолютную релевантность среди всех страниц
        float maxAbsoluteRelevance = Collections.max(pageIdMap.values());

        //подсчитываем относительную релевантность
        pageIdMap.replaceAll((key, value) -> value / maxAbsoluteRelevance);

        // сортируем средную релевантность по убыванию
        List<Map.Entry<Integer, Float>> sortedPageIdList
                = pageIdMap.entrySet().stream().sorted(Map.Entry.<Integer, Float>comparingByValue().reversed()).toList();
        return sortedPageIdList;
    }

    public void createDataList(List<Map.Entry<Integer, Float>> sortedPageIdList) {

        dataList = new ArrayList<>();

        for (Map.Entry<Integer, Float> line : sortedPageIdList) {

            Data data = new Data();

            String sqlSite = "SELECT sites.url FROM sites JOIN pages ON pages.site_id = sites.id WHERE pages.id = "
                    + line.getKey();
            String site = jdbcTemplate.queryForObject(sqlSite, String.class);
            data.setSite(site);

            String sqlSiteName = "SELECT sites.name FROM sites JOIN pages ON pages.site_id = sites.id WHERE pages.id = "
                    + line.getKey();
            data.setSiteName(jdbcTemplate.queryForObject(sqlSiteName, String.class));

            String sqlUrl = "SELECT path FROM pages WHERE id = " + line.getKey();
            String url = jdbcTemplate.queryForObject(sqlUrl, String.class);
            data.setUri(url);

            data.setRelevance(line.getValue());

            String sqlContent = "SELECT content FROM pages WHERE id = " + line.getKey();
            String content = jdbcTemplate.queryForObject(sqlContent, String.class);
            int startTitle = content.indexOf("<title>") + 7;
            int finishTitle = content.indexOf("</title>");
            data.setTitle(content.substring(startTitle, finishTitle));

            data.setSnippet(searchSnippet.getSnippet(queryArray, content));
            dataList.add(data);
        }
    }

    private Response scrollingDataList(int offset, int limit) {
        Response response = new Response();
        List<Data> dataListResponse = new ArrayList<>();
        for (int i = offset; i < Math.min(limit + offset, dataList.size()); i++) {
            dataListResponse.add(dataList.get(i));
        }
        response.setResult(true);
        response.setCount(pageIdListSize);
        response.setData(dataListResponse);
        return response;
    }

    public int compare(Lemma o1, Lemma o2) {
        return o1.getFrequency() - o2.getFrequency();
    }
}
