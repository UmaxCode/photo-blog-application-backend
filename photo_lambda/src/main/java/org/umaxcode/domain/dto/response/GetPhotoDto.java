package org.umaxcode.domain.dto.response;

import lombok.Builder;

@Builder
public record GetPhotoDto(
        String imgId,
        byte[] image
) {
}
