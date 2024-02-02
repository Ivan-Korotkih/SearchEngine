package searchengine.service;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.repository.LemmaRepository;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteTableRepository;
import searchengine.dto.statistics.response.Response;
import searchengine.utils.LemmaList;
import searchengine.utils.Indexing;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class IndexingServiceImpl implements IndexingService {
    private SitesList sitesList;
    private SiteTableRepository siteTableRepository;
    private LemmaRepository lemmaRepository;
    private PageRepository pageRepository;
    private final JdbcTemplate jdbcTemplate;
    private boolean isCanWork;
    private Map<String, Integer> lemmasMapFromTableLemmas;
    private HashMap<Integer, List<String>> lemmasListAllPages;
    private List<String[]> lemmasListFromTableIndex;
    private List<Page> pageList;
    @Autowired
    LemmaList lemmaList;
    @Autowired
    Indexing indexingClass;

    public IndexingServiceImpl(SitesList sitesList, SiteTableRepository siteTableRepository,
                               LemmaRepository lemmaRepository, PageRepository pageRepository,
                               JdbcTemplate jdbcTemplate) {
        this.sitesList = sitesList;
        this.siteTableRepository = siteTableRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Response getResponseFromStart() {
        return creatingResponseFromStart();
    }

    @Override
    public Response getResponseFromStop() {
        return creatingResponseFromStop();
    }

    @Override
    public Response getResponseFromIndexPage(String url) {
        return creatingResponseFromIndexPage(url);
    }

    public Response creatingResponseFromStart() {

        Response response = new Response();
        if (isSitesIndexing()) {
            response.setResult(false);
            response.setError("Индексация уже запущена");
        } else {
            response.setResult(true);
            startIndexing();
        }
        return response;
    }

    public Response creatingResponseFromStop() {

        Response response = new Response();
        if (isSitesIndexing()) {
            setCanWork(false);
            response.setResult(true);
        } else {
            response.setResult(false);
            response.setError("Индексация не запущена");
        }
        return response;
    }

    public boolean isSitesIndexing() {
        return siteTableRepository.findByStatus(SiteTableEnum.INDEXING).size() != 0;
    }

    public boolean isCanWork() {
        return isCanWork;
    }

    public void setCanWork(boolean canWork) {
        isCanWork = canWork;
    }

    public void startIndexing() {

        jdbcTemplate.update("TRUNCATE indexes");
        jdbcTemplate.update("TRUNCATE lemmas");
        jdbcTemplate.update("TRUNCATE pages");
        jdbcTemplate.update("DELETE FROM sites");
        setCanWork(true);

        for (Site site : sitesList.getSites()) {
            String responseSite = checkingResponseSite(site.getUrl());
            SiteTable siteTable = new SiteTable();
            siteTable.setName(site.getName());
            siteTable.setUrl(site.getUrl());
            siteTable.setStatusTime(LocalDateTime.now());

            if (!responseSite.equals("")) {
                siteTable.setStatus(SiteTableEnum.FAILED);
                siteTable.setLastError(responseSite);
                siteTableRepository.save(siteTable);
            } else {
                siteTable.setStatus(SiteTableEnum.INDEXING);
                siteTable.setLastError(null);
                siteTableRepository.save(siteTable);

                new Thread(() -> {
                    List<Page> pages = (indexingClass.getPageList(site.getUrl(), siteTable));

                    SiteTableEnum status = isCanWork() ? SiteTableEnum.INDEXED : SiteTableEnum.FAILED;
                    String lastError = status == SiteTableEnum.FAILED ? "Индексация остановлена пользователем" : null;
                    siteTable.setStatus(status);
                    siteTable.setLastError(lastError);
                    siteTable.setStatusTime(LocalDateTime.now());
                    siteTableRepository.save(siteTable);

                    if (status == SiteTableEnum.INDEXED) {
                        addDataToTablePages(pages);
                        fillingTablesLemmasAndIndexes(siteTable.getId());
                    }
                    System.out.println("Закончили искать страницы сайта " + site.getName());
                }).start();
            }
        }
    }

    public String checkingResponseSite(String url) {

        String responseSite;
        try {
            Jsoup.connect(url).execute();
            responseSite = "";
        } catch (HttpStatusException e) {
            responseSite = e.getStatusCode() + e.getMessage();
        } catch (IOException e) {
            responseSite = e.getMessage();
        }
        System.out.println(responseSite);
        return responseSite;
    }

    public void addDataToTablePages(List<Page> pages) {

        for (Page page : pages){
            pageRepository.save(page);
            page.getSite().setStatusTime(LocalDateTime.now());
            siteTableRepository.save(page.getSite());
        }
    }

    public void fillingTablesLemmasAndIndexes(int siteId) {

        createListPagesOfSite(siteId);
        lemmasListAllPages = new HashMap<>();
        lemmasMapFromTableLemmas = new HashMap<>();
        lemmasListFromTableIndex = new ArrayList<>();
        for (Page page : pageList) {
            List<String> lemmaListOfPage = lemmaList.getLemmaList(page.getContent());
            lemmasListAllPages.put(page.getId(), lemmaListOfPage);
        }
        createMapLemmasFromTableLemma();
        createListLemmasFromTableIndexes();
        writeDataToTableLemmas(siteId);
        writeDataToTableIndexes(lemmasListFromTableIndex, siteId);
    }

    public void createListPagesOfSite(int siteId) {

        pageList = new ArrayList<>();
        Iterable<Page> pages = pageRepository.findAll();
        for (Page page : pages) {
            if (page.getSite().getId() == (siteId)) {
                pageList.add(page);
            }
        }
    }

    public Response creatingResponseFromIndexPage(String url) {

        Response response = new Response();
        if (isValidURL(url)) {
            response.setResult(true);
        } else {
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        return response;
    }

    public boolean isValidURL(String url) {

        try {
            new URL(url);
            String newUrl = url.endsWith("/") ? url : url + "/";
            Iterable<SiteTable> siteTable = siteTableRepository.findAll();
            for (SiteTable site : siteTable) {
                if (newUrl.startsWith(site.getUrl())) {
                    new Thread(() -> {
                        if (isContainsInTablePages(newUrl, site)) {
                            deletingPageFromTablesPagesLemmasIndexes(newUrl, site);
                        } else {
                            writeToTablesPagesNewPage(newUrl, site);
                        }
                    }).start();
                    return true;
                }
            }
            return false;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public boolean isContainsInTablePages(String url, SiteTable site) {

        String urlFromCheck = url.replaceFirst(site.getUrl(), "");
        Iterable<Page> pages = pageRepository.findAll();
        for (Page page : pages) {
            if (page.getPath().equals(urlFromCheck) && page.getSite().getId() == site.getId()) {
                System.out.println("Страница есть в таблице Pages" + url);
                return true;
            }
        }
        System.out.println("Страницы нет в таблице Pages" + url);
        return false;
    }

    public void deletingPageFromTablesPagesLemmasIndexes(String url, SiteTable site) {

        String chengUrl = url.replaceFirst(site.getUrl(), "");
        int pageId = jdbcTemplate.queryForObject("SELECT id FROM pages WHERE path = '" + chengUrl + "'", Integer.class);

        if (pageId != 0) {
            List<Integer> lemmasId = new ArrayList<>();
            jdbcTemplate.query("SELECT lemma_id FROM indexes WHERE page_id = "
                    + pageId, ((rs, rowNum) -> {
                lemmasId.add(rs.getInt("lemma_id"));
                return null;
            }));
            jdbcTemplate.update("DELETE FROM indexes WHERE page_id = " + pageId);
            for (int lemmaId : lemmasId) {
                jdbcTemplate.update("update lemmas set frequency = frequency - 1 where id = " + lemmaId);
            }
            jdbcTemplate.update("DELETE FROM pages WHERE id = " + pageId);
        }
        writeToTablesPagesNewPage(url, site);
    }

    public void writeToTablesPagesNewPage(String url, SiteTable site) {

        int code;
        String content = "";
        try {
            code = Jsoup.connect(url).execute().statusCode();
            content = Jsoup.connect(url).get().html();
        } catch (HttpStatusException e) {
            code = e.getStatusCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Page newPage = new Page();
        newPage.setSite(site);
        newPage.setPath(url);
        newPage.setContent(content);
        newPage.setCode(code);
        pageRepository.save(newPage);
        writeToTablesLemmasIndexesNewPage(url);
    }

    public void writeToTablesLemmasIndexesNewPage(String url) {

        lemmasListAllPages = new HashMap<>();
        lemmasMapFromTableLemmas = new HashMap<>();
        lemmasListFromTableIndex = new ArrayList<>();

        int siteId = 0;
        Iterable<Page> pages = pageRepository.findAll();
        for (Page page : pages) {
            if (page.getPath().equals(url)) {
                List<String> lemmasListOfPage = lemmaList.getLemmaList(page.getContent());
                lemmasListAllPages.put(page.getId(), lemmasListOfPage);
                createMapLemmasFromTableLemma();
                createListLemmasFromTableIndexes();
                siteId = page.getSite().getId();
                break;
            }
        }
        if (!lemmasMapFromTableLemmas.isEmpty()) {
            for (Map.Entry<String, Integer> line : lemmasMapFromTableLemmas.entrySet()) {
                String sql = "update lemmas set frequency = frequency + 1 where site_id = "
                        + siteId + " and lemma = '" + line.getKey() + "'";
                jdbcTemplate.update(sql);
            }
            writeDataToTableIndexes(lemmasListFromTableIndex, siteId);
        }
    }

    public void createMapLemmasFromTableLemma() {

        HashMap<Integer, List<String>> lemmasListAllPagesCopy = new HashMap<>(lemmasListAllPages);

        for (Map.Entry<Integer, List<String>> entry : lemmasListAllPagesCopy.entrySet()) {
            HashSet<String> lemmasListFromTableLemmas = new HashSet<>(entry.getValue());
            for (String lemma : lemmasListFromTableLemmas) {
                if (lemmasMapFromTableLemmas.containsKey(lemma)) {
                    lemmasMapFromTableLemmas.put(lemma, lemmasMapFromTableLemmas.get(lemma) + 1);
                } else {
                    lemmasMapFromTableLemmas.put(lemma, 1);
                }
            }
        }
    }

    public void createListLemmasFromTableIndexes() {

        HashMap<Integer, List<String>> lemmasListAllPagesCopy = new HashMap<>(lemmasListAllPages);

        for (Map.Entry<Integer, List<String>> entry : lemmasListAllPagesCopy.entrySet()) {
            HashMap<String, Integer> map = new HashMap<>();
            for (String lemma : entry.getValue()) {
                if (map.containsKey(lemma)) {
                    map.put(lemma, map.get(lemma) + 1);
                } else {
                    map.put(lemma, 1);
                }
            }
            for (Map.Entry<String, Integer> line : map.entrySet()) {
                String[] array = new String[3];
                array[0] = line.getKey();
                array[1] = String.valueOf(line.getValue());
                array[2] = String.valueOf(entry.getKey());
                lemmasListFromTableIndex.add(array);
            }
        }
    }

    public void writeDataToTableLemmas(int siteId) {

        StringBuilder stringBuilder = new StringBuilder();
        String sql = "INSERT INTO lemmas(`site_id`, `lemma`, `frequency`) VALUES ";
        stringBuilder.append(sql);
        for (Map.Entry<String, Integer> line : lemmasMapFromTableLemmas.entrySet()) {
            stringBuilder.append("(")
                    .append(siteId)
                    .append(", '")
                    .append(line.getKey())
                    .append("', ")
                    .append(line.getValue())
                    .append("),");
        }
        stringBuilder.replace(stringBuilder.length() - 1, stringBuilder.length(), ";");
        jdbcTemplate.update(stringBuilder.toString());
    }

    public void writeDataToTableIndexes(List<String[]> lemmasListFromTableIndex, int siteId) {

        List<Index> indexList = new ArrayList<>();

        Iterable<Lemma> iterable = lemmaRepository.findAll();
        for (String[] line : lemmasListFromTableIndex) {
            Index index = new Index();
            index.setPageId(Integer.parseInt(line[2]));
            index.setRank(Float.parseFloat(line[1]));
            for (Lemma lemma : iterable) {
                if (lemma.getLemma().equals(line[0]) && lemma.getSite().getId() == siteId) {
                    index.setLemmaId(lemma.getId());
                    break;
                }
            }
            indexList.add(index);
        }
        StringBuilder stringBuilder = new StringBuilder();
        String sql = "INSERT INTO indexes(`page_id`, `lemma_id`, `rank`) VALUES ";
        stringBuilder.append(sql);
        for (Index index : indexList) {
            stringBuilder.append("(")
                    .append(index.getPageId())
                    .append(", ")
                    .append(index.getLemmaId())
                    .append(", ")
                    .append(index.getRank())
                    .append("),");
        }
        stringBuilder.replace(stringBuilder.length() - 1, stringBuilder.length(), ";");
        jdbcTemplate.update(stringBuilder.toString());
    }
}