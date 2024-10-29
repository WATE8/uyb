package searchengine.model;

public enum Status {
    INDEXING,
    INDEXED,
    FAILED;

    public String getStatusMessage() {
        return "Current status: " + this.name();
    }
}