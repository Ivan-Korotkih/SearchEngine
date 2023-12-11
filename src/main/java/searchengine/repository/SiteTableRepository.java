package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteTable;

@Repository
public interface SiteTableRepository extends CrudRepository<SiteTable, Integer> {

}
