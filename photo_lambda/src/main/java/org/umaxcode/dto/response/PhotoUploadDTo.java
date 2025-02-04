package org.umaxcode.dto.response;

import lombok.Builder;

@Builder
public record PhotoUploadDTo(
        String picUrl
) {
}
