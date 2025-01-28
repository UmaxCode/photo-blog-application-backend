package org.umaxcode.lambda_apiGateway.domain.dto.response;

import lombok.Builder;

@Builder
public record PhotoUploadDTo(
        String picUrl
) {
}
