package searchengine.utils;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.Page;
import searchengine.model.SiteTable;
import searchengine.service.IndexingServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Component
public class Indexing {
    @Autowired
    IndexingServiceImpl indexingService;
    private Vector<String> allLinksSite;
    private Vector<Page> pages;
    public synchronized List<Page> getPageList(String url, SiteTable siteTable){
        allLinksSite = new Vector<>();
        pages = new Vector<>();
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.invoke(new IndexingPages(url, siteTable));
        forkJoinPool.shutdown();
        return pages;
    }

    private boolean isCanWork(){
        return indexingService.isCanWork();
    }

    private class IndexingPages extends RecursiveAction {
        private final String link;
        private final SiteTable siteTable;

        public IndexingPages(String link, SiteTable siteTable) {
            this.link = link;
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

                        addNewPageToTablePages(link, document.html(), response.statusCode(), siteTable);

                        List<IndexingPages> tasks = new ArrayList<>();
                        Elements elements = document.select("a[href]");
                        elements.forEach(element -> {
                            String href = element.attr("abs:href");
                            if (isLinkAppropriate(href)) {
                                tasks.add(new IndexingPages(href, siteTable));
                            }
                        });
                        invokeAll(tasks);
                    } catch (HttpStatusException e) {
                        addNewPageToTablePages(link, "", e.getStatusCode(), siteTable);
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }

        private void addNewPageToTablePages(String link, String content, int code, SiteTable siteTable) {
            Page page = new Page();
            page.setPath(link.replaceFirst(siteTable.getUrl(), ""));
            page.setContent(content);
            page.setCode(code);
            page.setSite(siteTable);
            if (!siteTable.getUrl().equals(link)) {
                pages.add(page);
            }
        }

        private boolean isLinkAppropriate(String href) {
            href = href.toLowerCase().trim();
            if (href.startsWith(siteTable.getUrl())) {
                if (!(href.contains("?") || href.contains("#") || href.substring(siteTable.getUrl().length()).contains(":"))) {
                    return !(href.endsWith(".jpg") || href.endsWith(".pdf") || href.endsWith(".jpeg")
                            || href.endsWith(".png") || href.endsWith(".xlsx")
                            || href.endsWith(".eps") || href.endsWith(".doc"));
                }
            }
            return false;
        }
    }
}
