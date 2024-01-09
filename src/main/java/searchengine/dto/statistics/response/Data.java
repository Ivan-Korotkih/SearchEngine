package searchengine.dto.statistics.response;

@lombok.Data
public class Data {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;
}
