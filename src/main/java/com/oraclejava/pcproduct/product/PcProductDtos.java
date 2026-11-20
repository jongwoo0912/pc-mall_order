package com.oraclejava.pcproduct.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class PcProductDtos {

	private PcProductDtos() {
	}

	/** Create/update request. Images are uploaded separately via the image endpoints. */
	public record PcProductRequest(
			@NotBlank @Size(max = 200) String name,
			@NotNull @PositiveOrZero Integer price,
			@NotBlank @Size(max = 50) String kind,
			@Size(max = 4000) String text) {
	}

	/** Response without image bytes; images are served from picSUrl / picLUrl. */
	public record PcProductResponse(
			Long id,
			String name,
			Integer price,
			String kind,
			String text,
			String picSUrl,
			String picLUrl) {

		static PcProductResponse from(PcProduct p) {
			String base = "/api/pc-products/" + p.getId();
			return new PcProductResponse(
					p.getId(), p.getName(), p.getPrice(), p.getKind(), p.getText(),
					p.getPicS() != null ? base + "/pic-s" : null,
					p.getPicL() != null ? base + "/pic-l" : null);
		}
	}
}
