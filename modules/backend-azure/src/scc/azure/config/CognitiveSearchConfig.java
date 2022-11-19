package scc.azure.config;

public class CognitiveSearchConfig {
    public final String key;
    public final String url;
    public final String auctionsIndex;
    public final String questionsIndex;

    public CognitiveSearchConfig(String key, String url, String auctionsIndex, String questionsIndex) {
        this.key = key;
        this.url = url;
        this.auctionsIndex = auctionsIndex;
        this.questionsIndex = questionsIndex;
    }

    @Override
    public String toString() {
        return "CognitiveSearchConfig [key=" + key + ", url=" + url + ", auctionsIndex=" + auctionsIndex
                + ", questionsIndex=" + questionsIndex + "]";
    }
}
