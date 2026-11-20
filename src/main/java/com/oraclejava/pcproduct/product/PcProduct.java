package com.oraclejava.pcproduct.product;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "pc_product")
public class PcProduct {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false)
	private Integer price;

	@Column(nullable = false, length = 50)
	private String kind;

	@Column(length = 4000)
	private String text;

	// length up to 16MB -> MySQL MEDIUMBLOB (plain BLOB caps at 64KB, too small for photos)
	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(name = "pic_s", length = BLOB_MAX)
	private byte[] picS;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(name = "pic_l", length = BLOB_MAX)
	private byte[] picL;

	private static final int BLOB_MAX = 16 * 1024 * 1024 - 1;

	protected PcProduct() {
	}

	public PcProduct(String name, Integer price, String kind, String text) {
		this.name = name;
		this.price = price;
		this.kind = kind;
		this.text = text;
	}

	public void update(String name, Integer price, String kind, String text) {
		this.name = name;
		this.price = price;
		this.kind = kind;
		this.text = text;
	}

	public Long getId() { return id; }
	public String getName() { return name; }
	public Integer getPrice() { return price; }
	public String getKind() { return kind; }
	public String getText() { return text; }
	public byte[] getPicS() { return picS; }
	public byte[] getPicL() { return picL; }

	public void setPicS(byte[] picS) { this.picS = picS; }
	public void setPicL(byte[] picL) { this.picL = picL; }
}
