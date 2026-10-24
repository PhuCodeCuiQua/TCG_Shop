package com.tcgshop.controller.web;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("games", List.of(
				Map.of("name", "Magic", "mark", "M"),
				Map.of("name", "Yu-Gi-Oh!", "mark", "遊"),
				Map.of("name", "Pokémon", "mark", "P"),
				Map.of("name", "One Piece", "mark", "OP"),
				Map.of("name", "Riftbound", "mark", "R")));
		model.addAttribute("bestSellers", List.of(
				Map.of("name", "Radiant Vanguard", "series", "Riftbound: Radiance",
						"game", "Riftbound", "price", "1.250.000 ₫", "badge", "BÁN CHẠY",
						"image", "/img/home-card-riftbound.svg"),
				Map.of("name", "Dragon's Ember", "series", "Magic: The Gathering",
						"game", "Magic", "price", "890.000 ₫", "badge", "HOT",
						"image", "/img/home-card-magic.svg"),
				Map.of("name", "Prismatic Companion", "series", "Crystal Canopy",
						"game", "Pokémon", "price", "620.000 ₫", "badge", "BÁN CHẠY",
						"image", "/img/home-card-pokemon.svg"),
				Map.of("name", "Grand Line Captain", "series", "Voyage Beyond",
						"game", "One Piece", "price", "1.480.000 ₫", "badge", "HIẾM",
						"image", "/img/home-card-one-piece.svg"),
				Map.of("name", "Arcane Familiar", "series", "Magic: The Gathering",
						"game", "Magic", "price", "475.000 ₫", "badge", "ĐƯỢC YÊU THÍCH",
						"image", "/img/home-card-magic.svg"),
				Map.of("name", "Luminous Familiar", "series", "Prismatic Horizons",
						"game", "Pokémon", "price", "735.000 ₫", "badge", "MỚI",
						"image", "/img/home-card-pokemon.svg")));
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
