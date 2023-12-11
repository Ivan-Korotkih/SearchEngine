package searchengine.service;

import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.SiteTable;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteTableRepository;

import java.time.LocalDateTime;
@Service
public class FormationTablePages {
    private SiteTableRepository siteTableRepository;
    private PageRepository pageRepository;

    public FormationTablePages(SiteTableRepository siteTableRepository, PageRepository pageRepository) {
        this.siteTableRepository = siteTableRepository;
        this.pageRepository = pageRepository;
    }
    public void addNewPageToTablePages(String link, String content, int code, SiteTable siteTable){

        Page page = new Page();
        page.setPath(link.replaceFirst(siteTable.getUrl(), ""));
        page.setContent(content);
        page.setCode(code);
        page.setSite(siteTable);
        if (!siteTable.getUrl().equals(link)) {
            pageRepository.save(page);
        }
        siteTable.setStatusTime(LocalDateTime.now());
        siteTableRepository.save(siteTable);
    }
}
