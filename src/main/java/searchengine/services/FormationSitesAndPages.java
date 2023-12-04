package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.response.Response;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Service
public class FormationSitesAndPages {
    private SitesList sitesList;
    private SiteTableRepository siteTableRepository;
    private PageRepository pageRepository;
    private JdbcTemplate jdbcTemplate;
    private Vector<String> allLinksSite = new Vector<>();
    private boolean isCanWork;
    @Autowired
    FormationLemmasAndIndexes formationLemmasAndIndexes;
    public FormationSitesAndPages(SitesList sitesList, SiteTableRepository siteTableRepository,
                                  PageRepository pageRepository, JdbcTemplate jdbcTemplate) {
        this.sitesList = sitesList;
        this.siteTableRepository = siteTableRepository;
        this.pageRepository = pageRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isCanWork() {
        return isCanWork;
    }

    public void setCanWork(boolean canWork) {
        isCanWork = canWork;
    }

    public Response getResponseToStartIndexing() {

        Response response = new Response();
        if (isSitesIndexing()) {
            response.setResult(false);
            response.setError("Индексация уже запущена");
        } else {
            response.setResult(true);
            clearingTables();
            startIndexing();
        }
        return response;
    }

    public boolean isSitesIndexing() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM sites WHERE status = 'INDEXING'", Integer.class) != 0;
    }

    public void clearingTables() {
        jdbcTemplate.update("TRUNCATE indexes");
        jdbcTemplate.update("TRUNCATE lemmas");
        jdbcTemplate.update("TRUNCATE pages");
        jdbcTemplate.update("DELETE FROM sites");
        jdbcTemplate.update("ALTER TABLE pages ADD KEY (content(30))");
        allLinksSite.clear();
    }

    public void startIndexing() {
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

                new Thread(() ->{
                    ForkJoinPool forkJoinPool = new ForkJoinPool();
                    forkJoinPool.invoke(new Indexing(site.getUrl(), site.getUrl(), site.getName(), siteTable));
                    if (isCanWork()){
                        siteTable.setStatus(SiteTableEnum.INDEXED);

                    } else {
                        siteTable.setStatus(SiteTableEnum.FAILED);
                        siteTable.setLastError("Индексация остановлена пользователем");
                    }
                    siteTable.setStatusTime(LocalDateTime.now());
                    siteTableRepository.save(siteTable);
                    forkJoinPool.shutdown();
                    if (isCanWork){
                        formationLemmasAndIndexes.fillingTablesLemmasAndIndexes(siteTable.getId());
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

    public Response getResponseFromStopIndexing() {

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

    public class Indexing extends RecursiveAction {
        private String urlSite;
        private String link;
        private String name;
        private SiteTable siteTable;

        public Indexing(String urlSite, String link, String name, SiteTable siteTable) {
            this.urlSite = urlSite;
            this.link = link;
            this.name = name;
            this.siteTable = siteTable;
        }

        @Override
        protected void compute() {
            if (isCanWork()) {
                if (!allLinksSite.contains(link)) {
                    allLinksSite.add(link);
                    try {
                        Connection.Response response = Jsoup.connect(link).execute();
                        Document document = Jsoup.connect(link)
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                                .get();

                        addNewPageToTablePages(document.html(), response.statusCode(), siteTable);

                        List<Indexing> tasks = new ArrayList<>();
                        Elements elements = document.select("a[href]");
                        elements.forEach(element -> {
                            String href = element.attr("abs:href");
                            href = href.toLowerCase().trim();
                            if (href.startsWith(urlSite)) {
                                if (!(href.contains("?") || href.contains("#"))) {
                                    if (!(href.endsWith(".jpg") || href.endsWith(".pdf") || href.endsWith(".jpeg")
                                            || href.endsWith(".png") || href.endsWith(".xlsx")
                                            || href.endsWith(".eps") || href.endsWith(".doc"))) {
                                        tasks.add(new Indexing(urlSite, href, name, siteTable));
                                    }
                                }
                            }
                        });
                        invokeAll(tasks);
                    } catch (HttpStatusException e) {
                        addNewPageToTablePages("", e.getStatusCode(), siteTable);
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }

        public void addNewPageToTablePages(String content, int code, SiteTable siteTable){

            Page page = new Page();
            page.setPath(link.replaceFirst(urlSite, ""));
            page.setContent(content);
            page.setCode(code);
            page.setSite(siteTable);
            if (!urlSite.equals(link)) {
                pageRepository.save(page);
            }
            siteTable.setStatusTime(LocalDateTime.now());
            siteTableRepository.save(siteTable);
        }
    }
}