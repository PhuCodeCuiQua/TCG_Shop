package com.tcgshop.controller.rest;

import com.tcgshop.dto.request.CardRequest;
import com.tcgshop.dto.response.CardListingResponse;
import com.tcgshop.dto.response.CardResponse;
import com.tcgshop.dto.response.PageResponse;
import com.tcgshop.entity.enums.Rarity;
import com.tcgshop.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardRestController {

	private static final int MAX_PAGE_SIZE = 100;

	private final CardService cardService;

	@GetMapping("/listing")
	@PreAuthorize("hasRole('VENDOR')")
	public ResponseEntity<PageResponse<CardListingResponse>> searchForListing(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long seriesId,
			Pageable pageable) {
		return ResponseEntity.ok(cardService.searchForListing(keyword, seriesId, safePageable(pageable)));
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
	public ResponseEntity<PageResponse<CardResponse>> search(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long gameId,
			@RequestParam(required = false) Long seriesId,
			@RequestParam(required = false) Rarity rarity,
			@RequestParam(required = false) Boolean active,
			Pageable pageable) {
		return ResponseEntity.ok(cardService.search(
				keyword, gameId, seriesId, rarity, active, safePageable(pageable)));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
	public ResponseEntity<CardResponse> getById(@PathVariable Long id) {
		return ResponseEntity.ok(cardService.getById(id));
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
	public ResponseEntity<CardResponse> create(
			@Valid @RequestPart("card") CardRequest request,
			@RequestPart(value = "image", required = false) MultipartFile image) {
		return ResponseEntity.status(HttpStatus.CREATED).body(cardService.create(request, image));
	}

	@PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
	public ResponseEntity<CardResponse> update(
			@PathVariable Long id,
			@Valid @RequestPart("card") CardRequest request,
			@RequestPart(value = "image", required = false) MultipartFile image) {
		return ResponseEntity.ok(cardService.update(id, request, image));
	}

	@PatchMapping("/{id}/active")
	@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
	public ResponseEntity<CardResponse> setActive(
			@PathVariable Long id,
			@RequestParam boolean active) {
		return ResponseEntity.ok(cardService.setActive(id, active));
	}

	private Pageable safePageable(Pageable pageable) {
		int page = pageable.isPaged() ? pageable.getPageNumber() : 0;
		int size = pageable.isPaged()
				? Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE)
				: 20;
		Sort sort = pageable.getSort().isSorted()
				? pageable.getSort()
				: Sort.by(Sort.Direction.ASC, "name");
		return PageRequest.of(page, size, sort);
	}
}
