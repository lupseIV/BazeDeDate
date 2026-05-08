package org.example;

import java.util.List;

public class Page<T> {

    private final List<T> content;
    private final int     pageSize;
    private final int     pageNumber;     // -1 when keyset
    private final long    totalElements;  // -1 when keyset
    private final Long    lastId;         // null when offset
    private final boolean hasNext;

    public Page(List<T> content, int pageNumber, int pageSize, long totalElements) {
        this.content       = content;
        this.pageNumber    = pageNumber;
        this.pageSize      = pageSize;
        this.totalElements = totalElements;
        this.lastId        = null;
        this.hasNext       = (long)(pageNumber + 1) * pageSize < totalElements;
    }

    public Page(List<T> content, int pageSize, Long lastId, boolean hasNext) {
        this.content       = content;
        this.pageNumber    = -1;
        this.pageSize      = pageSize;
        this.totalElements = -1;
        this.lastId        = lastId;
        this.hasNext       = hasNext;
    }

    public List<T> getContent()       { return content; }
    public int     getPageNumber()    { return pageNumber; }
    public int     getPageSize()      { return pageSize; }
    public long    getTotalElements() { return totalElements; }
    public long    getTotalPages()    { return totalElements < 0 ? -1 : (totalElements + pageSize - 1) / pageSize; }
    public Long    getLastId()        { return lastId; }
    public boolean hasNext()          { return hasNext; }
    public boolean hasPrevious()      { return pageNumber > 0; }
}
