package it.university.etfpac.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;

    private PageMetadata pageMetadata;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    // Nuovo metodo per gestire Page<T>
    public static <T> ApiResponse<Page<T>> success(Page<T> page, String message) {
        return ApiResponse.<Page<T>>builder()
                .success(true)
                .message(message)
                .data(page)
                .pageMetadata(PageMetadata.fromPage(page))
                .build();
    }

    public static <T> ApiResponse<T> error(String error) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageMetadata {
        private int currentPage;
        private int totalPages;
        private long totalElements;
        private int pageSize;
        private boolean first;
        private boolean last;

        public static PageMetadata fromPage(Page<?> page) {
            return PageMetadata.builder()
                    .currentPage(page.getNumber())
                    .totalPages(page.getTotalPages())
                    .totalElements(page.getTotalElements())
                    .pageSize(page.getSize())
                    .first(page.isFirst())
                    .last(page.isLast())
                    .build();
        }
    }
}