package com.oraclejava.pcproduct.product;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.oraclejava.pcproduct.product.PcProductDtos.PcProductRequest;
import com.oraclejava.pcproduct.product.PcProductDtos.PcProductResponse;

@Service
@Transactional(readOnly = true)
public class PcProductService {

	public enum ImageType { SMALL, LARGE }

	private final PcProductRepository repository;

	public PcProductService(PcProductRepository repository) {
		this.repository = repository;
	}

	public List<PcProductResponse> findAll() {
		return repository.findAll().stream().map(PcProductResponse::from).toList();
	}

	public PcProductResponse findById(Long id) {
		return PcProductResponse.from(get(id));
	}

	@Transactional
	public PcProductResponse create(PcProductRequest req, byte[] picS, byte[] picL) {
		PcProduct product = new PcProduct(req.name(), req.price(), req.kind(), req.text());
		product.setPicS(picS);
		product.setPicL(picL);
		return PcProductResponse.from(repository.save(product));
	}

	@Transactional
	/** picS / picL 이 null 이면 기존 사진을 그대로 유지 */
	public PcProductResponse update(Long id, PcProductRequest req, byte[] picS, byte[] picL) {
		PcProduct product = get(id);
		product.update(req.name(), req.price(), req.kind(), req.text());
		if (picS != null) {
			product.setPicS(picS);
		}
		if (picL != null) {
			product.setPicL(picL);
		}
		return PcProductResponse.from(product);
	}

	/** 주문 내역(order_line)에 들어 있는 제품은 삭제할 수 없음 → 409 */
	@Transactional
	public void delete(Long id) {
		repository.delete(get(id));
		try {
			repository.flush();
		} catch (DataIntegrityViolationException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "주문 내역이 있는 제품은 삭제할 수 없습니다.", e);
		}
	}

	public byte[] getImage(Long id, ImageType type) {
		PcProduct product = get(id);
		byte[] data = type == ImageType.SMALL ? product.getPicS() : product.getPicL();
		if (data == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
		}
		return data;
	}

	@Transactional
	public void setImage(Long id, ImageType type, byte[] data) {
		PcProduct product = get(id);
		if (type == ImageType.SMALL) {
			product.setPicS(data);
		} else {
			product.setPicL(data);
		}
	}

	private PcProduct get(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PcProduct " + id + " not found"));
	}
}
