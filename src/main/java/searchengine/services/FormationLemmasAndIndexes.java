package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.response.Response;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Semaphore;

@Service
public class FormationLemmasAndIndexes {
    private SiteTableRepository siteTableRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private JdbcTemplate jdbcTemplate;

    private List<Page> pageList;
    private HashMap<Integer, List<String>> lemmasListAllPages;
    private Map<String, Integer> lemmasMapFromTableLemmas;
    private List<String[]> lemmasListFromTableIndex;
    private final int CORES = Runtime.getRuntime().availableProcessors();
    private final Semaphore SEMAPHORE = new Semaphore(CORES, true);

    @Autowired
    public FormationLemmasAndIndexes(SiteTableRepository siteTableRepository, PageRepository pageRepository,
                                     LemmaRepository lemmaRepository, IndexRepository indexRepository,
                                     JdbcTemplate jdbcTemplate) {
        this.siteTableRepository = siteTableRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public synchronized void fillingTablesLemmasAndIndexes(int siteId) {
        createListPagesOfSite(siteId);
        lemmasListAllPages = new HashMap<>();
        lemmasMapFromTableLemmas = new HashMap<>();
        lemmasListFromTableIndex = new ArrayList<>();

        Thread thread = null;
        try {
            for (int i = 0; i < pageList.size(); i++) {
                int x = i;
                thread = new Thread(() -> {
                    List<String> lemmaListOfPage = createListLemmas(pageList.get(x).getContent());
                    lemmasListAllPages.put(pageList.get(x).getId(), lemmaListOfPage);
                    SEMAPHORE.release();
                });
                thread.start();
                SEMAPHORE.acquire();
            }
            assert thread != null;
            thread.join();
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        createMapLemmasFromTableLemma();
        createListLemmasFromTableIndexes();
        writeDataToTableLemmas(siteId);
        writeDataToTableIndexes(siteId);
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

    public Response getResponseFromIndexPage(String url) {
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
                List<String> lemmasListOfPage = createListLemmas(page.getContent());
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
            writeDataToTableIndexes(siteId);
        }
    }

    public List<String> createListLemmas(String content) {

        List<String> lemmasListOfPage = new ArrayList<>();
        try {
            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            String[] rusWords = content.toLowerCase(Locale.ROOT)
                    .replaceAll("([^а-я\\s])", " ")
                    .trim()
                    .split("\\s+");

            for (String rusWord : rusWords) {
                if (rusWord.length() > 1) {
                    List<String> morphInfo = luceneMorph.getMorphInfo(rusWord);
                    String word = morphInfo.get(0);
                    if (!(word.contains("МЕЖД") || word.contains("СОЮЗ") ||
                            word.contains("ПРЕДЛ") || word.contains("ЧАСТ"))) {
                        List<String> normalForms = luceneMorph.getNormalForms(rusWord);
                        lemmasListOfPage.addAll(normalForms);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return lemmasListOfPage;
    }

    public void createMapLemmasFromTableLemma() {

        for (Map.Entry<Integer, List<String>> entry : lemmasListAllPages.entrySet()) {
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

        for (Map.Entry<Integer, List<String>> entry : lemmasListAllPages.entrySet()) {
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

    public void writeDataToTableIndexes(int siteId) {

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