package searchengine.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.response.Data;
import searchengine.dto.statistics.response.Response;
import searchengine.model.Lemma;
import searchengine.model.SiteTable;
import searchengine.service.FormationTableLemmas;

import java.util.*;

@Component
public class FormationResponseFromSearchQuery {
    private final JdbcTemplate jdbcTemplate;
    @Autowired
    FormationTableLemmas formationTableLemmas;
    @Autowired
    CheckingSearchQuery checkingSearchQuery;
    private List<List<Lemma>> sortedAscendingLemmaList;
    private String answerForCheckLemma;
    private int pageIdListSize;
    private List<Data> dataList;
    private final int[] ratioSnippet = {25, 20, 15, 15, 10, 10, 5, 5, 5, 5};

    public FormationResponseFromSearchQuery(JdbcTemplate jdbcTemplate, FormationTableLemmas formationTableLemmas) {
        this.jdbcTemplate = jdbcTemplate;
        this.formationTableLemmas = formationTableLemmas;
    }

    public Response searchPagesThatMeetTheQuery(Response response, int limit) {

        if (isCreatingMapLemmasFrequency()) {
            List<Integer> pageIdList = creatingPageIdList();
            if (!pageIdList.isEmpty()) {
                pageIdListSize = pageIdList.size();
                List<Map.Entry<Integer, Float>> sortedDescendingRelativeRelevanceList
                        = calculatingRelevance(pageIdList);
                formingListOfFoundPages(sortedDescendingRelativeRelevanceList);
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

    public boolean isCreatingMapLemmasFrequency() {

        // проверяем есть ли в таблице Lemmas нужные нам леммы (для каждого сайта), и кол-во страниц где она встречается
        answerForCheckLemma = "";
        for (SiteTable site : new ArrayList<>(checkingSearchQuery.siteOfQueryList)) {
            for (String lemmaOfQuery : new ArrayList<>(checkingSearchQuery.lemmaListOfQuery)) {
                String sqlCount = "SELECT count(*) FROM lemmas WHERE lemma = '"
                        + lemmaOfQuery + "' AND site_id = " + site.getId();
                if (jdbcTemplate.queryForObject(sqlCount, Integer.class) == 0) {
                    checkingSearchQuery.siteOfQueryList.remove(site);
                    answerForCheckLemma = "Введённые слова или некоторые из них не найдены";
                } else {
                    String sqlFrequency = "SELECT frequency FROM lemmas WHERE lemma = '"
                            + lemmaOfQuery + "' AND site_id = " + site.getId();
                    int count = jdbcTemplate.queryForObject(sqlFrequency, Integer.class);
                    if (count > 100) {
                        checkingSearchQuery.lemmaListOfQuery.remove(lemmaOfQuery);
                        answerForCheckLemma = "Введённые слова находятся на большом количестве страниц - "
                                + count + " поэтому поиск прекращён";
                    }
                }
            }
        }
        if (checkingSearchQuery.lemmaListOfQuery.isEmpty() || checkingSearchQuery.siteOfQueryList.isEmpty()) {
            return false;
        }
        System.out.println("Все или часть лемм найдены");

        // содаем список Мар лемм и количество страниц, на которых слово встречается хотя бы один раз, для каждого сайта
        List<HashMap<Lemma, Integer>> mapLemmaFrequencyList = new ArrayList<>();
        for (SiteTable site : checkingSearchQuery.siteOfQueryList) {
            HashMap<Lemma, Integer> map = new HashMap<>();
            for (String lemmaOfQuery : checkingSearchQuery.lemmaListOfQuery) {
                String sqlLemma = "SELECT * FROM lemmas WHERE lemma = '"
                        + lemmaOfQuery + "' AND site_id = " + site.getId();
                Map<String, Object> object = jdbcTemplate.queryForMap(sqlLemma);
                Lemma lemma = new Lemma();
                lemma.setId((Integer) object.get("id"));
                lemma.setFrequency((Integer) object.get("frequency"));
                lemma.setLemma((String) object.get("lemma"));
                lemma.setSite(site);
                map.put(lemma, lemma.getFrequency());
            }
            mapLemmaFrequencyList.add(map);
        }

        // сортируем каждую Мар в списке mapLemmaFrequencyList по возрастанию, и создаем отсортированный список лем
        // для каждого сайта
        sortedAscendingLemmaList = new ArrayList<>();
        for (HashMap<Lemma, Integer> map : mapLemmaFrequencyList) {
            List<Map.Entry<Lemma, Integer>> sortedLemmaMap = map.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue()).toList();
            List<Lemma> sortedLemmaList = new ArrayList<>();
            for (Map.Entry<Lemma, Integer> lemma : sortedLemmaMap) {
                sortedLemmaList.add(lemma.getKey());
            }
            sortedAscendingLemmaList.add(sortedLemmaList);
        }
        return true;
    }

    public List<Integer> creatingPageIdList() {

        //создаем список из page (pageId), на которых встречаются все леммы
        List<Integer> pageIdList = new ArrayList<>();
        for (List<Lemma> lemmaList : sortedAscendingLemmaList) {
            List<Integer> pageList = new ArrayList<>();
            for (int i = 0; i < lemmaList.size(); i++) {
                String sql = "SELECT indexes.page_id FROM indexes JOIN lemmas on indexes.lemma_id = lemmas.id " +
                        "WHERE lemma = '" + lemmaList.get(i).getLemma() + "' AND site_id = "
                        + lemmaList.get(i).getSite().getId();
                List<Integer> pages = jdbcTemplate.queryForList(sql, Integer.class);
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

    public List<Map.Entry<Integer, Float>> calculatingRelevance(List<Integer> pageIdList) {

        //подсчитываем абсолютную релевантность для каждой страницы
        HashMap<Integer, Integer> absoluteRelevanceList = new HashMap<>();
        for (int pageId : pageIdList) {
            int absoluteRelevance = 0;
            for (String lemma : checkingSearchQuery.lemmaListOfQuery) {
                String sql = "SELECT indexes.`rank` FROM indexes JOIN lemmas ON lemmas.id = indexes.lemma_id " +
                        "WHERE indexes.page_id = " + pageId + " AND lemmas.lemma = '" + lemma + "'";
                Integer rank = jdbcTemplate.queryForObject(sql, Integer.class);
                absoluteRelevance += rank;
            }
            absoluteRelevanceList.put(pageId, absoluteRelevance);
        }

        //подсчитываем относительную релевантность
        int maxAbsoluteRelevance = Collections.max(new ArrayList<>(absoluteRelevanceList.values()));

        HashMap<Integer, Float> relativeRelevanceList = new HashMap<>();
        for (Map.Entry<Integer, Integer> line : absoluteRelevanceList.entrySet()) {
            float relativeRelevance = (float) line.getValue() / maxAbsoluteRelevance;
            relativeRelevanceList.put(line.getKey(), relativeRelevance);
        }

        // сортируем средную релевантность по убыванию
        List<Map.Entry<Integer, Float>> sortedDescendingRelativeRelevanceList
                = relativeRelevanceList.entrySet().stream()
                .sorted(Map.Entry.<Integer, Float>comparingByValue()
                        .reversed()).toList();
        return sortedDescendingRelativeRelevanceList;
    }

    public void formingListOfFoundPages(List<Map.Entry<Integer, Float>> sortedDescendingRelativeRelevanceList) {

        dataList = new ArrayList<>();

        for (Map.Entry<Integer, Float> line : sortedDescendingRelativeRelevanceList) {
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

            data.setSnippet(searchSnippet(content));

            dataList.add(data);
        }
    }

    public String searchSnippet(String content) {

        List<String> lemmaContent = formationTableLemmas.createListLemmas(content);
        String[] text = lemmaContent.toArray(new String[0]);
        List<String> result = new ArrayList<>();
        int lengthSnippet = ratioSnippet[checkingSearchQuery.lemmaListOfQuery.size()];

        for (String s : checkingSearchQuery.lemmaListOfQuery) {
            boolean isContains = false;
            for (String r : result) {
                if (r.contains(s)) {
                    isContains = true;
                    break;
                }
            }
            if (!isContains) {
                int position = 0;
                for (int i = 0; i < text.length; i++) {
                    if (text[i].equals(s)) {
                        position = i;
                        break;
                    }
                }
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = position; i < (Math.min(position + lengthSnippet, text.length)); i++) {
                    stringBuilder.append(text[i]).append(" ");
                }
                result.add(stringBuilder.toString());
            }
        }
        StringBuilder finalSnippet = new StringBuilder();
        for (String r : result) {
            String stringR = r;
            for (String l : checkingSearchQuery.lemmaListOfQuery) {
                stringR = stringR.replace(l + " ", " <b>" + l + "</b> ");
            }
            for (String q : checkingSearchQuery.queryArray) {
                stringR = stringR.replace(q + " ", " <b>" + q + "</b> ");
            }
            finalSnippet.append(stringR).append(" ");
        }
        return finalSnippet.toString();
    }

    public Response scrollingThroughPages(int offset, int limit) {
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
}

