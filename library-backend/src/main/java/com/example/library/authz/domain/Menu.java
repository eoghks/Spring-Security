package com.example.library.authz.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메뉴(화면 단위).
 */
@Entity
@Table(name = "menus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String code;

	private String name;

	private String path;

	private int sortOrder;
}
