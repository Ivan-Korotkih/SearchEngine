package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.SiteTable;
import searchengine.model.SiteTableEnum;

import java.util.List;

@Repository
public interface SiteTableRepository extends JpaRepository<SiteTable, Integer> {
    List<SiteTable> findByStatus(SiteTableEnum siteTableEnum);
    List<SiteTable> findByUrlAndStatus(String url, SiteTableEnum siteTableEnum);

}
