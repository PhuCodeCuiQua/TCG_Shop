package com.tcgshop.controller.web;

import java.util.List;
import java.util.Map;

import com.tcgshop.dto.response.HomePageResponse;
import com.tcgshop.service.HomePageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {
	private final HomePageService homePageService;

	@GetMapping("/")
	public String home(Model model) {
		HomePageResponse homePage = homePageService.getHomePage();
		model.addAttribute("games", homePage.games());
		model.addAttribute("bestSellers", homePage.bestSellers());
		model.addAttribute("recentlyViewed", homePage.recentlyViewed());
		model.addAttribute("recommendations", homePage.recommendations());
		model.addAttribute("latestSets", List.of(
				Map.of("name", "Riftbound: Radiance", "game", "Riftbound",
						"detail", "Bộ sưu tập mới · 240+ thẻ", "image", "/img/home-card-riftbound.svg"),
				Map.of("name", "Prismatic Horizons", "game", "Pokémon",
						"detail", "Bộ sưu tập mới · 180+ thẻ", "image", "/img/home-card-pokemon.svg"),
				Map.of("name", "Aetherbound Chronicles", "game", "Magic",
						"detail", "Bộ sưu tập mới · 250+ thẻ", "image", "/img/home-card-magic.svg"),
				Map.of("name", "Grand Line Awakening", "game", "One Piece",
						"detail", "Bộ sưu tập mới · 150+ thẻ", "image", "/img/home-card-one-piece.svg")));
		return "index";
	}
}
