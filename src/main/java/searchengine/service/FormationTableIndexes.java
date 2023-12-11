package searchengine.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.repository.LemmaRepository;

import java.util.ArrayList;
import java.util.List;
@Service
public class FormationTableIndexes {
    private LemmaRepository lemmaRepository;
    private JdbcTemplate jdbcTemplate;

    public FormationTableIndexes(LemmaRepository lemmaRepository, JdbcTemplate jdbcTemplate) {
        this.lemmaRepository = lemmaRepository;
        this.jdbcTemplate = jdbcTemplate;
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
