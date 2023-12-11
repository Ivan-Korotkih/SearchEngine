package searchengine.dto.statistics.response;

import java.util.List;

@lombok.Data
public class Response {
    private boolean result;
    private String error;
    private int count;
    private List<Data> data;

}
