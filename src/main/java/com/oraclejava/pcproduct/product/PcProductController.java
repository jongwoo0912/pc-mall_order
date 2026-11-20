package com.oraclejava.pcproduct.product;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.oraclejava.pcproduct.product.PcProductDtos.PcProductRequest;
import com.oraclejava.pcproduct.product.PcProductDtos.PcProductResponse;
import com.oraclejava.pcproduct.product.PcProductService.ImageType;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pc-products")
public class PcProductController {

	private final PcProductService service;

	public PcProductController(PcProductService service) {
		this.service = service;
	}

	@GetMapping
	public List<PcProductResponse> list() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public PcProductResponse get(@PathVariable Long id) {
		return service.findById(id);
	}

	/** JSON 등록 (이미지 없음) */
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PcProductResponse> create(@Valid @RequestBody PcProductRequest request) {
		return created(service.create(request, null, null));
	}

	/**
	 * multipart/form-data 등록.
	 * 필드: name, price, kind, text / 파일(선택): picS(작은 이미지), picL(큰 이미지)
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<PcProductResponse> createMultipart(
			@Valid @ModelAttribute PcProductRequest request,
			@RequestPart(value = "picS", required = false) MultipartFile picS,
			@RequestPart(value = "picL", required = false) MultipartFile picL) {
		return created(service.create(request, readOptionalImage(picS), readOptionalImage(picL)));
	}

	private static ResponseEntity<PcProductResponse> created(PcProductResponse created) {
		return ResponseEntity.created(URI.create("/api/pc-products/" + created.id())).body(created);
	}

	/** JSON 수정 (사진은 그대로 유지) */
	@PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public PcProductResponse update(@PathVariable Long id, @Valid @RequestBody PcProductRequest request) {
		return service.update(id, request, null, null);
	}

	/**
	 * multipart/form-data 수정.
	 * 필드: name, price, kind, text / 파일(선택): picS, picL — 보내지 않은 사진은 기존 사진 유지
	 */
	@PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public PcProductResponse updateMultipart(
			@PathVariable Long id,
			@Valid @ModelAttribute PcProductRequest request,
			@RequestPart(value = "picS", required = false) MultipartFile picS,
			@RequestPart(value = "picL", required = false) MultipartFile picL) {
		return service.update(id, request, readOptionalImage(picS), readOptionalImage(picL));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

	// ---- images (BLOB columns pic_s / pic_l) ----

	@GetMapping("/{id}/pic-s")
	public ResponseEntity<byte[]> getPicS(@PathVariable Long id) {
		return image(service.getImage(id, ImageType.SMALL));
	}

	@GetMapping("/{id}/pic-l")
	public ResponseEntity<byte[]> getPicL(@PathVariable Long id) {
		return image(service.getImage(id, ImageType.LARGE));
	}

	@PutMapping(path = "/{id}/pic-s", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Void> uploadPicS(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
		service.setImage(id, ImageType.SMALL, read(file));
		return ResponseEntity.noContent().build();
	}

	@PutMapping(path = "/{id}/pic-l", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Void> uploadPicL(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
		service.setImage(id, ImageType.LARGE, read(file));
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}/pic-s")
	public ResponseEntity<Void> deletePicS(@PathVariable Long id) {
		service.setImage(id, ImageType.SMALL, null);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}/pic-l")
	public ResponseEntity<Void> deletePicL(@PathVariable Long id) {
		service.setImage(id, ImageType.LARGE, null);
		return ResponseEntity.noContent().build();
	}

	/** 파일이 없거나 비어 있으면 null, 이미지가 아니면 400 */
	private static byte[] readOptionalImage(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			return null;
		}
		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not an image file: " + file.getOriginalFilename());
		}
		return read(file);
	}

	private static byte[] read(MultipartFile file) {
		if (file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
		}
		try {
			return file.getBytes();
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot read file", e);
		}
	}

	private static ResponseEntity<byte[]> image(byte[] data) {
		MediaType type = MediaType.APPLICATION_OCTET_STREAM;
		try {
			String guessed = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(data));
			if (guessed != null) {
				type = MediaType.parseMediaType(guessed);
			}
		} catch (IOException ignored) {
			// fall back to octet-stream
		}
		return ResponseEntity.ok().contentType(type).body(data);
	}
}
