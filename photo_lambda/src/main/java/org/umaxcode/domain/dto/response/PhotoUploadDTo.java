package org.umaxcode.domain.dto.response;

import lombok.Builder;

@Builder
public record PhotoUploadDTo(
        String picUrl
) {
}
